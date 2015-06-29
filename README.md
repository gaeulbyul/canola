# Canola

**Canola**는 Clojure연습용으로 작성한 블로그용 정적 사이트 생성기입니다.

## 사용법

leiningen을 사용할 경우.

    $ lein run -- [command] [arguments...]

jar로 컴파일해서 사용할 경우.

    $ java -jar canola.jar -- [command] [arguments...]

## 명령어

- `new <POSTNAME>` : `contents/` 디렉토리에 새 포스트를 생성합니다.
- `build` : `dist`디렉토리에 HTML로 생성합니다.

## 라이센스

MIT 라이센스로 배포합니다.
