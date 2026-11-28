package geomystery.data;

import geomystery.data.Model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** data/*.csv 에서 읽은 학습 콘텐츠(읽기 전용). */
public final class Content {
    public final List<Chapter> chapters = new ArrayList<>();
    public final List<Lesson> lessons = new ArrayList<>();
    public final Map<String, Lesson> lessonById = new HashMap<>();
    public final Map<String, Chapter> chapterById = new HashMap<>();
    public final Map<String, List<Line>> dialogues = new HashMap<>();
    public final Map<String, List<Puzzle>> puzzlesByLesson = new HashMap<>();
    public final Map<String, Puzzle> puzzleById = new LinkedHashMap<>();
    public final Map<String, List<Cmd>> diagrams = new HashMap<>();
    public final Map<String, Theorem> theorems = new LinkedHashMap<>();
    public final Map<String, Npc> npcs = new HashMap<>();
    public final List<Quest> quests = new ArrayList<>();
    public final List<Badge> badges = new ArrayList<>();
    public final List<Bot> bots = new ArrayList<>();
    public final List<Quote> quotes = new ArrayList<>();
    public final Map<String, Misconception> misconceptions = new LinkedHashMap<>();
    public final Map<String, List<FinaleLine>> finales = new HashMap<>();
    public final Map<String, Explore> explores = new LinkedHashMap<>();
    /** 최종 사건 → 범인 자백 만화(동기·수법·체포). */
    public final Map<String, List<Confession>> confessions = new HashMap<>();
    /** 인물 → 후일담 대사(호감도를 가득 채우면 열린다). */
    public final Map<String, List<Line>> epilogues = new LinkedHashMap<>();

    public static Content load(Path dir) throws IOException {
        if (!Files.exists(dir.resolve("chapters.csv"))) {
            throw new IOException("콘텐츠 폴더를 찾을 수 없습니다: " + dir.toAbsolutePath());
        }
        Content c = new Content();

        for (Map<String, String> r : Csv.readTable(dir.resolve("chapters.csv"))) {
            Chapter ch = new Chapter();
            ch.id = r.get("id"); ch.order = Csv.toInt(r.get("order"), 0);
            ch.label = r.get("label"); ch.title = r.get("title"); ch.subtitle = r.get("subtitle");
            ch.npc = r.get("npc"); ch.scene = r.get("scene");
            c.chapters.add(ch);
            c.chapterById.put(ch.id, ch);
        }
        c.chapters.sort(Comparator.comparingInt(x -> x.order));

        for (Map<String, String> r : Csv.readTable(dir.resolve("npcs.csv"))) {
            Npc n = new Npc();
            n.id = r.get("id"); n.name = r.get("name"); n.role = r.get("role"); n.image = r.get("image");
            c.npcs.put(n.id, n);
        }

        List<Lesson> tmp = new ArrayList<>();
        for (Map<String, String> r : Csv.readTable(dir.resolve("lessons.csv"))) {
            Lesson l = new Lesson();
            l.id = r.get("id"); l.chapterId = r.get("chapter"); l.order = Csv.toInt(r.get("order"), 0);
            l.title = r.get("title"); l.caseName = r.get("case_name"); l.location = r.get("location");
            l.npc = r.get("npc"); l.scene = r.get("scene");
            l.xp = Csv.toInt(r.get("xp"), 100); l.minutes = Csv.toInt(r.get("minutes"), 5);
            l.boss = "1".equals(r.get("boss").trim()) || "true".equalsIgnoreCase(r.get("boss").trim());
            l.theoremId = r.get("theorem");
            for (String m : r.get("memo").split("\\|")) if (!m.trim().isEmpty()) l.memo.add(m.trim());
            l.tip = r.get("tip"); l.resolution = r.get("resolution"); l.nextLine = r.get("next_line");
            tmp.add(l);
        }
        tmp.sort(Comparator.<Lesson>comparingInt(l -> {
            Chapter ch = c.chapterById.get(l.chapterId);
            return ch == null ? 999 : ch.order;
        }).thenComparingInt(l -> l.order));
        for (int i = 0; i < tmp.size(); i++) {
            Lesson l = tmp.get(i);
            l.globalIndex = i;
            c.lessons.add(l);
            c.lessonById.put(l.id, l);
        }

        for (Map<String, String> r : Csv.readTable(dir.resolve("dialogues.csv"))) {
            Line ln = new Line();
            ln.lessonId = r.get("lesson"); ln.seq = Csv.toInt(r.get("seq"), 0);
            ln.speaker = r.get("speaker"); ln.text = r.get("text");
            String emo = r.get("emotion");
            ln.emotion = emo == null ? "" : emo.trim();
            c.dialogues.computeIfAbsent(ln.lessonId, k -> new ArrayList<>()).add(ln);
        }
        for (List<Line> ls : c.dialogues.values()) ls.sort(Comparator.comparingInt(x -> x.seq));

        int number = 1;
        for (Map<String, String> r : Csv.readTable(dir.resolve("puzzles.csv"))) {
            Puzzle p = new Puzzle();
            p.id = r.get("id"); p.lessonId = r.get("lesson"); p.order = Csv.toInt(r.get("order"), 0);
            p.title = r.get("title"); p.noteTitle = r.get("note_title"); p.note = r.get("note");
            p.hazel = r.get("hazel"); p.diagram = r.get("diagram"); p.type = r.get("type").trim();
            p.answer = r.get("answer").trim(); p.unit = r.get("unit"); p.answerLabel = r.get("answer_label");
            for (String ch : r.get("choices").split("\\|")) if (!ch.trim().isEmpty()) p.choices.add(ch.trim());
            for (String k : new String[]{"hint1", "hint2", "hint3"}) {
                String h = r.get(k);
                if (h != null && !h.trim().isEmpty()) p.hints.add(h.trim());
            }
            p.explanation = r.get("explanation");
            p.feedback.addAll(parseFeedback(r.get("feedback")));
            String st = r.get("steps");
            if (st != null) for (String x : st.split("\\|")) if (!x.trim().isEmpty()) p.steps.add(x.trim());
            p.number = number++;
            c.puzzleById.put(p.id, p);
            c.puzzlesByLesson.computeIfAbsent(p.lessonId, k -> new ArrayList<>()).add(p);
        }
        for (List<Puzzle> ps : c.puzzlesByLesson.values()) ps.sort(Comparator.comparingInt(x -> x.order));

        for (Map<String, String> r : Csv.readTable(dir.resolve("diagrams.csv"))) {
            Cmd cmd = new Cmd();
            cmd.layer = r.get("layer").trim();
            cmd.op = r.get("cmd").trim();
            cmd.args = tokenize(r.get("args"));
            c.diagrams.computeIfAbsent(r.get("diagram").trim(), k -> new ArrayList<>()).add(cmd);
        }

        for (Map<String, String> r : Csv.readTable(dir.resolve("theorems.csv"))) {
            Theorem t = new Theorem();
            t.id = r.get("id"); t.name = r.get("name"); t.description = r.get("description"); t.icon = r.get("icon");
            c.theorems.put(t.id, t);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("quests.csv"))) {
            Quest q = new Quest();
            q.id = r.get("id"); q.text = r.get("text"); q.type = r.get("type");
            q.target = Csv.toInt(r.get("target"), 1); q.reward = Csv.toInt(r.get("reward"), 1);
            c.quests.add(q);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("badges.csv"))) {
            Badge b = new Badge();
            b.id = r.get("id"); b.name = r.get("name"); b.description = r.get("description");
            b.stat = r.get("stat"); b.target = Csv.toInt(r.get("target"), 1); b.icon = r.get("icon");
            c.badges.add(b);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("league.csv"))) {
            Bot b = new Bot();
            b.name = r.get("name");
            b.color = Integer.parseInt(r.get("color").replace("#", "").trim(), 16);
            b.base = Csv.toInt(r.get("base"), 0); b.daily = Csv.toInt(r.get("daily"), 0);
            c.bots.add(b);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("quotes.csv"))) {
            Quote q = new Quote();
            q.text = r.get("text"); q.author = r.get("author");
            c.quotes.add(q);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("misconceptions.csv"))) {
            Misconception m = new Misconception();
            m.id = r.get("id").trim(); m.name = r.get("name"); m.tip = r.get("tip");
            c.misconceptions.put(m.id, m);
        }
        for (Map<String, String> r : Csv.readTable(dir.resolve("finales.csv"))) {
            FinaleLine f = new FinaleLine();
            f.lessonId = r.get("lesson").trim(); f.seq = Csv.toInt(r.get("seq"), 0);
            f.kind = r.get("kind").trim(); f.speaker = r.get("speaker").trim();
            f.text = r.get("text"); f.theoremId = r.get("theorem").trim();
            c.finales.computeIfAbsent(f.lessonId, k -> new ArrayList<>()).add(f);
        }
        for (List<FinaleLine> fs : c.finales.values()) fs.sort(Comparator.comparingInt(x -> x.seq));
        for (Map<String, String> r : Csv.readTable(dir.resolve("explore.csv"))) {
            Explore e = new Explore();
            e.theoremId = r.get("theorem").trim();
            e.diagram = r.get("diagram").trim();
            e.relation = r.get("relation");
            e.caption = r.get("caption");
            for (String part : r.get("drag").split("\\|")) {
                String[] t = part.trim().split("\\s+");
                if (t.length < 2) continue;
                Drag d = new Drag();
                d.point = t[0];
                d.kind = t[1];
                int i = 2;
                if (("circle".equals(d.kind) || "arc".equals(d.kind)) && t.length > 2) d.center = t[i++];
                List<Double> nums = new ArrayList<>();
                for (; i < t.length; i++) nums.add(Csv.toDouble(t[i], 0));
                int need = "arc".equals(d.kind) ? 2 : 0;
                d.args = new double[Math.min(need, nums.size())];
                for (int k = 0; k < d.args.length; k++) d.args[k] = nums.get(k);
                if (nums.size() > need) d.snap = nums.get(need);
                e.drags.add(d);
            }
            c.explores.put(e.theoremId, e);
        }
        if (Files.exists(dir.resolve("confessions.csv"))) {
            for (Map<String, String> r : Csv.readTable(dir.resolve("confessions.csv"))) {
                Confession cf = new Confession();
                cf.lessonId = r.get("lesson").trim(); cf.panel = Csv.toInt(r.get("panel"), 0);
                cf.kind = r.get("kind").trim(); cf.caption = r.get("caption");
                cf.prop = r.get("prop") == null ? "" : r.get("prop").trim();
                c.confessions.computeIfAbsent(cf.lessonId, k -> new ArrayList<>()).add(cf);
            }
            for (List<Confession> cs : c.confessions.values()) cs.sort(Comparator.comparingInt(x -> x.panel));
        }
        if (Files.exists(dir.resolve("epilogues.csv"))) {
            for (Map<String, String> r : Csv.readTable(dir.resolve("epilogues.csv"))) {
                Line ln = new Line();
                ln.lessonId = "E:" + r.get("npc").trim(); ln.seq = Csv.toInt(r.get("seq"), 0);
                ln.speaker = r.get("speaker").trim(); ln.text = r.get("text");
                String emo = r.get("emotion");
                ln.emotion = emo == null ? "" : emo.trim();
                c.epilogues.computeIfAbsent(r.get("npc").trim(), k -> new ArrayList<>()).add(ln);
            }
            for (List<Line> ls : c.epilogues.values()) ls.sort(Comparator.comparingInt(x -> x.seq));
        }
        return c;
    }

    /**
     * 맞춤 피드백 열을 읽는다. 형식: {@code 오답=대사|오답=#태그 대사}.
     * 오답 자리에는 숫자·식·보기 문구를 쓸 수 있다.
     */
    public static List<Feedback> parseFeedback(String s) {
        List<Feedback> out = new ArrayList<>();
        if (s == null) return out;
        for (String part : s.split("\\|")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            Feedback f = new Feedback();
            f.value = part.substring(0, eq).trim();
            String rest = part.substring(eq + 1).trim();
            if (rest.startsWith("#")) {
                int sp = rest.indexOf(' ');
                f.tag = (sp < 0 ? rest.substring(1) : rest.substring(1, sp)).trim();
                rest = sp < 0 ? "" : rest.substring(sp + 1).trim();
            }
            f.text = rest;
            out.add(f);
        }
        return out;
    }

    /** 공백으로 구분하되 '작은따옴표'로 감싼 토큰은 하나로 취급한다. */
    static String[] tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return new String[0];
        StringBuilder cur = new StringBuilder();
        boolean q = false, has = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (q) {
                if (ch == '\'') q = false; else cur.append(ch);
            } else if (ch == '\'') {
                q = true; has = true;
            } else if (Character.isWhitespace(ch)) {
                if (has || cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); has = false; }
            } else {
                cur.append(ch); has = true;
            }
        }
        if (has || cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // ---- 조회 도우미 ----
    public List<Lesson> lessonsOf(Chapter ch) {
        List<Lesson> out = new ArrayList<>();
        for (Lesson l : lessons) if (l.chapterId.equals(ch.id)) out.add(l);
        return out;
    }

    public List<Puzzle> puzzlesOf(Lesson l) {
        return puzzlesByLesson.getOrDefault(l.id, Collections.emptyList());
    }

    public List<Line> dialogueOf(Lesson l) {
        return dialogues.getOrDefault(l.id, Collections.emptyList());
    }

    public Chapter chapterOf(Lesson l) { return chapterById.get(l.chapterId); }

    public Npc npc(String id) { return npcs.get(id); }

    public List<FinaleLine> finaleOf(Lesson l) {
        return l == null ? Collections.emptyList() : finales.getOrDefault(l.id, Collections.emptyList());
    }

    public boolean hasFinale(Lesson l) {
        for (FinaleLine f : finaleOf(l)) if (FinaleLine.LIE.equals(f.kind)) return true;
        return false;
    }

    /**
     * 인물이 속한 장: 장의 의뢰인이거나, 그 장 최종 추리의 증언자. 헤이즐·해설은 null.
     */
    public Chapter chapterOfNpc(String npcId) {
        if (npcId == null || "hazel".equals(npcId) || "narration".equals(npcId)) return null;
        for (Chapter ch : chapters) if (npcId.equals(ch.npc)) return ch;
        for (Lesson l : lessons) {
            for (FinaleLine f : finaleOf(l)) if (npcId.equals(f.speaker)) return chapterOf(l);
        }
        return null;
    }

    /** 장에 나오는 인물들(의뢰인 먼저, 그다음 최종 추리 증언자 순). */
    public List<Npc> npcsOf(Chapter ch) {
        List<Npc> out = new ArrayList<>();
        Npc client = npcs.get(ch.npc);
        if (client != null) out.add(client);
        for (Lesson l : lessonsOf(ch)) {
            for (FinaleLine f : finaleOf(l)) {
                Npc n = npcs.get(f.speaker);
                if (n != null && !"hazel".equals(n.id) && !out.contains(n)) out.add(n);
            }
        }
        return out;
    }

    /** 사건의 범인(최종 추리에서 거짓 증언을 한 인물). 없으면 null. */
    public String culpritOf(Lesson l) {
        for (FinaleLine f : finaleOf(l)) if (FinaleLine.LIE.equals(f.kind)) return f.speaker;
        return null;
    }

    /** 정리 → 그 정리를 주는 사건. */
    public Lesson lessonOfTheorem(String theoremId) {
        for (Lesson l : lessons) if (theoremId != null && theoremId.equals(l.theoremId)) return l;
        return null;
    }
}
