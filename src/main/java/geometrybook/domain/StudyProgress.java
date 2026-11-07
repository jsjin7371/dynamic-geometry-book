package geometrybook.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

/** 실행 중에만 유지하는 레벨별 학습 기록. 파일 저장 및 포인트 계산은 담당하지 않는다. */
public final class StudyProgress {
    private final Set<LessonTopic> completed = new HashSet<>();
    private final Set<LocalDate> dates = new HashSet<>();
    public boolean isCompleted() {
        return completed.contains(LessonTopic.TRIANGLE);
    }

    public Set<LocalDate> studyDates() {
        return Set.copyOf(dates);
    }

    public Session start() {
        return start(LessonTopic.TRIANGLE);
    }

    public boolean isCompleted(LessonTopic topic) {
        return completed.contains(topic);
    }

    public int completedCount() {
        return completed.size();
    }

    public Session start(LessonTopic topic) {
        if (topic.ordinal() > 0) {
            LessonTopic previous = LessonTopic.values()[topic.ordinal() - 1];
            if (!isCompleted(previous)) {
                throw new IllegalStateException(previous.title() + "을 먼저 완료해 주세요.");
            }
        }
        return new Session(topic, completed.contains(topic));
    }

    public void complete(Session session) {
        if (!session.allCorrect()) throw new IllegalStateException("세 문제를 모두 맞혀야 합니다.");
        if (session.saved) return;
        session.saved = true;
        completed.add(session.topic);
        dates.add(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    /** 설명 확인과 답안은 각 시도에만 속하며 새 학습을 시작하면 초기화한다. */
    public static final class Session {
        public final boolean review;
        private boolean explanationConfirmed;
        private boolean saved;
        private final boolean[] correct = new boolean[3];
        private final LessonTopic topic;
        private Session(LessonTopic topic, boolean review) {
            this.topic = topic;
            this.review = review;
        }

        public void confirmExplanation() {
            explanationConfirmed = true;
        }

        public boolean canQuiz() {
            return review || explanationConfirmed;
        }

        public boolean isCorrect(int index) {
            return correct[index];
        }

        public boolean submit(int index, int choice) {
            if (!canQuiz()) throw new IllegalStateException("설명을 먼저 확인해 주세요.");
            if (choice < 0 || choice > 2) throw new IllegalArgumentException("보기를 선택해 주세요.");
            if (!saved) correct[index] |= choice == topic.answer(index);
            return correct[index];
        }

        public boolean allCorrect() {
            return correct[0] && correct[1] && correct[2];
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
