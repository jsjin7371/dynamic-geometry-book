package geometrybook.storage;

import geometrybook.application.quiz.QuizSource;
import geometrybook.domain.quiz.QuizQuestion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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

    private final Path dataDir;

    public CsvQuizSource(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
    }

    /**
     * 레벨 ID(예: TRIANGLE)의 문제를 questions.csv 행 순서대로 돌려준다. 문제가 없으면 빈 목록.
     * 파일이 없거나 형식이 틀리면 예외가 나고, 퀴즈 화면은 "다시 시도"를 보여 준다.
     */
    @Override
    public List<QuizQuestion> findByLevelId(String levelId) {
        List<QuizQuestion> result = new ArrayList<>();
        for (QuizQuestion question : readAllQuestions()) {
            if (question.levelId.equals(levelId)) result.add(question);
        }
        return List.copyOf(result);
    }

    /** 문제 ID 목록 순서대로 문제를 돌려준다. 파일에 없는 ID는 건너뛴다. (오답노트용) */
    public List<QuizQuestion> findByIds(Collection<String> ids) {
        Map<String, QuizQuestion> byId = new HashMap<>();
        for (QuizQuestion question : readAllQuestions()) byId.put(question.id, question);
        List<QuizQuestion> result = new ArrayList<>();
        for (String id : ids) {
            if (byId.containsKey(id)) result.add(byId.get(id));
        }
        return List.copyOf(result);
    }

    /** 두 파일을 읽어 모든 문제를 questions.csv 행 순서대로 조립한다. */
    private List<QuizQuestion> readAllQuestions() {
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

        List<QuizQuestion> result = new ArrayList<>();
        for (QuestionRow q : questions) {
            List<QuizQuestion.Choice> list = choicesByQuestion.get(q.id);
            list.sort(Comparator.comparingInt(c -> c.choiceNo));
            QuizQuestion question;
            try {
                question = new QuizQuestion(q.id, q.levelId, q.type, q.prompt, q.reviewSceneNo, list);
            } catch (IllegalArgumentException e) {
                throw new DataFileException(CHOICE_FILE + ": 문제 " + q.id + "의 보기 구성이 올바르지 않습니다. "
                        + "(보기 2개 이상, 정답 정확히 1개, OX 문제는 O·X 두 개) - " + e.getMessage());
            }
            result.add(question);
        }
        return result;
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
        protected QuestionRow fromRow(List<String> r) {
            return new QuestionRow(
                    r.get(0).trim(),
                    r.get(1).trim(),
                    QuizQuestion.Type.valueOf(r.get(2).trim()),
                    r.get(3).trim(),
                    Integer.parseInt(r.get(4).trim()));
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
        protected QuizQuestion.Choice fromRow(List<String> r) {
            return new QuizQuestion.Choice(
                    r.get(0).trim(),
                    r.get(1).trim(),
                    Integer.parseInt(r.get(2).trim()),
                    r.get(3).trim(),
                    Boolean.parseBoolean(r.get(4).trim()));
        }
    }
}
