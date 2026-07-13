# Bank Notification Balance Sync Support Matrix

검증 기준일: 2026-07-11

`SYNTHETIC`은 고정 fixture 단위 테스트 결과이며 실제 은행 알림 지원 증거가 아님
`UNVERIFIED`는 이 환경에서 실제 알림 원문을 확보하지 못했다는 의미임

| Provider | Package | Synthetic | Real sample | Debit | Credit | Transfer review | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| KB | `com.kbstar.kbbank` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | package verified |
| Shinhan | `com.shinhan.sbanking`, `com.shinhan.smartcaremgr` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | current and legacy package |
| Hana | `com.hanabank.oqf`, `com.kebhana.hanapush` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | current and legacy package |
| Woori | `com.wooribank.smart.npib` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | package verified |
| NH | `nh.smart.banking` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | package verified |
| IBK | `com.ibk.android.ionebank` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | package verified |
| KakaoBank | `com.kakaobank.channel` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | package verified |
| Toss aggregator | `viva.republica.toss` | SYNTHETIC | UNVERIFIED - no real sample available | SYNTHETIC | SYNTHETIC | SYNTHETIC | provider requires explicit bank name |

## Device Evidence

- Physical Galaxy: `UNVERIFIED - no authorized real device sample available` (기존 은행 parser 대상)
- Emulator build/install can verify app startup and instrumentation only
- No full account number or unmasked notification text is stored by the feature

## Notification Source Selection Verification (2026-07-13, Galaxy SM_S931N 실기기)

T7 검증 결과. 실제 은행 앱 알림 원문 대신 `adb shell cmd notification post`가 게시하는
`com.android.shell` 실알림(진짜 unknown package)으로 gate 전 경로를 검증함.

기기 상태:

- `enabled_notification_listeners`에 `com.choiyoonseo.automoney/...MoneyNotificationListenerService` 포함 확인
- `com.kbankwith.smartbank` 설치 확인 (정적 registry 밖 unknown package)
- debug APK 설치 후 설정 UI 실물 렌더 확인 (기본 지원 10개 앱 + toggle + packageName + 배지)

| 시나리오 | 방법 | 결과 |
| --- | --- | --- |
| unknown OFF에서 금융 문구 알림 | shell 알림 `10,000원 입금 되었습니다` | PASS — `observed_sources`에 `com.android.shell\|epoch\|count`만 기록, 본문·title 문자열 없음 |
| 차단 알림이 진단을 만들지 않음 | 위와 동일 | PASS — `notification_diagnostics.xml` 미생성 |
| 감지 목록 UI 노출 | 설정 재진입 | PASS — "추가 감지 앱"에 `셸 / com.android.shell / 감지됨 · 직접 허용 필요` (PackageManager label 조회 동작) |
| unknown ON 확인 dialog | toggle 탭 | PASS — 기기 내 분석·다음 알림부터 적용·검토함 확인 문구 표시, 취소/허용 |
| 허용 후 소액 결제 문구 | `스타벅스 6,100원 결제 완료` | PASS — Generic parser EXPENSE, `LOW_CONFIDENCE_CATEGORY`, 진단 preview `사용자 선택 앱 · 원문 미저장`, title 미저장 |
| 검토함 진입·자동확정 없음 | 검토 탭 | PASS — 1건, `APP shell` 배지, 확인 대기. merchant 원문 미노출 |
| 광고 차단어 | `지금 결제하면 10,000원 혜택` | PASS — IGNORED, 거래 미생성 |
| toggle OFF 즉시 반영 | 설정에서 OFF | PASS — enabled set 제거 + 최근 진단 즉시 clear |
| OFF 후 재차단 | `50,000원 입금` | PASS — 진단 비어 있음 유지, observed count만 증가 |
| toggle 상태 영속 | SharedPreferences 직접 확인 + instrumented tests | PASS |
| known trusted OFF→ON round-trip | 하나은행 `com.kebhana.hanapush` toggle | PASS — OFF 시 명시 차단 저장, ON 시 기본 상태 복원 (dialog 없음) |
| instrumented stores | `NotificationSourceStoresInstrumentedTest`, `NotificationDiagnosticsStoreInstrumentedTest` on SM-S931N | PASS (2 tests) |

남은 항목 (실사용 확인):

- 케이뱅크 등 실제 은행 앱의 실알림 원문 검증 — 사용자가 해당 앱 toggle ON 후 실제 거래 알림 도착 시 검토함 동작 확인
- 기존 trusted 은행(KB/Toss 등) 실알림 회귀 — parser 단위 테스트 전체 PASS로 코드 회귀는 없음, 실알림은 일상 사용 중 확인
- 테스트 잔여물: 상태바의 shell 테스트 알림 5건은 알림 패널에서 수동 삭제 필요. 감지 목록의 `com.android.shell` 항목은 metadata-only로 무해 (LRU로 자연 제거)
