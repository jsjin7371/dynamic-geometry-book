# 콘텐츠 CSV 형식

`data/`의 CSV만 고치면 코드를 건드리지 않고 사건·대사·문제를 늘릴 수 있다. 고친 뒤에는 `./run.sh validate`로 검사한다.

## 문제 유형 (`puzzles.csv` 의 `type`)
| type | 답안 화면 | `answer` | `choices` |
|---|---|---|---|
| `number` | 숫자 키패드 | 0~9999 정수 | — |
| `choice` | 보기 4개 | 보기 중 하나 | `보기1\|보기2\|…` |
| `expr` | 식 키패드 (x, √, π, ², 괄호, 분수) | 식: `x/2`, `x+60`, `√2`, `6π` — 값이 같은 식은 모두 정답(`2√3` = `√12`) | — |
| `tf` | 참/거짓 + 이유 고르기 | `참\|올바른 이유` 또는 `거짓\|올바른 이유` | 이유 후보들 |
| `order` | 추리 단계 순서 맞추기 | (비움) | **올바른 순서대로** 쓴 단계 — 화면에서는 섞여 나온다 |

## 오답 맞춤 피드백 (`puzzles.csv` 의 `feedback`)
`오답=대사|오답=#실수유형 대사` 형식. 예) `110=#central 110°는 중심각이야. 원주각은 그 절반!`
- 식 문제는 값이 같은 식이면 일치로 본다. 참/거짓은 `참=…`처럼 판단만 적어도 된다.
- 적어 두지 않은 오답도 규칙으로 진단한다: 보각(180°−답)·여각(90°−답)·한 바퀴(360°−답)·절반/두 배, 참거짓의 근거 오류, 추리 순서가 처음 어긋난 단계.
- `#실수유형`은 `misconceptions.csv`에 정의하며, 추리 분석 화면의 "자주 하는 실수"에 모인다.

## 파일별 형식

- `chapters.csv`, `lessons.csv`, `dialogues.csv`, `puzzles.csv`, `theorems.csv`, `npcs.csv`
  - `dialogues.csv`의 `emotion`: `happy`, `surprised`, `puzzled`, `worried`, `angry` (비우면 표정 없음)
  - `puzzles.csv`의 `steps`: 풀이 다시 보기 단계를 `|`로 직접 쓸 때(비우면 해설에서 자동으로 나눔). `@aux 문장`은 보조선 긋기, `@ans 문장`은 칠판에 답 쓰기
- `diagrams.csv` — 도형 스크립트 (`pt`, `polar`, `apex`, `seg`, `angle`, `right`, `dial`… 레이어 `base`/`aux`). 문법은 `DiagramView` 주석 참고, `./run.sh editor`로 편집
  - `sector O 반지름 시작° 끝° [색] [투명도]` 부채꼴 칠하기, `wedge O A B [색] [투명도]` 점 A에서 B까지(반시계) 부채꼴 칠하기
  - `tan 이름 O 반지름 P 1|-1` 원 밖의 점 P에서 그은 접선의 접점(정리 탐구에서 P를 끌면 접점이 따라 움직인다)
- `confessions.csv` — 범인 자백 만화: `lesson,panel,kind,caption,prop`. kind는 `motive`(동기, prop=`lamp`/`gear`/`rope`/`lens` 소품), `method`(수법, prop=범행 장면 `channel`/`gear`/`rope`/`disc`, 또는 도형 이름 — 그러면 칠판에 그 도형을 그림), `arrest`(체포: 의뢰인과 장면 배경은 그 장에서 정해진다)
- `epilogues.csv` — 후일담: `npc,seq,speaker,text,emotion`. 그 인물의 호감도 하트가 다섯 개가 되면 탐정 사무소에서 열린다
- `finales.csv` — 최종 추리 대본: `lesson,seq,kind,speaker,text,theorem`. kind는 `intro`(사건 정리), `claim`(참인 증언), `lie`(모순 증언, `theorem`=반박할 정리 카드), `reveal`(해결 후 대사). speaker는 `npcs.csv`의 id(얼굴 이미지가 표시됨)이거나 이름 그대로(이름 첫 글자로 표시됨)
- `misconceptions.csv` — 실수 유형 `id,name,tip`
- `explore.csv` — 정리 탐구: `theorem,diagram,drag,relation,caption`
  - drag: `점 움직임 [인자] [격자]`를 `|`로 나열. 움직임은 `free`, `hline`(가로로만), `vline`(세로로만), `circle O`(원 위), `arc O 시작° 끝°`(원의 일부 위). 끝의 숫자는 격자 간격(픽셀) 예) `B hline 40`
  - relation: `|`로 줄을 나누고 `{식}` 안에 `ang(A,O,B)`(∠AOB), `len(A,B)`(길이, 도형의 `unit` 기준)를 쓰면 실시간 계산. `{kind:식}`은 각의 종류(예각·직각·둔각·평각)
  - 탐구용 도형(`X01`~`X13`)에서 `angle`/`len`의 이름표를 `@`로 쓰면 잰 값을 표시하고, `unit 40 m`은 40픽셀 = 1m
- `quests.csv`, `badges.csv`(`finales`, `league_tier` 통계 추가), `league.csv`, `quotes.csv`

## 저장 (프로필)
- 진행 상황은 프로필마다 `data/save/profiles/<id>/`에 `profile`, `progress`, `history`, `mistakes`, `quests`, `stats`, `slips`(.csv)로 저장된다.
  지금 쓰는 프로필은 `data/save/active.txt`에 적힌다. 예전 버전의 `data/save/*.csv`는 첫 실행 때 `profiles/p1/`로 자동으로 옮겨진다.
- `stats.csv`(문제별 시도·정답·시간·힌트)와 `slips.csv`(실수 유형별 횟수)가 추리 분석의 바탕이다.
- 화면 설정(효과음·배경음악·테마·글자 크기)과 리그 단계도 `profile.csv`에 함께 저장된다.
- 탐정 수첩 → 백업에서 프로필을 zip 하나로 내보내고, 내보낸 zip을 새 프로필로 불러올 수 있다.

