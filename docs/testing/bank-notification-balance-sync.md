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

- Physical Galaxy: `UNVERIFIED - no authorized real device sample available`
- Emulator build/install can verify app startup and instrumentation only
- No full account number or unmasked notification text is stored by the feature
