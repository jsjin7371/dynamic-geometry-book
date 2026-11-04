package geometrybook.app;

import geometrybook.domain.StudyProgress;
import geometrybook.gui.quiz.QuizPanel;
import geometrybook.application.quiz.DemoQuizSource;
import geometrybook.domain.LessonTopic;
import geometrybook.gui.auth.LoginPanel;
import geometrybook.gui.home.HomePanel;
import geometrybook.gui.level.LevelMapPanel;
import geometrybook.gui.lesson.LessonPanel;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** 화면 전환과 임시 로그인 세션 수명을 관리한다. */
final class AppNavigation {
    private final JFrame frame;
    private StudyProgress study = new StudyProgress();
    private LessonPanel lesson;
    private QuizPanel quiz;
    AppNavigation(JFrame frame) {
        this.frame = frame;
        login();
    }

    private void show(JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void login() {
        lesson = null;
        quiz = null;
        study = new StudyProgress();
        show(new LoginPanel(this::home));
    }

    private void home() {
        lesson = null;
        quiz = null;
        show(new HomePanel(study, this::levels, this::quizzes, this::login));
    }

    private void levels() {
        lesson = null;
        quiz = null;
        show(new LevelMapPanel(study, this::startLesson, this::home, this::quizzes, this::login));
    }

    private void startLesson(LessonTopic topic) {
        lesson = new LessonPanel(study, topic, this::home, this::levels, this::quizzes, this::login);
        show(lesson);
        lesson.begin();
    }

    private void quizzes() {
        lesson = null;
        quiz = new QuizPanel(new DemoQuizSource(), this::home, this::levels, this::login);
        show(quiz);
    }

    void close() {
        if (lesson != null) lesson.leave(frame::dispose);
        else if (quiz != null) quiz.leave(frame::dispose);
        else frame.dispose();
    }
}
