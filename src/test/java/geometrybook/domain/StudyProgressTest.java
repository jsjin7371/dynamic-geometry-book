package geometrybook.domain;

/** 외부 테스트 라이브러리 없이 실행하는 학습 상태 검증. */
public final class StudyProgressTest {
    public static void main(String[] args) {
        StudyProgress study = new StudyProgress();
        try {
            study.start(Level.SQUARE);
            throw new AssertionError("선행 학습 없이 정사각형 시작");
        } catch (IllegalStateException expected) {
        }
        StudyProgress.Session first = study.start();
        check(!first.review && !first.canQuiz(), "최초 설명 확인 전 문제 진입 제한");
        try {
            study.complete(first);
            throw new AssertionError("미완료 저장 허용");
        }
        catch (IllegalStateException expected) {
        }
        first.confirmExplanation();
        check(!first.submit(0, 0), "오답 처리");
        check(first.submit(0, 1), "오답 재도전");
        check(first.submit(0, 2), "이미 맞힌 문제 유지");
        check(!study.start().isCorrect(0), "새 시도에서 이전 답안 폐기");
        first.submit(1, 2);
        first.submit(2, 0);
        study.complete(first);
        study.complete(first);
        check(study.isCompleted() && study.studyDates().size() == 1, "완료 및 같은 날 중복 방지");
        StudyProgress.Session review = study.start();
        check(review.review && review.canQuiz() && !review.allCorrect(), "복습 설명 생략 및 답안 초기화");
        review.submit(0, 1);
        review.submit(1, 2);
        review.submit(2, 0);
        study.complete(review);
        check(study.studyDates().size() == 1, "복습 학습일 중복 제거");
        check(!new StudyProgress().isCompleted(), "새 로그인 세션에서 초기화");
        StudyProgress.Session square = study.start(Level.SQUARE);
        check(!square.review && !square.canQuiz(), "정사각형 최초 학습");
        square.confirmExplanation();
        check(!square.submit(0, 0), "정사각형 오답");
        square.submit(0, 1);
        square.submit(1, 2);
        square.submit(2, 0);
        study.complete(square);
        check(study.completedCount() == 2, "레벨별 완료 기록 분리");
        check(study.start(Level.SQUARE).review, "정사각형 복습 가능");
        check(!study.start(Level.SQUARE).allCorrect(), "복습 답안 초기화");
        try {
            new StudyProgress().start(Level.PARALLELOGRAM);
            throw new AssertionError("선행 학습 없이 평행사변형 시작");
        } catch (IllegalStateException expected) {
        }
        for (Level topic : new Level[]{Level.RECTANGLE, Level.RHOMBUS, Level.TRAPEZOID}) {
            StudyProgress.Session session = study.start(topic);
            session.confirmExplanation();
            for (int i = 0; i < 3; i++) session.submit(i, topic.answer(i));
            study.complete(session);
        }
        StudyProgress.Session parallelogram = study.start(Level.PARALLELOGRAM);
        check(!parallelogram.review && !parallelogram.canQuiz(), "평행사변형 최초 학습");
        parallelogram.confirmExplanation();
        check(!parallelogram.submit(0, 1), "평행사변형 오답");
        check(parallelogram.submit(0, 0), "평행사변형 오답 재도전");
        try {
            study.complete(parallelogram);
            throw new AssertionError("평행사변형 미완료 저장 허용");
        } catch (IllegalStateException expected) {
        }
        parallelogram.submit(1, 1);
        parallelogram.submit(2, 2);
        study.complete(parallelogram);
        check(study.completedCount() == 6, "평행사변형 완료 기록");
        check(study.isCompleted(Level.TRIANGLE) && study.isCompleted(Level.SQUARE), "기존 레슨 완료 상태 유지");
        StudyProgress.Session parallelogramReview = study.start(Level.PARALLELOGRAM);
        check(parallelogramReview.review && parallelogramReview.canQuiz() && !parallelogramReview.allCorrect(), "평행사변형 복습 답안 초기화");
        StudyProgress sequential = new StudyProgress();
        for (Level topic : Level.values()) {
            if (topic.ordinal() > 0) {
                try {
                    new StudyProgress().start(topic);
                    throw new AssertionError("선행 완료 없이 시작: " + topic);
                } catch (IllegalStateException expected) { }
            }
            if (topic.ordinal() + 1 < Level.values().length) {
                try {
                    sequential.start(Level.values()[topic.ordinal() + 1]);
                    throw new AssertionError("순서 건너뛰기 허용");
                } catch (IllegalStateException expected) { }
            }
            StudyProgress.Session session = sequential.start(topic);
            session.confirmExplanation();
            for (int i = 0; i < 3; i++) {
                check(!topic.prompt(i).isBlank() && !topic.explanation(i).isBlank(), "문제와 해설 필수");
                int answer = topic.answer(i);
                check(answer >= 0 && answer < 3, "정답 인덱스");
                for (int j = 0; j < 3; j++) check(!topic.choice(i, j).isBlank(), "보기 필수");
                session.submit(i, answer);
            }
            sequential.complete(session);
            check(sequential.start(topic).review, "완료 후 복습");
        }
        check(sequential.completedCount() == 7, "일곱 레슨 완료");
        check(Level.CIRCLE.nextTitle().isEmpty(), "마지막 레슨");
        System.out.println("StudyProgress tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
