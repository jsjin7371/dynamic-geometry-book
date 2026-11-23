package geomystery.data;

import java.util.ArrayList;
import java.util.List;

/** CSV 콘텐츠에서 읽어 오는 도메인 모델 모음. */
public final class Model {
    private Model() {}

    public static class Chapter {
        public String id, label, title, subtitle, npc, scene;
        public int order;
    }

    public static class Lesson {
        public String id, chapterId, title, caseName, location, npc, scene, theoremId, tip, resolution, nextLine;
        public List<String> memo = new ArrayList<>();
        public int order, xp, minutes;
        public boolean boss;
        /** 전체 학습 경로에서의 순서(0부터). */
        public int globalIndex;
    }

    public static class Line {
        /** emotion: happy, surprised, puzzled, worried, angry (비어 있으면 표정 없음). */
        public String lessonId, speaker, text, emotion = "";
        public int seq;
    }

    /**
     * 수수께끼 한 문제.
     * <pre>
     * type     answer              choices
     * number   정수                 —
     * choice   보기 중 하나          보기1|보기2|...
     * order    (비워 둠)            올바른 순서대로 쓴 추리 단계 (화면에는 섞여서 나온다)
     * tf       참|이유 또는 거짓|이유  이유 후보들
     * expr     식 (3x+30, 5/2, 2√3)  —
     * </pre>
     */
    public static class Puzzle {
        public static final String NUMBER = "number", CHOICE = "choice", ORDER = "order", TF = "tf", EXPR = "expr";

        public String id, lessonId, title, noteTitle, note, hazel, diagram, type, answer, unit, answerLabel, explanation;
        public List<String> choices = new ArrayList<>();
        public List<String> hints = new ArrayList<>();
        /** 풀이 다시 보기 단계(비어 있으면 해설에서 자동으로 만든다). '@aux 문장' 은 보조선, '@ans 문장' 은 답 쓰기. */
        public List<String> steps = new ArrayList<>();
        /** 특정 오답에 대한 맞춤 피드백. */
        public List<Feedback> feedback = new ArrayList<>();
        public int order, number;

        public boolean isChoice() { return CHOICE.equals(type); }
        public boolean isOrder() { return ORDER.equals(type); }
        public boolean isTf() { return TF.equals(type); }
        public boolean isExpr() { return EXPR.equals(type); }
        public boolean isNumber() { return !isChoice() && !isOrder() && !isTf() && !isExpr(); }

        /** tf 문제의 판단(참/거짓). */
        public String tfVerdict() {
            int i = answer.indexOf('|');
            return (i < 0 ? answer : answer.substring(0, i)).trim();
        }

        /** tf 문제의 올바른 이유. */
        public String tfReason() {
            int i = answer.indexOf('|');
            return i < 0 ? "" : answer.substring(i + 1).trim();
        }

        /** 사람이 읽을 정답 표기. */
        public String answerText() {
            if (isOrder()) return String.join("  →  ", choices);
            if (isTf()) return tfVerdict() + " — " + tfReason();
            return answer + (unit == null ? "" : unit.trim());
        }

        public String typeName() { return typeName(type); }

        public static String typeName(String type) {
            if (type == null) return "숫자";
            switch (type) {
                case CHOICE: return "객관식";
                case ORDER: return "추리 순서";
                case TF: return "참·거짓";
                case EXPR: return "식 입력";
                default: return "숫자";
            }
        }
    }

    /** "오답=대사" 한 쌍. tag는 misconceptions.csv 의 실수 유형. */
    public static class Feedback {
        public String value, tag, text;
    }

    /** 자주 하는 실수 유형. */
    public static class Misconception {
        public String id, name, tip;
    }

    /** 최종 사건 뒤의 '최종 추리'(증언 속 모순을 정리 카드로 반박) 대본 한 줄. */
    public static class FinaleLine {
        public static final String INTRO = "intro", CLAIM = "claim", LIE = "lie", REVEAL = "reveal";
        public String lessonId, kind, speaker, text, theoremId;
        public int seq;

        public boolean isTestimony() { return CLAIM.equals(kind) || LIE.equals(kind); }
    }

    /** 범인 자백 만화 한 칸. kind: motive(동기), method(수법), arrest(체포). prop: 동기 칸은 소품 이름, 수법 칸은 도형 이름. */
    public static class Confession {
        public static final String MOTIVE = "motive", METHOD = "method", ARREST = "arrest";
        public String lessonId, kind, caption, prop;
        public int panel;
    }

    public static class Theorem {
        public String id, name, description, icon;
    }

    public static class Npc {
        public String id, name, role, image;
    }

    public static class Quest {
        public String id, text, type;
        public int target, reward;
    }

    public static class Badge {
        public String id, name, description, stat, icon;
        public int target;
    }

    public static class Bot {
        public String name;
        public int color, base, daily;
    }

    public static class Quote {
        public String text, author;
    }

    /**
     * 정리 탐구(도형 직접 조작) 설정. 정답을 맞힌 뒤나 도감에서 열린다.
     * drag 형식: {@code 점 종류 [인자] [격자]} 를 '|'로 나열.
     * 종류: free, hline(가로로만), vline(세로로만), circle O(원 위), arc O 시작° 끝°(원의 일부 위).
     * 끝에 숫자를 붙이면 그 픽셀 간격으로 맞춘다(예: {@code B hline 40}).
     * relation: '|'로 줄을 나누고 {식} 안에 ang(A,O,B), len(A,B) 를 쓰면 실시간으로 계산된다.
     * {kind:식} 은 각의 종류(예각·직각·둔각·평각)를 쓴다.
     */
    public static class Explore {
        public String theoremId, diagram, relation, caption;
        public List<Drag> drags = new ArrayList<>();
    }

    public static class Drag {
        public String point, kind;
        public double[] args = new double[0];
        public String center;
        public double snap;
    }

    /** 도형 스크립트 한 줄. */
    public static class Cmd {
        public String layer, op;
        public String[] args;
    }
}
