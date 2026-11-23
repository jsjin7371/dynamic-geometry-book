package geomystery.tools;

import geomystery.data.Content;
import geomystery.data.Csv;
import geomystery.data.Model.Cmd;
import geomystery.data.Model.Puzzle;
import geomystery.diagram.DiagramView;
import geomystery.ui.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * diagrams.csv 도형 편집기. 스크립트를 고치면 칠판 미리보기가 바로 바뀐다.
 * <ul>
 *   <li>한 줄에 명령 하나: {@code 레이어 명령 인자...} (예: {@code base pt A 170 130})</li>
 *   <li>미리보기 빈 곳 클릭 → 그 좌표에 새 점(pt) 추가 · pt로 만든 점은 끌어서 옮기기</li>
 *   <li>아래쪽에 스크립트 오류가 실시간으로 표시된다 · Ctrl/Cmd+S 저장</li>
 * </ul>
 * 저장하면 diagrams.csv 에서 그 도형의 줄만 바꾸고 나머지(주석 포함)는 그대로 둔다.
 * <pre>  ./run.sh editor</pre>
 */
public final class DiagramEditor extends JFrame {
    private final Path file;
    private final Path dataDir;
    private final DefaultListModel<String> ids = new DefaultListModel<>();
    private final JList<String> idList = new JList<>(ids);
    private final JTextArea editor = new JTextArea();
    private final JTextArea problems = new JTextArea(4, 40);
    private final JLabel status = new JLabel(" ");
    private final JLabel usage = new JLabel(" ");
    private final JCheckBox auxBox = new JCheckBox("보조선(aux) 보기", true);
    private final JCheckBox gridBox = new JCheckBox("격자", true);
    private final Preview preview = new Preview();
    private final javax.swing.Timer debounce;

    /** 디스크에서 읽은 도형별 스크립트 줄(편집기 형식). */
    private final Map<String, String> saved = new LinkedHashMap<>();
    private final Map<String, String> drafts = new HashMap<>();
    private Map<String, List<String>> users = new HashMap<>();
    private String current;
    private boolean loading;

    public DiagramEditor(Path home) throws Exception {
        super("도형 편집기 · diagrams.csv");
        dataDir = home.resolve("data");
        file = dataDir.resolve("diagrams.csv");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quit(); }
        });

        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setTabSize(4);
        problems.setEditable(false);
        problems.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        problems.setForeground(new Color(0x8E2E20));
        idList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        idList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        idList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                String id = String.valueOf(v);
                if (isDirty(id)) setText(id + "  *");
                if (!users.containsKey(id)) setForeground(s ? Color.WHITE : Color.GRAY);
                return this;
            }
        });
        idList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && idList.getSelectedValue() != null) open(idList.getSelectedValue());
        });

        debounce = new javax.swing.Timer(220, e -> refreshPreview());
        debounce.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        auxBox.addActionListener(e -> preview.repaint());
        gridBox.addActionListener(e -> preview.repaint());

        // ---- 배치
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        left.add(new JLabel("도형 (회색 = 쓰는 수수께끼 없음)"), BorderLayout.NORTH);
        left.add(new JScrollPane(idList), BorderLayout.CENTER);
        JPanel lb = new JPanel(new GridLayout(2, 1, 0, 4));
        JButton add = new JButton("새 도형");
        add.addActionListener(e -> newDiagram());
        JButton reload = new JButton("다시 읽기");
        reload.addActionListener(e -> reloadAll());
        lb.add(add);
        lb.add(reload);
        left.add(lb, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(190, 600));

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        JLabel help = new JLabel("<html><body style='width:440px'>한 줄 = <b>레이어 명령 인자…</b> &nbsp;예) <tt>base pt A 170 130</tt>, <tt>aux seg A B gold 2d</tt>"
                + "<br>명령: pt polar along offset vec foot apex · seg line ray arrow circle arc angle right tick parallel len tag text fill dot dial hand hub</html>");
        mid.add(help, BorderLayout.NORTH);
        mid.add(new JScrollPane(editor), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(new JScrollPane(problems), BorderLayout.CENTER);
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton save = new JButton("저장 (Ctrl/Cmd+S)");
        save.addActionListener(e -> saveCurrent());
        JButton revert = new JButton("되돌리기");
        revert.addActionListener(e -> revert());
        sb.add(save);
        sb.add(revert);
        sb.add(status);
        south.add(sb, BorderLayout.SOUTH);
        mid.add(south, BorderLayout.SOUTH);
        mid.setPreferredSize(new Dimension(480, 600));

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        opts.add(auxBox);
        opts.add(gridBox);
        opts.add(new JLabel("빈 곳 클릭 = 새 점 · 점 끌기 = 이동(pt만)"));
        right.add(opts, BorderLayout.NORTH);
        right.add(preview, BorderLayout.CENTER);
        right.add(usage, BorderLayout.SOUTH);

        JSplitPane inner = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mid, right);
        inner.setResizeWeight(0.4);
        JSplitPane outer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, inner);
        setContentPane(outer);

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getRootPane().registerKeyboardAction(e -> saveCurrent(), KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);

        reloadAll();
        setSize(1400, 820);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ 파일
    private void reloadAll() {
        if (anyDirty() && JOptionPane.showConfirmDialog(this, "저장하지 않은 변경을 버리고 다시 읽을까요?", "다시 읽기",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            Content c = Content.load(dataDir);
            saved.clear();
            drafts.clear();
            for (Map.Entry<String, List<Cmd>> e : c.diagrams.entrySet()) saved.put(e.getKey(), toText(e.getValue()));
            users = new HashMap<>();
            for (Puzzle p : c.puzzleById.values()) {
                if (p.diagram != null) users.computeIfAbsent(p.diagram.trim(), k -> new ArrayList<>()).add(p.id + " " + p.title);
            }
            String keep = current;
            ids.clear();
            List<String> sorted = new ArrayList<>(saved.keySet());
            Collections.sort(sorted);
            for (String id : sorted) ids.addElement(id);
            current = null;
            if (keep != null && saved.containsKey(keep)) idList.setSelectedValue(keep, true);
            else if (!ids.isEmpty()) idList.setSelectedIndex(0);
            status.setText("diagrams.csv: 도형 " + saved.size() + "개");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "읽지 못했습니다: " + ex);
        }
    }

    private static String toText(List<Cmd> cmds) {
        StringBuilder sb = new StringBuilder();
        for (Cmd c : cmds) sb.append(c.layer).append(' ').append(c.op).append(' ').append(String.join(" ", quoteArgs(c.args))).append('\n');
        return sb.toString();
    }

    private static List<String> quoteArgs(String[] args) {
        List<String> out = new ArrayList<>();
        for (String a : args) out.add(a.isEmpty() || a.contains(" ") ? "'" + a + "'" : a);
        return out;
    }

    /** 편집기 글을 (레이어, 명령, 인자 문자열) 줄들로. 빈 줄과 # 주석은 건너뛴다. */
    private static List<String[]> parseText(String text) {
        List<String[]> out = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String ln = raw.trim();
            if (ln.isEmpty() || ln.startsWith("#")) continue;
            String[] p = ln.split("\\s+", 3);
            out.add(new String[]{p[0], p.length > 1 ? p[1] : "", p.length > 2 ? p[2] : ""});
        }
        return out;
    }

    private List<Cmd> script(String text) {
        List<Cmd> out = new ArrayList<>();
        for (String[] r : parseText(text)) {
            Cmd c = new Cmd();
            c.layer = r[0];
            c.op = r[1];
            c.args = tokenize(r[2]);
            out.add(c);
        }
        return out;
    }

    /** Content.tokenize 와 같은 규칙('작은따옴표' 묶음). */
    private static String[] tokenize(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, has = false;
        for (char ch : s.toCharArray()) {
            if (q) { if (ch == '\'') q = false; else cur.append(ch); }
            else if (ch == '\'') { q = true; has = true; }
            else if (Character.isWhitespace(ch)) {
                if (has || cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); has = false; }
            } else { cur.append(ch); has = true; }
        }
        if (has || cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    /** 이 도형의 줄만 바꿔 diagrams.csv 를 다시 쓴다(첫 등장 위치에 새 묶음, 나머지 줄·주석은 그대로). */
    private void saveCurrent() {
        if (current == null) return;
        String text = editor.getText();
        List<String> problemsNow = DiagramView.check(script(text));
        if (!problemsNow.isEmpty() && JOptionPane.showConfirmDialog(this,
                "스크립트에 문제가 " + problemsNow.size() + "개 있습니다. 그래도 저장할까요?", "저장", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>(Arrays.asList(new String(Files.readAllBytes(file), StandardCharsets.UTF_8).split("\n", -1)));
            if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines.remove(lines.size() - 1);
            List<String> block = new ArrayList<>();
            for (String[] r : parseText(text)) {
                block.add(Csv.quote(current) + "," + Csv.quote(r[0]) + "," + Csv.quote(r[1]) + "," + Csv.quote(r[2]));
            }
            List<String> out = new ArrayList<>();
            boolean placed = false;
            for (String ln : lines) {
                String first = ln.contains(",") ? ln.substring(0, ln.indexOf(',')).trim() : ln.trim();
                if (first.equals(current) && !ln.startsWith("#")) {
                    if (!placed) { out.addAll(block); placed = true; }
                    continue;
                }
                out.add(ln);
            }
            if (!placed) {
                out.add("# ----- " + current);
                out.addAll(block);
            }
            Path tmp = file.resolveSibling("diagrams.csv.tmp");
            Files.write(tmp, (String.join("\n", out) + "\n").getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            saved.put(current, normalized(text));
            drafts.remove(current);
            status.setText(current + " 저장함 (" + block.size() + "줄)");
            idList.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "저장하지 못했습니다: " + ex);
        }
    }

    private static String normalized(String text) {
        StringBuilder sb = new StringBuilder();
        for (String[] r : parseText(text)) sb.append(r[0]).append(' ').append(r[1]).append(' ').append(r[2]).append('\n');
        return sb.toString();
    }

    private boolean isDirty(String id) {
        String d = drafts.get(id);
        return d != null && !normalized(d).equals(normalized(saved.getOrDefault(id, "")));
    }

    private boolean anyDirty() {
        if (current != null) drafts.put(current, editor.getText());
        for (String id : drafts.keySet()) if (isDirty(id)) return true;
        return false;
    }

    private void revert() {
        if (current == null) return;
        drafts.remove(current);
        loading = true;
        editor.setText(saved.getOrDefault(current, ""));
        loading = false;
        refreshPreview();
        idList.repaint();
    }

    private void newDiagram() {
        String id = JOptionPane.showInputDialog(this, "새 도형 id (예: D320)", "D");
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty() || id.contains(",") || id.contains(" ")) return;
        if (!saved.containsKey(id)) {
            saved.put(id, "");
            ids.addElement(id);
            drafts.put(id, "base pt A 200 330\nbase pt B 440 330\nbase apex C A B 60 60\n"
                    + "base seg A B chalk 3\nbase seg B C chalk 3\nbase seg C A chalk 3\n"
                    + "base angle B A C 40 60° gold\nbase dot A sw\nbase dot B se\nbase dot C n\n");
        }
        idList.setSelectedValue(id, true);
    }

    private void quit() {
        if (anyDirty() && JOptionPane.showConfirmDialog(this, "저장하지 않은 도형이 있습니다. 그래도 닫을까요?", "닫기",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ 편집
    private void open(String id) {
        if (id.equals(current)) return;
        if (current != null) drafts.put(current, editor.getText());
        current = id;
        loading = true;
        editor.setText(drafts.containsKey(id) ? drafts.get(id) : saved.getOrDefault(id, ""));
        editor.setCaretPosition(0);
        loading = false;
        List<String> u = users.getOrDefault(id, Collections.emptyList());
        usage.setText(u.isEmpty() ? "이 도형을 쓰는 수수께끼가 없습니다 — puzzles.csv의 diagram 열에 " + id + "를 적으면 연결됩니다"
                : "사용: " + String.join(", ", u));
        refreshPreview();
    }

    private void changed() {
        if (loading || current == null) return;
        drafts.put(current, editor.getText());
        debounce.restart();
    }

    private void refreshPreview() {
        List<Cmd> sc = script(editor.getText());
        preview.setScript(sc);
        List<String> pr = DiagramView.check(sc);
        problems.setText(pr.isEmpty() ? "문제 없음 ✓" : String.join("\n", pr));
        problems.setForeground(pr.isEmpty() ? new Color(0x2F6B55) : new Color(0x8E2E20));
        idList.repaint();
    }

    /** pt 줄의 좌표를 바꾼다. */
    private void movePoint(String name, int x, int y) {
        String[] lines = editor.getText().split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String[] p = lines[i].trim().split("\\s+");
            if (p.length >= 5 && "pt".equals(p[1]) && name.equals(p[2])) {
                lines[i] = p[0] + " pt " + name + " " + x + " " + y;
                int caret = editor.getCaretPosition();
                loading = true;
                editor.setText(String.join("\n", lines));
                loading = false;
                editor.setCaretPosition(Math.min(caret, editor.getDocument().getLength()));
                drafts.put(current, editor.getText());
                refreshPreview();
                return;
            }
        }
    }

    private boolean isFreePoint(String name) {
        for (String raw : editor.getText().split("\n")) {
            String[] p = raw.trim().split("\\s+");
            if (p.length >= 3 && "pt".equals(p[1]) && name.equals(p[2])) return true;
        }
        return false;
    }

    private void addPoint(int x, int y) {
        Set<String> used = DiagramView.points(script(editor.getText())).keySet();
        String name = null;
        for (char ch = 'A'; ch <= 'Z' && name == null; ch++) if (!used.contains("" + ch)) name = "" + ch;
        for (int i = 1; name == null; i++) if (!used.contains("P" + i)) name = "P" + i;
        String text = editor.getText();
        if (!text.isEmpty() && !text.endsWith("\n")) text += "\n";
        editor.setText(text + "base pt " + name + " " + x + " " + y + "\nbase dot " + name + " n\n");
        status.setText("점 " + name + " 추가 (" + x + ", " + y + ")");
    }

    // ------------------------------------------------------------------ 미리보기
    private final class Preview extends JComponent {
        private List<Cmd> sc = Collections.emptyList();
        private Point2D.Double mouse;
        private String dragging, hover;

        Preview() {
            setPreferredSize(new Dimension(660, 460));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    mouse = toLogical(e.getPoint());
                    String h = hit(mouse);
                    if (!Objects.equals(h, hover)) hover = h;
                    setCursor(Cursor.getPredefinedCursor(h != null && isFreePoint(h) ? Cursor.MOVE_CURSOR : Cursor.CROSSHAIR_CURSOR));
                    repaint();
                }
                @Override public void mousePressed(MouseEvent e) {
                    Point2D.Double p = toLogical(e.getPoint());
                    String h = hit(p);
                    dragging = h != null && isFreePoint(h) ? h : null;
                }
                @Override public void mouseDragged(MouseEvent e) {
                    mouse = toLogical(e.getPoint());
                    if (dragging != null) movePoint(dragging, snap(mouse.x), snap(mouse.y));
                }
                @Override public void mouseReleased(MouseEvent e) { dragging = null; }
                @Override public void mouseClicked(MouseEvent e) {
                    Point2D.Double p = toLogical(e.getPoint());
                    if (hit(p) == null && p.x >= 0 && p.y >= 0 && p.x <= DiagramView.W && p.y <= DiagramView.H) {
                        addPoint(snap(p.x), snap(p.y));
                    }
                }
                @Override public void mouseExited(MouseEvent e) { mouse = null; hover = null; repaint(); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int snap(double v) { return (int) Math.round(v / 5.0) * 5; }

        void setScript(List<Cmd> s) { sc = s; repaint(); }

        private double scale() { return Math.min(getWidth() / DiagramView.W, getHeight() / DiagramView.H); }
        private double ox() { return (getWidth() - DiagramView.W * scale()) / 2; }
        private double oy() { return (getHeight() - DiagramView.H * scale()) / 2; }

        private Point2D.Double toLogical(Point p) {
            return new Point2D.Double((p.x - ox()) / scale(), (p.y - oy()) / scale());
        }

        private String hit(Point2D.Double p) {
            if (p == null) return null;
            for (Map.Entry<String, Point2D.Double> e : DiagramView.points(sc).entrySet()) {
                if (e.getValue().distance(p) < 12) return e.getKey();
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            g.setColor(new Color(0x22473D));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.translate(ox(), oy());
            g.scale(scale(), scale());
            g.setColor(new Color(0x2F5E50));
            g.fill(new Rectangle2D.Double(0, 0, DiagramView.W, DiagramView.H));
            if (gridBox.isSelected()) {
                g.setColor(new Color(255, 255, 255, 22));
                for (int x = 0; x <= DiagramView.W; x += 20) g.drawLine(x, 0, x, (int) DiagramView.H);
                for (int y = 0; y <= DiagramView.H; y += 20) g.drawLine(0, y, (int) DiagramView.W, y);
                g.setColor(new Color(255, 255, 255, 45));
                for (int x = 0; x <= DiagramView.W; x += 100) g.drawLine(x, 0, x, (int) DiagramView.H);
                for (int y = 0; y <= DiagramView.H; y += 100) g.drawLine(0, y, (int) DiagramView.W, y);
            }
            try {
                DiagramView.render(g, sc, auxBox.isSelected() ? 1f : 0f);
            } catch (RuntimeException ignored) {
                // 오류는 아래 문제 칸에 표시된다
            }
            for (Map.Entry<String, Point2D.Double> e : DiagramView.points(sc).entrySet()) {
                Point2D.Double p = e.getValue();
                boolean free = isFreePoint(e.getKey());
                g.setColor(e.getKey().equals(hover) ? new Color(0xFFD166) : free ? new Color(0x8DB7DA, false) : new Color(255, 255, 255, 90));
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval((int) p.x - 9, (int) p.y - 9, 18, 18);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                g.drawString(e.getKey(), (int) p.x + 10, (int) p.y - 10);
            }
            g.dispose();
            if (mouse != null) {
                Graphics2D t = (Graphics2D) g0;
                t.setColor(Color.WHITE);
                t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                t.drawString(String.format("x %d  y %d%s", Math.round(mouse.x), Math.round(mouse.y),
                        hover != null ? "  · " + hover + (isFreePoint(hover) ? " (끌어서 이동)" : " (계산된 점)") : ""), 8, getHeight() - 8);
            }
        }
    }

    // ------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        java.nio.file.Path home = args.length > 0 ? java.nio.file.Paths.get(args[0]) : ContentValidator.findHome();
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.loadFonts(home.resolve("assets/fonts"));
                new DiagramEditor(home).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "편집기를 열지 못했습니다: " + e);
                System.exit(1);
            }
        });
    }
}
