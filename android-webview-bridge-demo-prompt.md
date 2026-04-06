# Android Java WebView Bridge + SDK 에러 핸들링 데모 — Qwen Code CLI 프롬프트

## 프로젝트 목적

위시켓 공고 "기 구축 앱 내 레일플러스 교통카드 SDK 연동 성능 개선 및 안정화"에 지원하기 위한
포트폴리오 사이드 프로젝트입니다.

이 데모는 다음 세 가지를 실증합니다:
1. Android Java 네이티브 코드 작성 및 WebView 래핑 구조 이해
2. WebView ↔ Java 브릿지(JavascriptInterface) 구현 및 비동기 통신 처리
3. 3rd party Mock SDK 에러 처리 패턴 + Firebase Crashlytics 로그 수집 연동

---

## 기술 스택

- Language: Java (Android)
- Min SDK: API 26 (Android 8.0)
- Target SDK: API 34
- WebView: Android WebView + JavascriptInterface
- Mock SDK: 자체 구현 (레일플러스 SDK 행동 시뮬레이션)
- 로그: Firebase Crashlytics + 자체 로그 수집 레이어
- Build: Gradle 8.x, Android Studio 환경 기준

---

## 디렉토리 구조 (목표)

```
app/
  src/main/
    java/com/demo/railbridge/
      MainActivity.java
      WebViewActivity.java
      bridge/
        NativeBridge.java          # JavascriptInterface 핵심 클래스
        BridgeCallback.java        # 결과 콜백 인터페이스
      sdk/
        MockRailSdk.java           # 레일플러스 SDK 행동 시뮬레이션
        SdkErrorCode.java          # 에러 코드 enum
        ChargeResult.java          # 충전 결과 모델
      logging/
        ErrorLogger.java           # 에러 로그 수집 레이어 (Crashlytics 래핑)
        LogEvent.java              # 로그 이벤트 모델
      retry/
        RetryHandler.java          # Retry 로직 (지수 백오프)
    assets/
      webview/
        index.html                 # 테스트용 WebView 페이지
        bridge_test.js             # 브릿지 호출 JS
    res/
      layout/
        activity_main.xml
        activity_webview.xml
```

---

## 구현 요구사항 (기능 단위)

### 1. WebView 기본 세팅 (WebViewActivity.java)

```
- WebView 초기화: JavaScript 활성화, DOM Storage 활성화
- NativeBridge 인스턴스를 addJavascriptInterface로 주입
  - 인터페이스 이름: "RailBridge"
- WebViewClient 커스텀: shouldOverrideUrlLoading, onPageFinished, onReceivedError 구현
- 페이지 로드 완료 후 JS에 "bridgeReady" 이벤트 전달
- 하드웨어 가속 활성화 (레이아웃 또는 코드)
```

### 2. JavascriptInterface 브릿지 (NativeBridge.java)

아래 메서드를 @JavascriptInterface로 노출합니다.

```java
// 충전 요청
// JS: RailBridge.requestCharge(JSON.stringify({amount: 10000, cardId: "CARD_001"}))
void requestCharge(String paramsJson)

// 잔액 조회
// JS: RailBridge.getBalance(JSON.stringify({cardId: "CARD_001"}))
void getBalance(String paramsJson)

// SDK 상태 확인
// JS: RailBridge.getSdkStatus()
void getSdkStatus()

// 에러 리포트 수신 (JS → Native)
// JS: RailBridge.reportError(JSON.stringify({code: "ERR_001", message: "...", context: "..."}))
void reportError(String errorJson)
```

각 메서드는 아래 패턴을 따릅니다:
- 메인 스레드가 아닌 스레드에서 호출되므로, 결과 콜백은 `runOnUiThread` 또는 `Handler(Looper.getMainLooper())`를 통해 JS에 반환
- 결과 반환: `webView.evaluateJavascript("window.onBridgeResult('" + resultJson + "')", null)`
- 모든 메서드는 try-catch로 감싸고, 예외 발생 시 ErrorLogger에 기록 후 에러 결과 반환

### 3. Mock SDK (MockRailSdk.java)

레일플러스 SDK의 실제 동작을 시뮬레이션합니다.

```
charge(String cardId, int amount, SdkCallback callback):
  - 80% 확률: 성공 (200~500ms 랜덤 딜레이)
  - 10% 확률: SDK 내부 에러 (ERR_SDK_INTERNAL)
  - 5%  확률: 네트워크 타임아웃 (ERR_NETWORK_TIMEOUT)
  - 5%  확률: 카드 잔액 부족 (ERR_INSUFFICIENT_BALANCE)
  - 딜레이 구현: Thread.sleep 또는 Handler.postDelayed
  - 타임아웃은 5초로 설정, 초과 시 강제 ERR_TIMEOUT 반환

getBalance(String cardId, SdkCallback callback):
  - 90% 성공, 10% 에러 (ERR_SDK_INTERNAL 또는 ERR_NETWORK_TIMEOUT)

getSdkVersion(): String 반환 ("1.2.3-mock")
isInitialized(): boolean 반환
```

SdkErrorCode enum:
```java
ERR_SDK_INTERNAL("1001", "SDK 내부 오류"),
ERR_NETWORK_TIMEOUT("1002", "네트워크 타임아웃"),
ERR_INSUFFICIENT_BALANCE("1003", "잔액 부족"),
ERR_INVALID_CARD("1004", "유효하지 않은 카드"),
ERR_TIMEOUT("1005", "요청 타임아웃"),
ERR_UNKNOWN("9999", "알 수 없는 오류")
```

### 4. 에러 로그 수집 레이어 (ErrorLogger.java)

```
log(LogEvent event):
  - Crashlytics.log(event.toString()) 호출
  - 로컬 SQLite 또는 SharedPreferences에 최근 50건 저장
  - 콘솔에도 Log.e() 출력

logSdkFailure(String method, SdkErrorCode code, String context):
  - LogEvent 생성 후 log() 호출
  - Crashlytics.setCustomKey("last_error_code", code.getCode()) 설정
  - Crashlytics.setCustomKey("last_error_method", method) 설정

getRecentLogs(): List<LogEvent> 반환 (최근 50건)

clearLogs(): 로컬 로그 초기화
```

LogEvent 모델:
```java
String id          // UUID
String timestamp   // ISO-8601
String method      // 어느 메서드에서 발생
String errorCode
String errorMessage
String context     // 추가 컨텍스트 (JSON string)
boolean resolved   // Retry로 해결됐는지 여부
```

### 5. Retry 핸들러 (RetryHandler.java)

```
execute(RetryTask task, int maxRetry, RetryCallback callback):
  - maxRetry 횟수까지 재시도
  - 지수 백오프: 1차 500ms, 2차 1000ms, 3차 2000ms
  - 각 시도마다 ErrorLogger에 attempt 로그 기록
  - maxRetry 소진 시 RETRY_EXHAUSTED 에러로 callback 호출
  - 성공 시 resolved = true로 LogEvent 업데이트

현재 공고 요구사항 반영:
  - "외부 SDK 문제로 판명될 경우 우회 처리(Retry 등) 로직을 제공해야 함"
  - ERR_NETWORK_TIMEOUT, ERR_SDK_INTERNAL에만 Retry 적용
  - ERR_INSUFFICIENT_BALANCE, ERR_INVALID_CARD는 Retry 없이 즉시 에러 반환
```

### 6. 테스트용 WebView 페이지 (assets/webview/index.html)

```html
화면 구성:
- 카드 ID 입력 필드 (기본값: "CARD_001")
- 충전 금액 입력 필드 (기본값: 10000)
- [충전 요청] 버튼 → RailBridge.requestCharge() 호출
- [잔액 조회] 버튼 → RailBridge.getBalance() 호출
- [SDK 상태] 버튼 → RailBridge.getSdkStatus() 호출
- 결과 로그 영역: 타임스탬프 + 결과 JSON을 실시간 누적 표시
- [에러 강제 발생] 버튼 → RailBridge.reportError()로 테스트 에러 전송

window.onBridgeResult(resultJson) 함수:
  - 결과를 파싱하여 로그 영역에 추가
  - 성공/실패 여부에 따라 색상 구분 (초록/빨강)
  - 결과에 retryCount 포함 시 "[N차 재시도 후 성공/실패]" 표시
```

---

## 결과 JSON 형식

### 성공
```json
{
  "status": "success",
  "method": "requestCharge",
  "data": {
    "transactionId": "TXN_20260406_001",
    "amount": 10000,
    "balance": 45000,
    "timestamp": "2026-04-06T14:30:00Z"
  },
  "retryCount": 0
}
```

### 실패
```json
{
  "status": "error",
  "method": "requestCharge",
  "error": {
    "code": "1002",
    "message": "네트워크 타임아웃",
    "retryCount": 2,
    "resolved": false
  },
  "logId": "uuid-here"
}
```

---

## 구현 순서 (권장)

```
Day 1-2: 프로젝트 셋업 + MockRailSdk 구현
  - Android 프로젝트 생성 (Java, API 26)
  - Firebase 프로젝트 생성 및 Crashlytics 연동
  - MockRailSdk + SdkErrorCode 구현 및 단독 테스트

Day 3-4: 브릿지 핵심 구현
  - NativeBridge.java 구현
  - WebViewActivity.java 구현
  - assets/webview/index.html 기본 버전 구현

Day 5-6: 에러 처리 레이어 구현
  - ErrorLogger.java 구현 (Crashlytics 연동 포함)
  - RetryHandler.java 구현
  - NativeBridge에 ErrorLogger + RetryHandler 연결

Day 7: 통합 테스트 및 정리
  - 시나리오별 E2E 테스트 (충전 성공/실패/재시도)
  - README.md 작성 (구조 설명 + 스크린샷)
  - GitHub 커밋 및 APK 빌드
```

---

## Qwen Code CLI 시작 프롬프트

아래 프롬프트를 Qwen Code CLI에 입력하여 시작합니다.

---

```
당신은 Android Java 개발 전문가입니다. 아래 스펙에 따라 Android 데모 앱을 단계별로 구현해 주세요.

## 프로젝트 개요
위시켓 프리랜서 포트폴리오용 사이드 프로젝트입니다.
"레일플러스 교통카드 SDK WebView 브릿지 트러블슈팅 데모"를 구현합니다.
실제 레일플러스 SDK 대신 Mock SDK를 만들어 동일한 에러 패턴을 시뮬레이션합니다.

## 기술 스택
- Java (Android API 26+)
- WebView + JavascriptInterface 브릿지
- Firebase Crashlytics (로그 수집)
- Gradle 8.x

## 구현할 클래스 목록
1. MockRailSdk.java — 충전/잔액 조회 시뮬레이션, 에러 케이스 포함
2. SdkErrorCode.java — 에러 코드 enum
3. NativeBridge.java — @JavascriptInterface 메서드 (requestCharge, getBalance, getSdkStatus, reportError)
4. BridgeCallback.java — 결과 콜백 인터페이스
5. ErrorLogger.java — Crashlytics 래핑 + 로컬 로그 저장
6. LogEvent.java — 로그 이벤트 모델 (UUID, timestamp, method, errorCode 포함)
7. RetryHandler.java — 지수 백오프 Retry (최대 3회, ERR_NETWORK_TIMEOUT/ERR_SDK_INTERNAL만 재시도)
8. WebViewActivity.java — WebView 초기화, NativeBridge 주입, evaluateJavascript로 결과 반환
9. MainActivity.java — 진입점, WebViewActivity 실행
10. assets/webview/index.html — 테스트 UI (충전/조회 버튼, 결과 로그 영역, 색상 구분)

## 핵심 동작 요구사항

NativeBridge의 모든 메서드는:
- @JavascriptInterface 어노테이션 필수
- 백그라운드 스레드에서 MockRailSdk 호출
- 결과는 runOnUiThread + evaluateJavascript로 JS에 반환
  형식: webView.evaluateJavascript("window.onBridgeResult('" + resultJson + "')", null)
- 예외 발생 시 ErrorLogger에 기록 후 에러 JSON 반환

MockRailSdk의 charge() 메서드는:
- 80% 성공 (200~500ms 랜덤 딜레이)
- 10% ERR_SDK_INTERNAL
- 5% ERR_NETWORK_TIMEOUT
- 5% ERR_INSUFFICIENT_BALANCE
- 타임아웃 5초 초과 시 ERR_TIMEOUT

RetryHandler는:
- ERR_NETWORK_TIMEOUT, ERR_SDK_INTERNAL에만 재시도
- 지수 백오프: 500ms → 1000ms → 2000ms
- 최대 3회, 소진 시 RETRY_EXHAUSTED 반환

## 성공/에러 JSON 형식

성공:
{"status":"success","method":"requestCharge","data":{"transactionId":"TXN_xxx","amount":10000,"balance":45000},"retryCount":0}

실패:
{"status":"error","method":"requestCharge","error":{"code":"1002","message":"네트워크 타임아웃","retryCount":2,"resolved":false},"logId":"uuid"}

## 시작 방법
1. MockRailSdk.java와 SdkErrorCode.java부터 구현해 주세요.
2. 구현 후 단독으로 테스트할 수 있는 MockSdkTest.java도 만들어 주세요.
3. 각 클래스를 완성할 때마다 "완료: [클래스명]" 을 출력하고 다음으로 넘어가세요.
4. 전체 완성 후 README.md에 구조 설명과 실행 방법을 작성해 주세요.

준비되면 MockRailSdk.java 구현부터 시작해 주세요.
```

---

## 완료 기준 체크리스트

- [ ] MockRailSdk 에러 시뮬레이션 정상 동작 확인
- [ ] JS → Java 브릿지 호출 성공 (requestCharge, getBalance)
- [ ] Java → JS 결과 반환 성공 (evaluateJavascript)
- [ ] ERR_NETWORK_TIMEOUT 발생 시 자동 재시도 후 결과 반환 확인
- [ ] Firebase Crashlytics에 에러 로그 수집 확인
- [ ] WebView 테스트 UI에서 성공/실패 색상 구분 표시 확인
- [ ] APK 빌드 성공
- [ ] GitHub 배포 완료
- [ ] README.md 작성 완료 (스크린샷 포함)
- [ ] Notion 포트폴리오 DB 업로드 완료
- [ ] 위시켓 지원서 재작성 요청

---

## 지원서 활용 포인트 (완성 후)

이 프로젝트를 완성하면 지원서에 다음과 같이 활용할 수 있습니다:

"Android Java 환경에서 WebView-네이티브 브릿지(JavascriptInterface) 구조를 직접 구현하고,
3rd party Mock SDK의 간헐적 충전 실패(ERR_NETWORK_TIMEOUT, ERR_SDK_INTERNAL)를
지수 백오프 Retry 로직과 Firebase Crashlytics 에러 수집 체계로 처리한 경험이 있습니다.
레일플러스 SDK 연동 구간의 로깅 고도화 및 예외 처리 패턴을 동일하게 적용할 수 있습니다."
