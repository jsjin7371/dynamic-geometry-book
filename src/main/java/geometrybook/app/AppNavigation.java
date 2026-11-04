package geometrybook.app;

import geometrybook.domain.StudyProgress;
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
        study = new StudyProgress();
        show(new LoginPanel(this::home));
    }

    private void home() {
        lesson = null;
        show(new HomePanel(study, this::levels, this::login));
    }

    private void levels() {
        lesson = null;
        show(new LevelMapPanel(study, this::startLesson, this::home, this::login));
    }

    private void startLesson(LessonTopic topic) {
        lesson = new LessonPanel(study, topic, this::home, this::levels, this::login);
        show(lesson);
        lesson.begin();
    }

    void close() {
        if (lesson != null) lesson.leave(frame::dispose);
        else frame.dispose();
    }
}
