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
 * - 단순 split(",")을 쓰지 않고, CSV 문법에 맞지 않는 줄은 오류로 처리한다
 */
public final class CsvCodec {

    private CsvCodec() {}

    /** 읽는 중 지금 어디에 있는지 */
    private enum State {
        FIELD_START,   // 칸이 막 시작됨
        UNQUOTED,      // 따옴표 없는 칸 안
        QUOTED,        // 따옴표로 감싼 칸 안
        AFTER_QUOTE    // 감싼 칸의 닫는 따옴표 바로 뒤
    }

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
        State state = State.FIELD_START;
        int line = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean newline = (c == '\n' || c == '\r');
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;   // CRLF는 한 번만

            switch (state) {
                case FIELD_START:
                    if (c == '"') {
                        state = State.QUOTED;
                    } else if (c == ',') {
                        row.add("");
                    } else if (newline) {
                        if (!row.isEmpty()) {          // "a," 처럼 쉼표로 끝난 줄
                            row.add("");
                            rows.add(row);
                            row = new ArrayList<>();
                        }                              // 아니면 빈 줄 → 건너뜀
                        line++;
                    } else {
                        cell.append(c);
                        state = State.UNQUOTED;
                    }
                    break;

                case UNQUOTED:
                    if (c == ',') {
                        row.add(cell.toString());
                        cell.setLength(0);
                        state = State.FIELD_START;
                    } else if (newline) {
                        row.add(cell.toString());
                        cell.setLength(0);
                        rows.add(row);
                        row = new ArrayList<>();
                        state = State.FIELD_START;
                        line++;
                    } else if (c == '"') {
                        throw new DataFileException("CSV " + line + "행: 따옴표(\")는 칸 맨 앞에서만 쓸 수 있습니다.");
                    } else {
                        cell.append(c);
                    }
                    break;

                case QUOTED:
                    if (c == '"') {
                        if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                            cell.append('"');          // "" → "
                            i++;
                        } else {
                            state = State.AFTER_QUOTE; // 인용 끝
                        }
                    } else {
                        if (newline) {
                            cell.append('\n');         // 값 안의 줄바꿈 (CRLF도 \n 하나로)
                            line++;
                        } else {
                            cell.append(c);
                        }
                    }
                    break;

                case AFTER_QUOTE:
                    if (c == ',') {
                        row.add(cell.toString());
                        cell.setLength(0);
                        state = State.FIELD_START;
                    } else if (newline) {
                        row.add(cell.toString());
                        cell.setLength(0);
                        rows.add(row);
                        row = new ArrayList<>();
                        state = State.FIELD_START;
                        line++;
                    } else {
                        throw new DataFileException("CSV " + line + "행: 닫는 따옴표 뒤에는 쉼표나 줄바꿈만 올 수 있습니다.");
                    }
                    break;
            }
        }

        // 파일 끝 처리
        if (state == State.QUOTED) {
            throw new DataFileException("CSV " + line + "행: 따옴표(\")가 닫히지 않았습니다.");
        }
        if (state != State.FIELD_START || !row.isEmpty()) {
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
     * (여러 프로그램이 동시에 쓰는 것은 호출하는 쪽에서 잠금으로 막는다 → StudentRepository)
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
