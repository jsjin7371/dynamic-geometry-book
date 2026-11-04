package geometrybook.domain;

/** 화면 문구와 고정 확인 문제를 담는 임시 콘텐츠. 파일 입출력과 분리한다. */
public enum LessonTopic {
    TRIANGLE("삼각형", "정사각형", "변과 꼭짓점은 3개, 세 내각의 합은 180도예요.",
            new String[]{"삼각형의 변은 몇 개일까요?", "삼각형의 꼭짓점은 몇 개일까요?", "삼각형의 세 내각을 모두 더하면 몇 도일까요?"},
            new String[][]{{"2개", "3개", "4개"}, {"1개", "2개", "3개"}, {"180도", "90도", "360도"}},
            new int[]{1, 2, 0},
            new String[]{"삼각형의 변은 3개예요.", "삼각형의 꼭짓점은 3개예요.", "세 내각을 모으면 곧은 각인 180도가 돼요."}),
    SQUARE("정사각형", "직사각형", "네 변의 길이가 같고, 네 각이 모두 직각인 사각형이에요.",
            new String[]{"정사각형의 변은 몇 개일까요?", "정사각형의 네 변의 길이는 어떤가요?", "정사각형의 한 내각은 몇 도일까요?"},
            new String[][]{{"3개", "4개", "5개"}, {"모두 달라요", "두 변만 같아요", "모두 같아요"}, {"90도", "60도", "180도"}},
            new int[]{1, 2, 0},
            new String[]{"정사각형은 네 변과 네 꼭짓점을 가져요.", "정사각형은 네 변의 길이가 모두 같아요.", "정사각형의 네 각은 각각 90도인 직각이에요."});

    private final String title;
    private final String nextTitle;
    private final String summary;
    private final String[] prompts;
    private final String[][] choices;
    private final int[] answers;
    private final String[] explanations;

    LessonTopic(String title, String nextTitle, String summary, String[] prompts,
                String[][] choices, int[] answers, String[] explanations) {
        this.title = title;
        this.nextTitle = nextTitle;
        this.summary = summary;
        this.prompts = prompts;
        this.choices = choices;
        this.answers = answers;
        this.explanations = explanations;
    }

    public String title() { return title; }
    public String nextTitle() { return nextTitle; }
    public String summary() { return summary; }
    public int number() { return ordinal() + 1; }
    public String prompt(int index) { return prompts[index]; }
    public String choice(int question, int option) { return choices[question][option]; }
    public int answer(int index) { return answers[index]; }
    public String explanation(int index) { return explanations[index]; }
}
