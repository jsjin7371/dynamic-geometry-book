package geometrybook.app;

import geometrybook.domain.StudyProgress;
import geometrybook.gui.quiz.QuizPanel;
import geometrybook.domain.LessonTopic;
import geometrybook.gui.auth.LoginPanel;
import geometrybook.gui.home.HomePanel;
import geometrybook.gui.level.LevelMapPanel;
import geometrybook.gui.lesson.LessonPanel;
import geometrybook.storage.CsvQuizSource;
import geometrybook.storage.AuthService;
import geometrybook.storage.ProgressRepository;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** 화면 전환과 임시 로그인 세션 수명을 관리한다. */
final class AppNavigation {
    private final JFrame frame;
    private final AuthService auth = new AuthService(Path.of("data"));
    private final ProgressRepository progress = new ProgressRepository(Path.of("data"));
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
        auth.logout();
        lesson = null;
        quiz = null;
        study = new StudyProgress();
        show(new LoginPanel(auth, this::afterLogin));
    }

    /** 로그인한 학생의 완료 기록을 불러온다. 레벨을 처음 완료하면 progress.csv에 저장한다. */
    private void afterLogin() {
        String studentId = auth.getCurrentStudent().getId();
        try {
            Set<String> doneIds = progress.findCompletedLevelIds(studentId);
            List<LessonTopic> done = new ArrayList<>();
            for (LessonTopic topic : LessonTopic.values()) {
                if (doneIds.contains(topic.name())) done.add(topic);
            }
            study = new StudyProgress(done, topic -> progress.markCompleted(studentId, topic.name()));
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(frame, "학습 기록(progress.csv)을 읽지 못했어요.\n" + e.getMessage(),
                    "불러오기 실패", JOptionPane.ERROR_MESSAGE);
            login();
            return;
        }
        home();
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
        quiz = new QuizPanel(new CsvQuizSource(Path.of("data")), this::home, this::levels, this::login);
        show(quiz);
    }

    void close() {
        if (lesson != null) lesson.leave(frame::dispose);
        else if (quiz != null) quiz.leave(frame::dispose);
        else frame.dispose();
    }
}
