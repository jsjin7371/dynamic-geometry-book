#!/bin/sh
# 기하미궁 (幾何迷宮) — 컴파일 후 실행 (JDK 11 이상)
#   ./run.sh            게임 실행
#   ./run.sh validate   data/*.csv 콘텐츠 검사 (오류가 있으면 종료 코드 1)
#   ./run.sh editor     도형 편집기 (diagrams.csv)
cd "$(dirname "$0")" || exit 1
rm -rf out/app && mkdir -p out/app
javac -encoding UTF-8 -d out/app $(find src -name '*.java') || exit 1
case "$1" in
  validate) exec java -cp out/app geomystery.tools.ContentValidator ;;
  editor)   exec java -cp out/app geomystery.tools.DiagramEditor ;;
  *)        exec java -cp out/app geomystery.App ;;
esac
