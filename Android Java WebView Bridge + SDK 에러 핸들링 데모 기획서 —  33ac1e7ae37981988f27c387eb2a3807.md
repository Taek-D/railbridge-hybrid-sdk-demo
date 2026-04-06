# Android Java WebView Bridge + SDK 에러 핸들링 데모 기획서 — 2026-04-06

## 연관 공고

- **공고명**: 기 구축 앱 내 레일플러스 교통카드 SDK 연동 성능 개선 및 안정화
- **모집 마감일**: 2026-04-17
- **부족 역량**: Android Java 네이티브 코드 직접 작성/수정 경험, iOS Swift 네이티브 경험, 3rd party 폐쇄형 SDK 디버깅 경험

---

## 프로젝트 목표

Android Java 환경에서 WebView-네이티브 브릿지(JavascriptInterface) 구조를 직접 구현하고, 3rd party Mock SDK 에러 처리 패턴(Retry, 로깅)을 실증하는 포트폴리오 데모.

---

## 구현 범위

- [MockRailSdk.java](http://MockRailSdk.java) — 충전/잔액 조회 시뮬레이션 (에러율: 20%)
- [NativeBridge.java](http://NativeBridge.java) — @JavascriptInterface (requestCharge, getBalance, getSdkStatus, reportError)
- [ErrorLogger.java](http://ErrorLogger.java) — Firebase Crashlytics 래핑 + 로컬 로그 50건 저장
- [RetryHandler.java](http://RetryHandler.java) — 지수 백오프 (500ms → 1000ms → 2000ms, 최대 3회)
- [WebViewActivity.java](http://WebViewActivity.java) — WebView 초기화 및 브릿지 주입
- assets/webview/index.html — 테스트 UI (충전/조회 버튼, 결과 로그)

---

## 예상 일정

- Day 1-2: MockRailSdk + 프로젝트 셋업
- Day 3-4: NativeBridge + WebViewActivity 구현
- Day 5-6: ErrorLogger + RetryHandler 연결
- Day 7: 통합 테스트 + GitHub 배포 + README

---

## 기술 스택

Java (Android API 26+), WebView + JavascriptInterface, Firebase Crashlytics, Gradle 8.x

---

## 공고 연관성

완성 시 지원서에서 다음을 실증 가능:

- Android Java WebView 브릿지 구조 이해 및 직접 구현
- 3rd party SDK 간헐적 실패(ERR_NETWORK_TIMEOUT, ERR_SDK_INTERNAL) 처리 패턴
- Firebase Crashlytics 기반 에러 로그 수집 체계 구축
- 외부 SDK 우회 처리(Retry) 로직 — 공고 비기능 요구사항 직접 대응

---

## 완료 기준

- [ ]  MockRailSdk 에러 시뮬레이션 정상 동작
- [ ]  JS → Java 브릿지 호출 성공
- [ ]  Java → JS 결과 반환 성공 (evaluateJavascript)
- [ ]  Retry 로직 동작 확인 (ERR_NETWORK_TIMEOUT → 자동 재시도)
- [ ]  Firebase Crashlytics 에러 로그 수집 확인
- [ ]  APK 빌드 성공
- [ ]  GitHub 배포 완료
- [ ]  Notion 포트폴리오 DB 업로드 완료
- [ ]  위시켓 지원서 재작성 요청

---

## 개발 도구

Qwen Code CLI 사용. 프롬프트 파일: [android-webview-bridge-demo-prompt.md](http://android-webview-bridge-demo-prompt.md)