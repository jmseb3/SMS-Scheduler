# SMSScheduler 작업 가이드

## 작업 원칙

- 코드를 바꾸기 전에 가정과 영향 범위를 확인한다. 요구사항이 불명확하면 임의로 구조를 확장하지 않는다.
- 요청된 동작에 필요한 최소 범위만 수정한다. 관련 없는 정리, 이름 변경, 의존성 추가를 함께 하지 않는다.
- 현재 프로젝트의 단순한 구조를 유지한다. 별도 모듈, Repository 인터페이스, UseCase, ViewModel 같은 계층은 실제 필요가 생기거나 명시적으로 요청될 때만 도입한다.
- 파일 하나가 여러 책임을 갖기 시작하면 역할별로 나누되, 작은 기능을 위해 추상화를 먼저 만들지는 않는다.
- 기존 사용자 변경 사항과 릴리스 산출물을 보존한다.

## 프로젝트 개요

SMSScheduler는 연락처와 메시지 템플릿을 저장하고, 지정한 시각에 SMS를 발송하는 단일 모듈 Android 앱이다.

- 패키지: `com.wonddak.sms`
- 모듈: `:app`
- UI: Jetpack Compose + Material 3
- DI: Hilt
- 영속 저장소: Room, SharedPreferences
- 예약 실행: `AlarmManager` + `BroadcastReceiver`
- 빌드: Gradle Kotlin DSL, Version Catalog, KSP
- Java 호환 버전: 17
- 최소 SDK: 31

의존성 버전은 `gradle/libs.versions.toml`에서 관리한다. 저장소는 `settings.gradle.kts`에 선언하며, 개별 모듈에 저장소를 추가하지 않는다.

## 디렉터리와 책임

```text
app/src/main/java/com/wonddak/sms/
├── MainActivity.kt                  # Hilt 진입점, 테마/설정 상태, 알림 딥 링크 처리
├── SmsSchedulerApplication.kt       # @HiltAndroidApp Application
├── data/
│   ├── AppDatabase.kt               # Room 데이터베이스 정의
│   ├── AppDao.kt                    # 동기식 Room 쿼리
│   ├── AppEntities.kt               # Room 엔티티
│   ├── AppStore.kt                  # 모델 변환, 목록 저장, 상태 갱신, 구버전 데이터 이관
│   └── AppSettings.kt               # 앱 설정 모델과 SharedPreferences 저장소
├── di/AppModule.kt                  # Singleton Room DB 제공
├── model/
│   ├── Models.kt                    # UI/업무 모델과 MessageStatus
│   └── TemplateEngine.kt            # {{변수}} 추출과 치환
├── scheduling/
│   ├── MessageAlarmScheduler.kt     # 발송/사전 알림 AlarmManager 등록 및 취소
│   ├── SmsAlarmReceiver.kt          # 실제 SMS 발송과 결과 기록
│   ├── ReminderAlarmReceiver.kt     # 발송 전 알림 표시
│   ├── CancelScheduledMessageReceiver.kt
│   ├── BootReceiver.kt              # 재부팅 후 미래 예약 복원
│   ├── NotificationHelper.kt        # 알림 생성, 취소, 예약 내역 딥 링크
│   └── ReminderScheduleSync.kt       # 설정 변경 시 알림 예약 동기화
└── ui/
    ├── AppState.kt                  # Compose 관찰 목록과 저장 동작
    ├── SmsSchedulerApp.kt           # 최상위 Scaffold와 화면 전환
    ├── *Screen.kt                   # 화면 및 화면 전용 순수 헬퍼
    └── theme/                       # 색상, 타이포그래피, 테마
```

테스트는 `app/src/test`의 JVM 단위 테스트와 `app/src/androidTest`의 기기/에뮬레이터 테스트로 나뉜다.

## 아키텍처와 의존성 방향

현재 구조는 다음 흐름을 따르는 가벼운 계층 구조다.

```text
Compose Screen
    ↓
AppState
    ↓
AppStore               SettingsStore
    ↓                       ↓
Room Database          SharedPreferences

Compose / Receiver
    ↓
MessageAlarmScheduler → AlarmManager → BroadcastReceiver
```

- UI는 `AppState`의 `mutableStateListOf` 목록을 관찰한다.
- `AppState`는 UI 입력을 정규화하고 `AppStore`에 즉시 저장한다.
- `AppStore`가 앱 모델과 Room 엔티티 사이의 변환 경계다. UI에서 DAO나 엔티티를 직접 사용하지 않는다.
- 설정은 업무 데이터와 분리해 `SettingsStore`에 저장한다.
- Android 시스템이 생성하는 Activity와 Receiver에는 `@AndroidEntryPoint`를 사용한다.
- 순수 계산은 Android 의존성이 없는 함수로 분리해 JVM 테스트가 가능하게 유지한다.

현재는 Navigation 라이브러리나 ViewModel을 사용하지 않는다. 최상위 화면 전환은 `SmsSchedulerApp`의 Compose 상태로 처리하고, 프로세스 외 실행은 Receiver가 담당한다.

## DI 규칙

- `SmsSchedulerApplication`의 `@HiltAndroidApp`을 유지한다.
- `MainActivity`와 의존성을 주입받는 Receiver에는 `@AndroidEntryPoint`가 필요하다.
- 생성자 주입이 가능한 클래스는 `@Inject constructor`를 우선한다.
- 애플리케이션 수명 객체는 `@Singleton`으로 유지한다.
- Context는 Activity Context를 보관하지 말고 `@ApplicationContext`를 주입받는다.
- 직접 생성할 수 없는 Room DB 같은 객체만 `di/AppModule.kt`에서 `@Provides` 한다.
- `AppDatabase`, `AppStore`, `SettingsStore`, `MessageAlarmScheduler`의 생명주기는 모두 애플리케이션 범위다.

## 데이터와 상태 관리

### Room

- 데이터베이스 파일명은 `sms_scheduler.db`, 현재 스키마 버전은 1이다.
- 연락처, 연락처별 템플릿 값, 템플릿, 예약 메시지, 발송 이력을 저장한다.
- `AppStore`의 목록 저장은 현재 전체 삭제 후 전체 삽입 방식이다. 일부만 수정하는 DAO를 추가하지 않는 한 이 의미를 보존한다.
- 연락처별 템플릿 값은 연락처 삭제 시 cascade 삭제된다.
- 예약 상태가 `PENDING`에서 `SENT` 또는 `FAILED`로 바뀌면 같은 트랜잭션에서 발송 이력을 기록한다.
- 저장된 알 수 없는 상태 문자열은 안전하게 `PENDING`으로 복구한다.
- 스키마를 변경할 때는 DB 버전과 Migration 전략을 함께 추가한다. 개발 편의를 위한 destructive migration으로 사용자 데이터를 지우지 않는다.

`AppModule`은 현재 `allowMainThreadQueries()`를 사용하며 `AppStore` API도 동기식이다. 이 제약 아래에서는 UI 데이터 흐름을 임의로 coroutine/Flow 기반으로 절반만 전환하지 않는다. 비동기화가 필요하면 DAO, Store, AppState와 호출부를 하나의 변경 단위로 설계한다.

### 구버전 데이터 이관

`AppStore` 생성 시 기존 `sms_scheduler` SharedPreferences의 JSON 데이터를 Room으로 한 번 이관한다. `room_migrated_v1` 플래그, 빈 테이블일 때만 복원하는 조건, 완료된 예약의 발송 이력 생성 규칙을 유지한다.

### UI 상태

- `AppState.reload()`는 Room을 다시 읽으며, 앱이 `ON_RESUME`될 때 호출된다. Receiver가 백그라운드에서 변경한 상태가 이 경로로 UI에 반영된다.
- ID는 현재 시간과 전체 모델의 최대 ID를 비교해 단조 증가하도록 생성한다.
- 예약 목록은 발송 시각 내림차순을 유지한다.
- 연락처와 템플릿 입력은 저장 전에 공백을 정리한다.

## 예약 발송의 핵심 규칙

예약 관련 변경은 저장 데이터, AlarmManager 등록, 알림, 재부팅 복원까지 함께 검토한다.

1. UI가 `ScheduledMessage(PENDING)`를 먼저 저장한다.
2. `MessageAlarmScheduler.schedule()`이 발송 알람을 등록한다.
3. 설정이 켜져 있으면 발송 1시간 전 사전 알림도 등록한다.
4. `SmsAlarmReceiver`는 ID로 저장된 메시지를 다시 읽고, 여전히 `PENDING`인 경우에만 발송한다.
5. 발송 시도 후 상태를 `SENT` 또는 `FAILED`로 갱신하고 발송 이력을 남긴다.
6. 취소 시 발송 알람, 사전 알림 알람, 표시 중인 알림을 모두 정리하고 대기 예약을 저장소에서 제거한다.
7. 재부팅 후에는 미래 시각의 `PENDING` 예약만 다시 등록한다.

추가 불변조건:

- SMS 권한이 없으면 발송하지 않고 `FAILED`로 기록한다.
- 정확한 알람 권한이 없으면 `setAndAllowWhileIdle`로 대체한다. `schedule()`의 `false`는 예약 실패가 아니라 비정확 알람 사용을 뜻한다.
- 사전 알림 시각이 이미 지났다면 현재 시각에서 약 1초 뒤로 보정한다.
- 발송 알람과 사전 알림은 같은 메시지 ID를 쓰되 서로 다른 request-code offset으로 구분한다.
- Receiver는 중복 실행에 대비해 항상 저장된 상태를 다시 확인해야 한다.
- PendingIntent의 extra 키와 request-code 계산을 바꾸면 등록, 취소, 알림 딥 링크를 모두 함께 갱신한다.

## 알림과 화면 이동

- 알림 채널 ID는 `scheduled_sms`다.
- Android 13 이상에서는 `POST_NOTIFICATIONS` 권한을 확인한다.
- 사전 알림을 누르면 `MainActivity`의 기존 인스턴스를 재사용하고 예약 내역 탭으로 이동한다.
- 딥 링크 상태는 `EXTRA_OPEN_HISTORY`, `EXTRA_MESSAGE_ID`, `openHistoryRequest`의 조합으로 갱신한다.
- Android 36 이상 사전 알림은 플랫폼 `Notification.ProgressStyle`, 그 이전은 `NotificationCompat`를 사용한다.
- 사전 알림 설정 변경 시 미래의 `PENDING` 예약만 등록하거나 취소한다. 이 필터는 `syncReminderSchedules`의 단위 테스트로 보호한다.

## UI 규칙

- 최상위 탭은 예약, 예약 내역, 템플릿, 연락처 순서다.
- 상세 화면이나 설정 화면에서는 하단 내비게이션을 숨긴다.
- Edge-to-edge와 window inset 처리를 유지한다.
- 테마는 시스템/밝게/어둡게를 지원하며 동적 색상은 사용하지 않는다.
- 공통 컴포넌트는 `CommonUi.kt`, 설정 전용 컴포넌트는 `SettingsComponents.kt`에 둔다.
- 화면에서 재사용 가치가 없는 작은 로직은 해당 화면 파일 가까이에 둔다. 테스트가 필요한 계산 로직은 `internal` 순수 함수로 추출한다.
- 사용자에게 표시되는 문구는 현재 한국어 어조와 용어를 따른다.

템플릿 변수 문법은 `{{변수명}}`이다. 변수명은 앞뒤 공백을 제거하고 중복을 제거한다. 값이 없는 변수는 원문 placeholder를 유지하며, `{{이름}}`은 선택한 연락처 이름으로 자동 채운다.

## 권한과 플랫폼 구성

Manifest의 주요 권한은 다음과 같다.

- `SEND_SMS`: 실제 SMS 발송
- `POST_NOTIFICATIONS`: 사전/완료 알림
- `RECEIVE_BOOT_COMPLETED`: 재부팅 후 예약 복원
- `SCHEDULE_EXACT_ALARM`: 정확한 예약 시각
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: 시스템 준비 상태 안내

Receiver의 exported 설정을 느슨하게 바꾸지 않는다. 앱 내부 알람 Receiver는 `exported=false`, 시스템 부팅 이벤트를 받는 `BootReceiver`만 intent filter와 함께 `exported=true`다.

## 테스트와 검증

변경 범위에 맞는 가장 작은 검증부터 실행하고, 완료 전에는 전체 단위 테스트와 디버그 빌드를 확인한다.

```bash
# JVM 단위 테스트
./gradlew :app:testDebugUnitTest

# 디버그 APK 빌드
./gradlew :app:assembleDebug

# 연결된 기기/에뮬레이터가 있을 때 계측 테스트
./gradlew :app:connectedDebugAndroidTest
```

테스트 배치 원칙:

- Android API가 필요 없는 모델, 치환, 필터, 시간 계산: `app/src/test`
- Compose UI 조작, Context/SharedPreferences, 실제 Android API: `app/src/androidTest`
- 예약 로직을 수정하면 미래/과거, `PENDING`/완료 상태, 설정 on/off를 함께 검증한다.
- 시간 의존 로직은 테스트에서 `nowMillis`처럼 기준 시간을 주입해 결정적으로 만든다.

## 변경 시 체크리스트

- 데이터 모델 변경: 모델, Entity, 변환 함수, DB 버전/Migration, 테스트를 함께 확인했는가?
- 예약 변경: 등록과 취소가 대칭이며 BootReceiver 복원 경로도 일치하는가?
- 알림 변경: 권한 없음, 설정 꺼짐, Android 버전 분기를 확인했는가?
- Receiver 변경: Hilt 진입점, Manifest 선언, 중복 실행 방어를 유지했는가?
- UI 변경: 앱 재개 시 reload와 알림 딥 링크 경로가 깨지지 않는가?
- 설정 변경: 기본값, 저장/복원, 기존 예약 동기화가 함께 동작하는가?
- 순수 로직 변경: JVM 단위 테스트를 추가하거나 갱신했는가?

## 저장소에서 주의할 파일

- `keystore/`와 `keystore/signing.gradle`은 릴리스 서명 자료다. 명시적인 요청 없이 열람, 수정, 교체, 출력하지 않는다.
- `app/release/`의 APK/AAB와 메타데이터는 생성된 릴리스 산출물이다. 기능 작업 중 수정하거나 삭제하지 않는다.
- Gradle wrapper와 버전 카탈로그는 빌드 도구 변경 요청이 있을 때만 수정한다.
