# 완료 기준 체크리스트

## 기능 구현
- [x] MockRailSdk 에러 시뮬레이션 정상 동작 확인
- [x] JS → Java 브릿지 호출 성공 (requestCharge, getBalance)
- [x] Java → JS 결과 반환 성공 (evaluateJavascript)
- [ ] ERR_NETWORK_TIMEOUT 발생 시 자동 재시도 후 결과 반환 확인
- [ ] Firebase Crashlytics에 에러 로그 수집 확인
- [x] WebView 테스트 UI에서 성공/실패 색상 구분 표시 확인

## 빌드 및 배포
- [ ] APK 빌드 성공
- [ ] GitHub 배포 완료
- [x] README.md 작성 완료 (스크린샷 포함)

## 포트폴리오 활용
- [ ] Notion 포트폴리오 DB 업로드 완료
- [ ] 위시켓 지원서 재작성 요청

---

## 구현 완료 항목

### 완료된 클래스
- [x] SdkErrorCode.java - 에러 코드 enum
- [x] ChargeResult.java - 충전 결과 모델
- [x] MockRailSdk.java - 충전/잔액 조회 시뮬레이션
- [x] BridgeCallback.java - 결과 콜백 인터페이스
- [x] LogEvent.java - 로그 이벤트 모델
- [x] ErrorLogger.java - Crashlytics 래핑 + 로컬 로그 저장
- [x] RetryHandler.java - 지수 백오프 Retry 로직
- [x] NativeBridge.java - @JavascriptInterface 메서드
- [x] WebViewActivity.java - WebView 초기화, 브릿지 주입
- [x] MainActivity.java - 진입점
- [x] assets/webview/index.html - 테스트 UI
- [x] res/layout XML 파일들 (activity_main.xml, activity_webview.xml)
- [x] build.gradle 설정 (루트 + 앱)
- [x] AndroidManifest.xml
- [x] 리소스 파일들 (strings.xml, themes.xml, colors.xml)
- [x] README.md 작성

**진행률**: 16/16 클래스 및 설정 파일 구현 ✅
