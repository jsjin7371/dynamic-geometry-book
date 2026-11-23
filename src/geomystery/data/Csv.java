package geomystery.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * RFC4180 스타일의 간단한 CSV 읽기/쓰기 유틸리티.
 * - 큰따옴표로 감싼 필드 안의 쉼표, 줄바꿈, "" 이스케이프를 지원한다.
 * - 첫 필드가 '#'으로 시작하는 줄은 주석으로 무시한다.
 */
public final class Csv {
    private Csv() {}

    public static List<List<String>> parse(String s) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        int i = 0, n = s.length();
        if (n > 0 && s.charAt(0) == '﻿') i = 1;
        for (; i < n; i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < n && s.charAt(i + 1) == '"') { f.append('"'); i++; }
                    else quoted = false;
                } else if (c != '\r') {
                    f.append(c);
                }
            } else {
                if (c == '"') quoted = true;
                else if (c == ',') { row.add(f.toString()); f.setLength(0); }
                else if (c == '\r') { /* skip */ }
                else if (c == '\n') {
                    row.add(f.toString()); f.setLength(0);
                    addRow(rows, row);
                    row = new ArrayList<>();
                } else f.append(c);
            }
        }
        if (f.length() > 0 || !row.isEmpty()) {
            row.add(f.toString());
            addRow(rows, row);
        }
        return rows;
    }

    private static void addRow(List<List<String>> rows, List<String> row) {
        if (row.isEmpty()) return;
        if (row.size() == 1 && row.get(0).trim().isEmpty()) return;
        if (row.get(0).startsWith("#")) return;
        rows.add(row);
    }

    /** 헤더가 있는 CSV를 (열 이름 → 값) 맵의 리스트로 읽는다. 파일이 없으면 빈 리스트. */
    public static List<Map<String, String>> readTable(Path p) throws IOException {
        List<Map<String, String>> out = new ArrayList<>();
        if (!Files.exists(p)) return out;
        List<List<String>> rows = parse(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        if (rows.isEmpty()) return out;
        List<String> header = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            Map<String, String> m = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                m.put(header.get(c).trim(), c < row.size() ? row.get(c) : "");
            }
            out.add(m);
        }
        return out;
    }

    /** 임시 파일에 쓴 뒤 교체하는 방식으로 안전하게 CSV를 저장한다. */
    public static void writeTable(Path p, List<String> header, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (List<String> r : rows) appendRow(sb, r);
        Files.createDirectories(p.toAbsolutePath().getParent());
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void appendRow(StringBuilder sb, List<String> r) {
        for (int i = 0; i < r.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(r.get(i)));
        }
        sb.append('\n');
    }

    public static String quote(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0
                || s.startsWith("#")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    public static int toInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static double toDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static long toLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
