package geomystery.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 여러 탐정 프로필 관리.
 * <pre>
 *  data/save/active.txt          — 지금 쓰는 프로필 id
 *  data/save/profiles/p1/*.csv   — 프로필마다 SaveData 파일 묶음
 * </pre>
 * 예전 버전처럼 data/save/ 바로 아래에 저장 파일이 있으면 처음 열 때 profiles/p1 로 옮긴다.
 */
public final class Profiles {
    private static final long MAX_ENTRY = 4L * 1024 * 1024;

    private final Path root;

    public static final class Entry {
        public String id, name;
        public int xp;
        public boolean active;
    }

    private Profiles(Path root) { this.root = root; }

    public static Profiles open(Path saveRoot) throws IOException {
        Profiles p = new Profiles(saveRoot);
        Files.createDirectories(saveRoot.resolve("profiles"));
        p.migrateLegacy();
        if (p.ids().isEmpty()) p.create("견습 탐정");
        if (!p.ids().contains(p.activeId())) p.setActive(p.ids().get(0));
        return p;
    }

    private void migrateLegacy() throws IOException {
        if (!Files.exists(root.resolve("profile.csv"))) return;
        Path dest = root.resolve("profiles").resolve(nextId());
        Files.createDirectories(dest);
        for (String f : SaveData.FILES) {
            Path src = root.resolve(f);
            if (Files.exists(src)) Files.move(src, dest.resolve(f), StandardCopyOption.REPLACE_EXISTING);
        }
        setActive(dest.getFileName().toString());
    }

    public Path dirOf(String id) { return root.resolve("profiles").resolve(id); }

    public List<String> ids() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> st = Files.list(root.resolve("profiles"))) {
            st.filter(Files::isDirectory).forEach(d -> out.add(d.getFileName().toString()));
        } catch (IOException ignored) { }
        out.sort(Comparator.comparingInt(Profiles::idNumber).thenComparing(s -> s));
        return out;
    }

    private static int idNumber(String id) {
        try { return Integer.parseInt(id.replaceAll("\\D", "")); } catch (NumberFormatException e) { return 9999; }
    }

    private String nextId() {
        int max = 0;
        for (String id : ids()) max = Math.max(max, idNumber(id) == 9999 ? 0 : idNumber(id));
        return "p" + (max + 1);
    }

    public String activeId() {
        try {
            Path a = root.resolve("active.txt");
            if (Files.exists(a)) return new String(Files.readAllBytes(a), StandardCharsets.UTF_8).trim();
        } catch (IOException ignored) { }
        return "";
    }

    public void setActive(String id) throws IOException {
        Files.write(root.resolve("active.txt"), id.getBytes(StandardCharsets.UTF_8));
    }

    public List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        String act = activeId();
        for (String id : ids()) {
            Entry e = new Entry();
            e.id = id;
            e.name = "이름 없는 탐정";
            e.active = id.equals(act);
            try {
                for (Map<String, String> r : Csv.readTable(dirOf(id).resolve("profile.csv"))) {
                    if ("name".equals(r.get("key"))) e.name = r.get("value");
                    if ("xp".equals(r.get("key"))) e.xp = Csv.toInt(r.get("value"), 0);
                }
            } catch (IOException ignored) { }
            out.add(e);
        }
        return out;
    }

    /** 새 프로필을 만들고 id를 돌려준다(활성화하지는 않는다). */
    public String create(String name) throws IOException {
        String id = nextId();
        SaveData s = SaveData.load(dirOf(id));
        s.name = name == null || name.trim().isEmpty() ? "견습 탐정" : name.trim();
        s.save();
        return id;
    }

    public void delete(String id) throws IOException {
        if (id.equals(activeId())) throw new IOException("사용 중인 프로필은 지울 수 없습니다");
        Path d = dirOf(id);
        if (!Files.isDirectory(d)) return;
        try (Stream<Path> st = Files.list(d)) {
            for (Path f : (Iterable<Path>) st::iterator) Files.deleteIfExists(f);
        }
        Files.deleteIfExists(d);
    }

    // ------------------------------------------------------------------ 내보내기 / 불러오기
    /** 프로필의 저장 파일을 zip 하나로 묶는다. */
    public void exportZip(String id, Path zip) throws IOException {
        Path d = dirOf(id);
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(os)) {
            for (String f : SaveData.FILES) {
                Path p = d.resolve(f);
                if (!Files.exists(p)) continue;
                zos.putNextEntry(new ZipEntry(f));
                zos.write(Files.readAllBytes(p));
                zos.closeEntry();
            }
        }
    }

    /**
     * 내보낸 zip을 새 프로필로 불러온다. 알려진 저장 파일 이름만 받아들이고 나머지 항목은 무시한다.
     * @return 새 프로필 id
     */
    public String importZip(Path zip) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (InputStream is = Files.newInputStream(zip); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName().replace('\\', '/');
                name = name.substring(name.lastIndexOf('/') + 1);
                if (e.isDirectory() || !SaveData.FILES.contains(name)) continue;
                files.put(name, readLimited(zis));
            }
        }
        if (!files.containsKey("profile.csv")) throw new IOException("탐정 수첩 백업 파일이 아닙니다 (profile.csv 없음)");
        // 형식 확인: CSV로 읽을 수 있어야 한다
        Csv.parse(new String(files.get("profile.csv"), StandardCharsets.UTF_8));
        String id = nextId();
        Path d = dirOf(id);
        Files.createDirectories(d);
        for (Map.Entry<String, byte[]> f : files.entrySet()) Files.write(d.resolve(f.getKey()), f.getValue());
        return id;
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_ENTRY) throw new IOException("백업 파일 항목이 너무 큽니다");
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
