# 레슨별 학습 화면 개발 가이드

## 1. 현재 구조와 작업 범위

새 도형을 추가할 때 학습 화면 전체를 복사하지 않습니다. 공통 화면인 `LessonPanel`을 사용하고, 미리 작성된 도형별 콘텐츠를 사용하고 애니메이션을 구현합니다.

애니메이션 클래스만 추가하면 자동으로 연결되는 구조가 아닙니다. **기존 콘텐츠 확인 → 애니메이션 구현 → 화면 연결 → 선행 학습 검증 → 테스트**까지 진행해야 합니다.

| 구성 요소                                      | 역할 | 새 레슨 추가 시 작업 |
|--------------------------------------------|---|---|
| `domain/Level.java`                  | 도형 이름, 설명, 고정 문제·보기·정답·해설 | 기존 콘텐츠 사용 |
| `gui/lesson/animation/LessonAnimation.java` | 재생 시간, 일시정지·재개·다시 보기, 타이머 정리 | 기존 기능 재사용 |
| `gui/lesson/animation/(도형)Animation.java`  | 장면 그리기와 시간에 따른 자막 | 클래스 추가 |
| `gui/lesson/LessonPanel.java`              | 설명·문제·결과·복습 공통 UI | 애니메이션 생성 분기 연결 |
| `domain/StudyProgress.java`                | 메모리의 레벨별 완료, 답안, 학습일 | 공통 순서 검사 사용 |
| `gui/level/LevelMapPanel.java`             | 레벨 선택·잠금·상세 정보 | 순서와 연결 확인 |
| `app/AppNavigation.java`                   | 선택한 레슨을 학습 화면에 전달 | 일반적인 레슨 추가에는 수정 불필요 |

위 경로의 기준은 `src/main/java/geometrybook/`입니다.

기능은 [기능명세서](기능명세서.md)와 [개발 계획](PLAN.md)을 기준으로 합니다. UI는 [디자인 참고 이미지](design/reference/)를 활용하되 화면 구성은 상황에 맞게 조정할 수 있습니다. 난이도와 보상은 임의로 배정하지 않습니다. 현재 데이터는 메모리에만 유지하며 파일 입출력과 포인트 지급은 별도 작업입니다.

## 2. 미리 작성된 학습 콘텐츠 사용

`Level`에는 일곱 도형의 설명과 문제·보기·정답·해설이 이미 작성되어 있습니다. 담당 도형의 enum을 그대로 사용하고 새 항목이나 문제를 중복 추가하지 마세요. 예를 들어 직사각형은 `Level.RECTANGLE`을 사용합니다. 애니메이션은 기존 설명과 문제에서 다루는 내용을 포함해야 합니다.

- `title`: 도형 이름
- `nextTitle`: 다음 도형 이름
- `summary`: 설명 화면과 완료 화면에서 공유할 핵심 설명
- `prompts`: 확인 문제 3개
- `choices`: 각 문제의 보기 3개
- `answers`: 문제별 정답 보기 인덱스. 첫 번째 보기는 `0`입니다.
- `explanations`: 문제별 해설 3개

문제·보기·정답·해설은 같은 인덱스끼리 대응해야 합니다. **현재 구현은 세 문제, 문제당 세 보기를 전제로 합니다.** 보기 개수를 변경하려면 콘텐츠뿐 아니라 `LessonPanel`과 `StudyProgress.Session`도 함께 수정해야 합니다.

`Level.number()`와 `StudyProgress.start(topic)`은 enum 선언 순서를 사용합니다. 아래 순서를 변경하지 마세요.

> 삼각형 → 정사각형 → 직사각형 → 마름모 → 사다리꼴 → 평행사변형 → 원

마지막 레슨인 원은 `nextTitle`이 빈 문자열이며, 결과 화면은 마지막 단계 완료 안내를 표시합니다.

## 3. 애니메이션 구현

`gui/lesson/animation/`에 클래스를 만들고 `LessonAnimation`을 상속합니다. 새 타이머나 재생 버튼을 만들 필요는 없습니다.

```java
package geometrybook.gui.lesson.animation;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class RectangleAnimation extends LessonAnimation {
    public RectangleAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override
    public String caption() {
        if (elapsed < 8000) {
            return "네 점을 이어 직사각형을 만들어요.";
        }
        return "직사각형은 네 각이 모두 직각이에요.";
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            // elapsed에 따라 도형의 선분, 각 표시 등을 그립니다.
            // 실제 구현은 SquareAnimation의 비율 유지와 장면 전환을 참고하세요.
        } finally {
            g.dispose();
        }
    }
}
```

위 코드는 클래스 구조 예시이며 완성된 애니메이션은 아닙니다.

- `elapsed`는 경과 시간이며 단위는 밀리초입니다. 현재 공통 재생 시간은 `DURATION = 24000`, 즉 24초입니다.
- 장면별 시작·종료 시점을 정하고 그 시간에 맞춰 도형과 `caption()`을 함께 바꿉니다.
- `paintComponent()`에서는 현재 시간에 해당하는 장면만 그립니다. 여기서 시간을 증가시키거나 타이머를 시작하지 않습니다.
- 기존 애니메이션처럼 논리 좌표와 배율을 사용해 창 크기가 달라도 도형 비율을 유지합니다.
- `Graphics2D`는 복사해서 사용한 뒤 정리합니다. UI 갱신은 Swing EDT에서 수행하며 `Thread.sleep()`으로 재생하지 않습니다.
- 부모 클래스의 `play()`, `pause()`, `restart()`, `playing()`, `elapsed()`를 그대로 사용합니다. 화면 제거 시 타이머 정리도 부모 클래스가 담당합니다.
- `DURATION`을 바꾸면 기존 레슨의 재생 길이도 바뀝니다. 도형별 재생 시간이 필요하면 공통 클래스와 진행률 표시를 함께 설계해야 합니다.

## 4. 공통 화면과 진행 상태에 연결

### 애니메이션 선택

`LessonPanel.showExplanation()`은 `switch`로 도형별 애니메이션을 생성합니다. 담당 도형의 기존 enum에 해당하는 `case`를 추가해야 합니다. 애니메이션이 연결되지 않은 레슨은 준비 중 안내 후 지도로 돌아갑니다.

직사각형 애니메이션을 구현한 뒤에는 다음처럼 각 도형을 명시적으로 연결할 수 있습니다. 해당 클래스 import도 추가해야 합니다.

```java
switch (topic) {
    case TRIANGLE:
        animation = new TriangleAnimation(updatePlayback);
        break;
    case SQUARE:
        animation = new SquareAnimation(updatePlayback);
        break;
    case RECTANGLE:
        animation = new RectangleAnimation(updatePlayback);
        break;
    default:
        JOptionPane.showMessageDialog(this, "이 레슨의 애니메이션은 준비 중이에요.");
        onLevel.run();
        return;
}
```

문제 표시·정답 확인·완료·복습 기능은 기존 `LessonPanel`과 `StudyProgress.Session`을 재사용합니다. 새 레슨을 추가할 때는 `LessonPanel`의 애니메이션 생성 분기에 해당 도형을 직접 연결해야 합니다.

### 선행 학습과 진도

`StudyProgress.start(Level)`은 첫 레슨을 제외하고 enum 순서상 바로 앞 레슨의 완료 여부를 공통으로 검사합니다. 도형별 검사 코드를 추가하지 마세요. 직사각형은 정사각형, 평행사변형은 사다리꼴 완료 후 시작할 수 있습니다.

화면의 버튼 잠금만으로는 충분하지 않습니다. 서비스를 직접 호출해도 순서를 건너뛸 수 없도록 도메인 검사와 지도 표시를 함께 맞춥니다. `StudyProgress.isCompleted()`와 `start()`의 인자 없는 버전은 삼각형용이므로, 다른 도형에는 `isCompleted(topic)`과 `start(topic)`을 사용합니다.

현재 지도는 완료 개수로 다음 레벨을 결정하므로 순차 완료를 전제로 합니다. 마지막으로 나의 탐험의 다음 학습 문구와 지도 상세 설명도 새 레슨에 맞는지 확인하세요.

## 5. 검증과 협업 체크리스트

- [ ] 레슨 이름·번호·자막·문제가 선택한 도형과 일치한다.
- [ ] 선행 레슨 미완료 시 화면과 `StudyProgress.start(topic)`에서 시작을 막는다.
- [ ] 애니메이션의 도형 비율과 수학 설명이 정확하다.
- [ ] 일시정지 후 같은 위치에서 재개하고, 다시 보기로 처음부터 재생한다.
- [ ] 화면 이탈·로그아웃·창 종료 시 타이머와 미완료 상태가 정리된다.
- [ ] 최초 학습은 설명 확인 후 문제로 이동하며, 복습은 설명 생략이 가능하다.
- [ ] 미선택 제출을 막고, 맞힌 문제를 유지하며 오답만 다시 풀 수 있다.
- [ ] 세 문제를 모두 맞혀야 완료되고, 새 복습에서는 답안이 초기화된다.
- [ ] 완료 후 진도와 달력이 반영되며 다른 레슨의 완료 상태는 유지된다.
- [ ] 삼각형·정사각형 등 기존 레슨의 학습과 복습이 정상 동작한다.
- [ ] 공통 코드 변경은 팀원에게 공유하고, 화면 캡처와 검증 결과를 PR에 남긴다.

프로젝트 루트에서 실행합니다. Java 11 이상이 필요합니다.

```sh
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
java -cp out geometrybook.app.Main
```

학습 상태 테스트에도 새 레슨의 선행 조건, 최초 완료, 복습과 기존 레슨 영향 여부를 추가합니다.

```sh
javac -encoding UTF-8 -cp out -d out src/test/java/geometrybook/domain/StudyProgressTest.java
java -cp out geometrybook.domain.StudyProgressTest
```
