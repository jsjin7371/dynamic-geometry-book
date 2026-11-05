package geometrybook.storage;

import geometrybook.application.quiz.QuizSource;
import geometrybook.domain.quiz.QuizQuestion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 심화 퀴즈 조회 계약(QuizSource)의 CSV 구현.
 * data/questions.csv(문제)와 data/answer_choices.csv(보기)를 읽어 QuizQuestion으로 조립한다.
 *
 * 퀴즈 화면은 백그라운드에서 findByLevelId()를 부르고, 실패하면 "다시 시도"를 보여 준다.
 * 그래서 조회할 때마다 파일을 새로 읽는다. 파일을 고친 뒤 다시 시도하면 바로 반영되고,
 * 공유하는 상태가 없어 여러 스레드에서 불러도 안전하다. (콘텐츠 파일은 작아서 비용이 크지 않다.)
 *
 * 사용 예:
 *   QuizSource source = new CsvQuizSource(Path.of("data"));
 *   List<QuizQuestion> questions = source.findByLevelId("TRIANGLE");
 */
public final class CsvQuizSource implements QuizSource {

    public static final String QUESTION_FILE = "questions.csv";
    public static final String CHOICE_FILE = "answer_choices.csv";

    /** 퀴즈 화면(QuizPanel)이 조회하는 레벨 ID. 여기에 없는 levelId는 오타로 보고 오류를 낸다. */
    public static final List<String> LEVEL_IDS = List.of(
            "TRIANGLE", "SQUARE", "RECTANGLE", "RHOMBUS", "TRAPEZOID", "PARALLELOGRAM", "CIRCLE");

    private final Path dataDir;

    public CsvQuizSource(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
    }

    /**
     * 레벨 ID(예: TRIANGLE)의 문제를 questions.csv 행 순서대로 돌려준다. 문제가 없으면 빈 목록.
     * 두 파일 중 하나라도 규칙에 맞지 않으면 DataFileException.
     */
    @Override
    public List<QuizQuestion> findByLevelId(String levelId) {
        if (levelId == null || levelId.isBlank()) {
            throw new IllegalArgumentException("레벨 ID가 비어 있습니다.");
        }
        List<QuestionRow> questions = new QuestionReader(dataDir).readAll();
        List<QuizQuestion.Choice> choices = new ChoiceReader(dataDir).readAll();

        // 보기를 문제별로 묶는다. 없는 문제를 가리키는 보기(FK 위반)는 오류.
        Map<String, List<QuizQuestion.Choice>> choicesByQuestion = new LinkedHashMap<>();
        for (QuestionRow q : questions) {
            choicesByQuestion.put(q.id, new ArrayList<>());
        }
        for (QuizQuestion.Choice c : choices) {
            List<QuizQuestion.Choice> list = choicesByQuestion.get(c.questionId);
            if (list == null) {
                throw new DataFileException(CHOICE_FILE + ": 보기 " + c.id + "의 questionId " + c.questionId
                        + "가 " + QUESTION_FILE + "에 없습니다.");
            }
            list.add(c);
        }

        // 모든 문제를 조립해 데이터 전체를 검사하고, 요청한 레벨만 돌려준다.
        List<QuizQuestion> result = new ArrayList<>();
        for (QuestionRow q : questions) {
            List<QuizQuestion.Choice> list = choicesByQuestion.get(q.id);
            list.sort(Comparator.comparingInt(c -> c.choiceNo));
            if (q.type == QuizQuestion.Type.OX && list.size() == 2
                    && !(list.get(0).content.equals("O") && list.get(1).content.equals("X"))) {
                throw new DataFileException(CHOICE_FILE + ": OX 문제 " + q.id + "의 보기는 1번 O, 2번 X여야 합니다.");
            }
            QuizQuestion question;
            try {
                question = new QuizQuestion(q.id, q.levelId, q.type, q.prompt, q.reviewSceneNo, list);
            } catch (IllegalArgumentException e) {
                throw new DataFileException(CHOICE_FILE + ": 문제 " + q.id + "의 보기 구성이 올바르지 않습니다. "
                        + "(보기 2개 이상, 정답 정확히 1개, OX 문제는 O·X 두 개) - " + e.getMessage());
            }
            if (question.levelId.equals(levelId)) {
                result.add(question);
            }
        }
        return List.copyOf(result);
    }

    /** questions.csv 한 행 (보기와 합치기 전의 중간 값) */
    private static final class QuestionRow {
        final String id;
        final String levelId;
        final QuizQuestion.Type type;
        final String prompt;
        final int reviewSceneNo;

        QuestionRow(String id, String levelId, QuizQuestion.Type type, String prompt, int reviewSceneNo) {
            this.id = id;
            this.levelId = levelId;
            this.type = type;
            this.prompt = prompt;
            this.reviewSceneNo = reviewSceneNo;
        }
    }

    /** questions.csv 읽기. 열: id, levelId, type, prompt, reviewSceneNo */
    private static final class QuestionReader extends CsvRecordReader<QuestionRow> {
        private static final List<String> HEADER = List.of("id", "levelId", "type", "prompt", "reviewSceneNo");

        QuestionReader(Path dataDir) {
            super(dataDir, QUESTION_FILE);
        }

        @Override
        protected List<String> header() {
            return HEADER;
        }

        @Override
        protected QuestionRow fromRow(List<String> r, String where) {
            return new QuestionRow(
                    requireText(r.get(0), where, "id"),
                    parseLevelId(r.get(1), where),
                    parseType(r.get(2), where),
                    requireText(r.get(3), where, "prompt"),
                    requirePositiveInt(r.get(4), where, "reviewSceneNo"));
        }

        private static String parseLevelId(String value, String where) {
            String levelId = requireText(value, where, "levelId");
            if (!LEVEL_IDS.contains(levelId)) {
                throw new DataFileException(where + "levelId는 " + String.join(", ", LEVEL_IDS)
                        + " 중 하나여야 합니다: " + levelId);
            }
            return levelId;
        }

        private static QuizQuestion.Type parseType(String value, String where) {
            for (QuizQuestion.Type type : QuizQuestion.Type.values()) {
                if (type.name().equals(value.trim())) return type;
            }
            throw new DataFileException(where + "type은 OX 또는 MULTIPLE_CHOICE여야 합니다: " + value);
        }

        @Override
        protected void validate(List<QuestionRow> rows) {
            Set<String> ids = new HashSet<>();
            for (QuestionRow q : rows) {
                if (!ids.add(q.id)) {
                    throw new DataFileException(QUESTION_FILE + ": id 중복 " + q.id);
                }
            }
        }
    }

    /** answer_choices.csv 읽기. 열: id, questionId, choiceNo, content, isCorrect */
    private static final class ChoiceReader extends CsvRecordReader<QuizQuestion.Choice> {
        private static final List<String> HEADER = List.of("id", "questionId", "choiceNo", "content", "isCorrect");

        ChoiceReader(Path dataDir) {
            super(dataDir, CHOICE_FILE);
        }

        @Override
        protected List<String> header() {
            return HEADER;
        }

        @Override
        protected QuizQuestion.Choice fromRow(List<String> r, String where) {
            return new QuizQuestion.Choice(
                    requireText(r.get(0), where, "id"),
                    requireText(r.get(1), where, "questionId"),
                    requirePositiveInt(r.get(2), where, "choiceNo"),
                    requireText(r.get(3), where, "content"),
                    requireBoolean(r.get(4), where, "isCorrect"));
        }

        @Override
        protected void validate(List<QuizQuestion.Choice> choices) {
            Set<String> ids = new HashSet<>();
            Map<String, Set<Integer>> choiceNosByQuestion = new HashMap<>();
            for (QuizQuestion.Choice c : choices) {
                if (!ids.add(c.id)) {
                    throw new DataFileException(CHOICE_FILE + ": id 중복 " + c.id);
                }
                if (!choiceNosByQuestion.computeIfAbsent(c.questionId, k -> new HashSet<>()).add(c.choiceNo)) {
                    throw new DataFileException(CHOICE_FILE + ": 같은 문제의 choiceNo 중복 "
                            + c.questionId + " " + c.choiceNo);
                }
            }
        }
    }
}
