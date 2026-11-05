# 심화 퀴즈 CSV 작성 가이드

심화 퀴즈 화면(`QuizPanel`)은 `data/questions.csv`(문제)와 `data/answer_choices.csv`(보기)를 읽어서 문제를 보여 줍니다. 코드를 고치지 않고 **두 CSV에 줄을 추가**하면 새 문제가 나옵니다.

읽는 코드는 `storage/CsvQuizSource`이고, 팀의 `QuizSource` 인터페이스를 구현합니다.

## 1. questions.csv (문제)

```
id,levelId,type,prompt,reviewSceneNo
triangle-quiz-1,TRIANGLE,MULTIPLE_CHOICE,삼각형의 두 각이 50도와 60도라면 나머지 각은 몇 도일까요?,1
triangle-quiz-2,TRIANGLE,OX,한 삼각형 안에 직각(90도)이 두 개 있을 수 있다.,1
```

| 열 | 쓰는 법 |
|---|---|
| id | 문제 고유 이름. 겹치면 안 됨. 규칙: `도형-quiz-번호` (예: `square-quiz-1`) |
| levelId | 어느 단원 문제인지. 아래 표의 레벨 ID 중 하나 (오타가 있으면 오류) |
| type | `OX` 또는 `MULTIPLE_CHOICE`(객관식) |
| prompt | 문제 문장 |
| reviewSceneNo | 틀렸을 때 다시 볼 설명 장면 번호. 지금은 설명이 한 화면이라 `1` |

**레벨 ID** (퀴즈 화면 순서)

| 삼각형 | 정사각형 | 직사각형 | 마름모 | 사다리꼴 | 평행사변형 | 원 |
|---|---|---|---|---|---|---|
| TRIANGLE | SQUARE | RECTANGLE | RHOMBUS | TRAPEZOID | PARALLELOGRAM | CIRCLE |

문제는 **파일에 적힌 줄 순서대로** 나옵니다.

## 2. answer_choices.csv (보기)

```
id,questionId,choiceNo,content,isCorrect
triangle-quiz-1-1,triangle-quiz-1,1,60도,false
triangle-quiz-1-2,triangle-quiz-1,2,70도,true
triangle-quiz-1-3,triangle-quiz-1,3,80도,false
triangle-quiz-2-1,triangle-quiz-2,1,O,false
triangle-quiz-2-2,triangle-quiz-2,2,X,true
```

| 열 | 쓰는 법 |
|---|---|
| id | 보기 고유 이름. 규칙: `문제id-보기번호` (예: `square-quiz-1-2`) |
| questionId | 어느 문제의 보기인지. questions.csv의 id와 똑같이 |
| choiceNo | 보기 번호 1, 2, 3… 화면에 이 순서로 나옴 |
| content | 보기 내용 |
| isCorrect | 정답이면 `true`, 아니면 `false` |

## 3. 꼭 지킬 규칙

- 문제마다 보기 **2개 이상**, 정답(`true`)은 **정확히 1개**
- **OX 문제**는 보기 2개: 1번 `O`, 2번 `X`. 정답인 쪽만 `true`
- 문장에 **쉼표(,)** 가 있으면 칸 전체를 큰따옴표로 감싸기
  - 예: `"한 각이 100도이고 나머지 두 각이 같다면, 같은 두 각은 각각 몇 도일까요?"`
- 문장 안에 큰따옴표를 쓰려면 `""`처럼 두 번 쓰기
- **엑셀로 열어 저장하지 말 것.** VS Code나 메모장에서 UTF-8로 저장 (엑셀은 한글 인코딩을 바꿔 버림)
- 빈 줄은 무시되지만, 칸을 비우면 오류

## 4. 새 단원 문제 추가 순서 (예: 정사각형)

1. questions.csv 맨 아래에 `square-quiz-1,SQUARE,OX,…,1` 같은 줄 추가
2. answer_choices.csv 맨 아래에 그 문제의 보기 줄 추가
3. 앱 실행 → 퀴즈 → 정사각형 선택해서 확인
4. 커밋 메시지 예: `chore: 정사각형 심화 퀴즈 CSV 추가`

## 5. 오류가 나면

규칙에 맞지 않으면 퀴즈 화면에 "문제를 불러오지 못했어요"가 뜹니다. 파일을 고친 뒤 **다시 시도**를 누르면 바로 다시 읽습니다.

지금 화면에는 자세한 원인이 표시되지 않으므로, 아래 표를 보고 확인하거나 데이터 담당에게 파일을 보내 주세요. 저장 모듈이 내는 오류 메시지는 이런 식입니다.

| 오류 메시지 예 | 원인 |
|---|---|
| `questions.csv 4행: type은 OX 또는 MULTIPLE_CHOICE여야 합니다` | type 오타 |
| `questions.csv 2행: levelId는 TRIANGLE, SQUARE, … 중 하나여야 합니다` | levelId 오타 (예: TRIANGEL) |
| `answer_choices.csv: OX 문제 … 의 보기는 1번 O, 2번 X여야 합니다` | OX 보기 순서가 바뀜 |
| `answer_choices.csv: 보기 … 의 questionId … 가 questions.csv에 없습니다` | 보기의 questionId 오타 |
| `answer_choices.csv: 문제 … 의 보기 구성이 올바르지 않습니다` | 정답이 0개·2개, 보기 1개, OX인데 O·X가 아님 |
| `questions.csv 3행: 열 개수가 6개입니다` | 문장에 쉼표가 있는데 큰따옴표를 안 씀 |

궁금한 점은 데이터 담당(정연)에게 물어봐 주세요.
