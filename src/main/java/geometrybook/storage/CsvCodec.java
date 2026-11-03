package geometrybook.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 읽기·쓰기 공통 클래스 (ERD 6장 저장 계약).
 * - UTF-8, 첫 행은 헤더
 * - 값에 쉼표·따옴표·줄바꿈이 있으면 큰따옴표로 감싸고, 안의 따옴표는 "" 로 바꾼다
 * - 단순 split(",") 을 쓰지 않고 한 글자씩 읽어서 인용 규칙을 처리한다
 */
public final class CsvCodec {

    private CsvCodec() {}

    /** 파일 전체를 읽어 행 목록으로 돌려준다. 0번 행이 헤더. */
    public static List<List<String>> read(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) text = text.substring(1); // 메모장 BOM 제거
        return parse(text);
    }

    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean rowHasContent = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');   // "" → "
                        i++;
                    } else {
                        inQuotes = false;   // 인용 끝
                    }
                } else {
                    cell.append(c);         // 쉼표·줄바꿈도 값의 일부
                }
            } else if (c == '"') {
                inQuotes = true;
                rowHasContent = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                rowHasContent = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (rowHasContent || cell.length() > 0) {
                    row.add(cell.toString());
                    rows.add(row);
                }
                row = new ArrayList<>();
                cell.setLength(0);
                rowHasContent = false;
            } else {
                cell.append(c);
                rowHasContent = true;
            }
        }
        if (inQuotes) throw new DataFileException("CSV 인용부호(\")가 닫히지 않았습니다.");
        if (rowHasContent || cell.length() > 0) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    /** 값 하나를 CSV 규칙에 맞게 바꾼다. */
    static String escape(String value) {
        if (value == null) return "";
        boolean needQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needQuote) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * 헤더 + 행 전체를 파일에 쓴다.
     * 같은 폴더의 임시 파일에 다 쓴 뒤 원본과 교체해서, 쓰는 도중 꺼져도 원본이 깨지지 않게 한다.
     */
    public static void writeAtomically(Path file, List<String> header, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (List<String> r : rows) appendRow(sb, r);

        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells.get(i)));
        }
        sb.append('\n');
    }
}
