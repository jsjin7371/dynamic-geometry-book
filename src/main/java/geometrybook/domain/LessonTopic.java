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
            new String[]{"정사각형은 네 변과 네 꼭짓점을 가져요.", "정사각형은 네 변의 길이가 모두 같아요.", "정사각형의 네 각은 각각 90도인 직각이에요."}),
    RECTANGLE("직사각형", "마름모", "네 각이 모두 직각인 사각형이에요. 마주 보는 변의 길이가 같아요.",
            new String[]{"직사각형의 네 각은 어떤 각인가요?", "직사각형에서 길이가 같은 변은 어느 것인가요?", "가로가 5cm, 세로가 3cm인 직사각형의 넓이는 얼마인가요?"},
            new String[][]{{"직각", "모두 60도", "모두 180도"}, {"이웃한 변은 항상 같아요", "마주 보는 변", "같은 변이 없어요"}, {"8cm²", "16cm²", "15cm²"}},
            new int[]{0, 1, 2},
            new String[]{"직사각형의 네 각은 모두 90도인 직각이에요.", "직사각형은 마주 보는 두 변의 길이가 같아요.", "직사각형의 넓이는 가로 × 세로이므로 5 × 3 = 15cm²예요."}),
    RHOMBUS("마름모", "사다리꼴", "네 변의 길이가 모두 같은 사각형이에요. 마주 보는 각의 크기도 같아요.",
            new String[]{"마름모의 네 변의 길이는 어떤가요?", "마름모에서 마주 보는 두 각의 크기는 어떤가요?", "마름모의 한 변이 4cm이면 둘레는 얼마인가요?"},
            new String[][]{{"모두 달라요", "모두 같아요", "한 쌍만 같아요"}, {"서로 같아요", "항상 서로 달라요", "항상 90도예요"}, {"8cm", "12cm", "16cm"}},
            new int[]{1, 0, 2},
            new String[]{"마름모는 네 변의 길이가 모두 같아요.", "마름모의 마주 보는 각은 같지만 모든 각이 반드시 직각인 것은 아니에요.", "네 변이 모두 4cm이므로 둘레는 4 × 4 = 16cm예요."}),
    TRAPEZOID("사다리꼴", "평행사변형", "마주 보는 한 쌍의 변이 서로 평행한 사각형이에요. 평행한 두 변 사이의 수직 거리가 높이예요.",
            new String[]{"사다리꼴에서 반드시 찾을 수 있는 것은 무엇인가요?", "사다리꼴의 높이는 어떤 길이인가요?", "윗변 3cm, 아랫변 5cm, 높이 2cm인 사다리꼴의 넓이는 얼마인가요?"},
            new String[][]{{"평행한 한 쌍의 변", "길이가 모두 같은 네 변", "직각 네 개"}, {"기울어진 옆변의 길이", "평행한 두 변 사이의 수직 거리", "둘레"}, {"16cm²", "10cm²", "8cm²"}},
            new int[]{0, 1, 2},
            new String[]{"사다리꼴에는 서로 평행한 한 쌍의 변이 있어요.", "높이는 평행한 두 변 사이를 수직으로 잰 거리예요.", "넓이는 (윗변 + 아랫변) × 높이 ÷ 2이므로 (3 + 5) × 2 ÷ 2 = 8cm²예요."}),
    PARALLELOGRAM("평행사변형", "원", "마주 보는 두 쌍의 변이 서로 평행하고 마주 보는 두 각의 크기가 같은 사각형이에요. 넓이는 밑변 × 높이예요.",
            new String[]{"평행사변형에서 마주 보는 두 각의 크기는 어떤가요?", "평행사변형의 한쪽을 잘라 옮기면 어떤 도형이 될까요?", "밑변이 6cm, 높이가 4cm인 평행사변형의 넓이는 얼마일까요?"},
            new String[][]{{"서로 같아요", "서로 달라요", "모두 직각이에요"}, {"삼각형", "직사각형", "원"}, {"10cm²", "20cm²", "24cm²"}},
            new int[]{0, 1, 2},
            new String[]{"평행사변형에서 마주 보는 두 각의 크기는 서로 같아요.", "잘라 낸 삼각형을 반대쪽에 붙이면 직사각형이 돼요.", "평행사변형의 넓이는 밑변 × 높이이므로 6 × 4 = 24cm²예요."}),
    CIRCLE("원", "", "원의 중심에서 원 위의 모든 점까지의 거리는 같아요. 지름은 반지름의 두 배예요.",
            new String[]{"원의 중심에서 원 위의 한 점까지 이은 선분은 무엇인가요?", "한 원에서 모든 반지름의 길이는 어떤가요?", "반지름이 3cm인 원의 지름은 얼마인가요?"},
            new String[][]{{"반지름", "둘레", "꼭짓점"}, {"모두 달라요", "모두 같아요", "두 개만 같아요"}, {"3cm", "9cm", "6cm"}},
            new int[]{0, 1, 2},
            new String[]{"중심에서 원 위의 한 점까지 이은 선분이 반지름이에요.", "한 원의 반지름은 모두 길이가 같아요.", "지름은 반지름의 두 배이므로 3 × 2 = 6cm예요."});

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
