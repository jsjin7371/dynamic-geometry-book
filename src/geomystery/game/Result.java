package geomystery.game;

import geomystery.data.Model.Puzzle;
import geomystery.data.Model.Theorem;

import java.util.ArrayList;
import java.util.List;

/** 세션이 끝난 뒤 결과 화면에 보여 줄 값. */
public final class Result {
    public Session session;
    public int xp, accuracy, total, firstTry, seconds;
    public int previousSeconds = -1;
    public int streak;
    public boolean streakGrew;
    public int tokensGained;
    public Theorem card;
    public boolean cardIsNew;
    public int cardCount, cardTotal;
    public boolean firstClear;
    /** 최종 추리를 해냈으면 보너스 경험치(아니면 0). */
    public int finaleXp;
    /** 이번 풀이 전후의 레벨(레벨 업 축하용). */
    public int levelBefore, levelAfter;
    /** 마지막 사건을 처음 해결했는지(엔딩). */
    public boolean gameCleared;
    /** 사건 의뢰인의 호감도 변화(0~5). 연습이면 heartNpc 가 null. */
    public String heartNpc;
    public int heartsBefore, heartsAfter;
    /** 이번에 새로 열린 후일담의 인물들. */
    public final List<String> epiloguesOpened = new ArrayList<>();
    public final List<Puzzle> wrong = new ArrayList<>();
    public final List<String> messages = new ArrayList<>();
}
