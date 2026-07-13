# Toss-style UI Restyle — Foundation + Home Implementation Plan

> Historical document: branch references below describe the original execution only. Current work must follow `AGENTS.md` and use `main` exclusively.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a light+dark design-token layer (colors, Pretendard typography, shapes, spacing) and apply it to the Home screen + bottom nav, matching the approved Toss-like look.

**Architecture:** Introduce `ui/theme/` token files consumed via `MaterialTheme` + a `MoneyColors` CompositionLocal. Refactor the Home-used shared components to read tokens instead of hardcoded literals, keeping their public signatures unchanged so screens still compile. Validate Home in both themes via `@Preview`. Live system-dark switch is deferred until every screen is converted.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.06.00), Material3, Compose `Font`/`FontFamily`, Pretendard OTF bundled in `res/font/`.

## Global Constraints

- Branch: `claude/ui-polish`. Edit only `ui/**` and `res/font/**`. Never touch `data/ domain/ notification/ ui/model/**` (Codex-owned).
- Never `git add .` / `git add -A`. Stage only the exact files listed per task; run `git status` first and confirm no Codex files are staged.
- `ui/AppRoot.kt` is a shared boundary zone — add a claim line to `docs/AI_COLLABORATION.md` "Shared File Claims" **before** editing it (Task 6).
- Do NOT edit `MainActivity.kt` or `app/build.gradle.kts` (fonts go in `res/font/`, no gradle change needed).
- Keep the existing top-level color `val`s in `MoneyVisuals.kt` (`MoneyBlue`, `MoneyCanvas`, `MoneyInk`, `MoneyMuted`, `MoneyGreen`, `MoneyCoral`, `MoneyMint`, `MoneySoftBlue`, etc.) — untouched screens still import them. Do not delete or change their values in this plan.
- Do NOT change any component's public parameter signature.
- Card radius is 18–20dp (this supersedes the old "keep 8dp" rule). Toss blue is `#3182F6` (light) / `#4593FC` (dark).
- Verification env (PowerShell): `$env:JAVA_HOME='D:\Android Studio\jbr'` then `.\gradlew.bat <task> --no-daemon --console=plain`.

---

### Task 1: Pretendard fonts + Typography

**Files:**
- Create: `app/src/main/res/font/pretendard_regular.otf`
- Create: `app/src/main/res/font/pretendard_medium.otf`
- Create: `app/src/main/res/font/pretendard_semibold.otf`
- Create: `app/src/main/res/font/pretendard_bold.otf`
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/theme/Typography.kt`

**Interfaces:**
- Produces: `val Pretendard: FontFamily`, `val AutoMoneyTypography: Typography` (consumed by Task 4).

- [ ] **Step 1: Download the four Pretendard static OTF weights**

Run (Bash tool):
```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
mkdir -p app/src/main/res/font
base="https://github.com/orioncactus/pretendard/raw/v1.3.9/packages/pretendard/dist/public/static"
curl -fL "$base/Pretendard-Regular.otf"  -o app/src/main/res/font/pretendard_regular.otf
curl -fL "$base/Pretendard-Medium.otf"   -o app/src/main/res/font/pretendard_medium.otf
curl -fL "$base/Pretendard-SemiBold.otf" -o app/src/main/res/font/pretendard_semibold.otf
curl -fL "$base/Pretendard-Bold.otf"     -o app/src/main/res/font/pretendard_bold.otf
```

- [ ] **Step 2: Verify the files downloaded and are real fonts (not HTML error pages)**

Run (Bash tool):
```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
ls -l app/src/main/res/font/
file app/src/main/res/font/pretendard_regular.otf
```
Expected: each file is > 500 KB and `file` reports "OpenType font". If any file is a few KB or reports HTML/text, the URL 404'd — stop and fix the URL/tag before continuing.

- [ ] **Step 3: Create `Typography.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.choiyoonseo.automoney.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

private const val TNUM = "tnum"

val AutoMoneyTypography = Typography(
    displaySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = TNUM),
    headlineMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFeatureSettings = TNUM),
    headlineSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)
```
Note: these 11 styles are the ones the app actually renders; any unlisted M3 style is never used, so Pretendard shows everywhere in practice.

- [ ] **Step 4: Verify it compiles**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL. (`Typography.kt` references `R.font.*`, which now resolve.)

- [ ] **Step 5: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/res/font/pretendard_regular.otf app/src/main/res/font/pretendard_medium.otf app/src/main/res/font/pretendard_semibold.otf app/src/main/res/font/pretendard_bold.otf app/src/main/java/com/choiyoonseo/automoney/ui/theme/Typography.kt
git commit -m "feat(ui): bundle Pretendard font and Typography scale"
```

---

### Task 2: Color tokens (`MoneyColors`)

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/theme/MoneyColors.kt`

**Interfaces:**
- Produces: `data class MoneyColors(canvas, surface, divider, ink, inkSub, muted, primary, positive, negative, isDark)` with `fun soft(accent: Color): Color`; `val LightMoneyColors`, `val DarkMoneyColors`; `val LocalMoneyColors`; `object MoneyTheme { val colors }`. Consumed by Tasks 4, 5, 6.

- [ ] **Step 1: Create `MoneyColors.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MoneyColors(
    val canvas: Color,
    val surface: Color,
    val divider: Color,
    val ink: Color,
    val inkSub: Color,
    val muted: Color,
    val primary: Color,
    val positive: Color,
    val negative: Color,
    val isDark: Boolean
) {
    fun soft(accent: Color): Color = accent.copy(alpha = if (isDark) 0.22f else 0.12f)
}

val LightMoneyColors = MoneyColors(
    canvas = Color(0xFFF2F4F6),
    surface = Color(0xFFFFFFFF),
    divider = Color(0xFFF2F4F6),
    ink = Color(0xFF191F28),
    inkSub = Color(0xFF4E5968),
    muted = Color(0xFF8B95A1),
    primary = Color(0xFF3182F6),
    positive = Color(0xFF00C471),
    negative = Color(0xFFF04452),
    isDark = false
)

val DarkMoneyColors = MoneyColors(
    canvas = Color(0xFF17171C),
    surface = Color(0xFF242429),
    divider = Color(0xFF2E2E35),
    ink = Color(0xFFF2F4F6),
    inkSub = Color(0xFF9DA5B4),
    muted = Color(0xFF8B95A1),
    primary = Color(0xFF4593FC),
    positive = Color(0xFF2AC769),
    negative = Color(0xFFFF6A76),
    isDark = true
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

object MoneyTheme {
    val colors: MoneyColors
        @Composable @ReadOnlyComposable
        get() = LocalMoneyColors.current
}
```

- [ ] **Step 2: Verify it compiles**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/java/com/choiyoonseo/automoney/ui/theme/MoneyColors.kt
git commit -m "feat(ui): add light/dark MoneyColors token layer"
```

---

### Task 3: Shapes + Spacing tokens

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/theme/Shapes.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/theme/Spacing.kt`

**Interfaces:**
- Produces: `val AutoMoneyShapes: Shapes` (Task 4); `object MoneySpacing` with `xs/sm/md/lg/xl/xxl/screen/cardInner/cardGap` dp values (Tasks 5, 6).

- [ ] **Step 1: Create `Shapes.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AutoMoneyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(20.dp)
)
```

- [ ] **Step 2: Create `Spacing.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.theme

import androidx.compose.ui.unit.dp

object MoneySpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val screen = 20.dp
    val cardInner = 18.dp
    val cardGap = 12.dp
}
```

- [ ] **Step 3: Verify it compiles**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/java/com/choiyoonseo/automoney/ui/theme/Shapes.kt app/src/main/java/com/choiyoonseo/automoney/ui/theme/Spacing.kt
git commit -m "feat(ui): add Shapes (18-20dp) and Spacing tokens"
```

---

### Task 4: Wire tokens into `AutoMoneyTheme` + dark preview harness

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/theme/Theme.kt` (full rewrite)
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/PreviewCatalog.kt` (add dark previews)

**Interfaces:**
- Consumes: `LightMoneyColors`, `DarkMoneyColors`, `LocalMoneyColors` (Task 2); `AutoMoneyTypography` (Task 1); `AutoMoneyShapes` (Task 3).
- Produces: `fun AutoMoneyTheme(darkTheme: Boolean = false, content: @Composable () -> Unit)` — the `darkTheme` param defaults to `false` so `MainActivity` (unedited) keeps rendering light; previews pass `true` for dark.

- [ ] **Step 1: Replace the entire contents of `Theme.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun AutoMoneyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val money = if (darkTheme) DarkMoneyColors else LightMoneyColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = money.primary,
            secondary = money.positive,
            tertiary = money.negative,
            background = money.canvas,
            surface = money.surface,
            surfaceVariant = money.primary.copy(alpha = 0.16f),
            onPrimary = Color.White,
            onBackground = money.ink,
            onSurface = money.ink,
            onSurfaceVariant = money.muted
        )
    } else {
        lightColorScheme(
            primary = money.primary,
            secondary = money.positive,
            tertiary = money.negative,
            background = money.canvas,
            surface = money.surface,
            surfaceVariant = money.primary.copy(alpha = 0.10f),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = money.ink,
            onSurface = money.ink,
            onSurfaceVariant = money.muted
        )
    }
    CompositionLocalProvider(LocalMoneyColors provides money) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AutoMoneyTypography,
            shapes = AutoMoneyShapes,
            content = content
        )
    }
}
```

- [ ] **Step 2: Add dark-mode previews to `PreviewCatalog.kt`**

Append these two composables to the end of `PreviewCatalog.kt` (the `HomeScreen` import already exists in that file):

```kotlin
@Preview(name = "홈 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        HomeScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "앱 전체 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppRootDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        AppRoot()
    }
}
```

- [ ] **Step 3: Verify the whole app still builds**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL. The app now renders in Pretendard with 18dp Material shapes; existing screens still compile because their imported color `val`s are untouched.

- [ ] **Step 4: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/java/com/choiyoonseo/automoney/ui/theme/Theme.kt app/src/main/java/com/choiyoonseo/automoney/ui/PreviewCatalog.kt
git commit -m "feat(ui): wire tokens/typography/shapes into AutoMoneyTheme with dark support"
```

---

### Task 5: `SoftShadowCard` + refactor Home-used shared components to tokens

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/components/SoftShadowCard.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/components/MoneyVisuals.kt` (functions `ScreenTitle`, `FinanceSectionCard`, `MetricTile`, `TransactionRow`, `MonthlyFlowCard` only)

**Interfaces:**
- Consumes: `MoneyTheme.colors` (Task 2), `MoneySpacing` (Task 3).
- Produces: `@Composable fun SoftShadowCard(modifier, shape, onClick, content: @Composable ColumnScope.() -> Unit)`.
- Only touch the five functions named above. Leave `ReviewActionCard`, `CategoryBar`, `MoneyHeroCard`, `MoneyFlowHeroCard`, `IllustratedSummaryCard`, `MonthlyFlowCardOld`, and all top-level color `val`s exactly as they are.

- [ ] **Step 1: Create `SoftShadowCard.kt`**

```kotlin
package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.ui.theme.MoneyTheme

@Composable
fun SoftShadowCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MoneyTheme.colors
    val surface = if (colors.isDark) {
        Modifier.border(1.dp, colors.divider, shape)
    } else {
        Modifier.shadow(elevation = 6.dp, shape = shape, clip = false)
    }
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(surface)
            .clip(shape)
            .background(colors.surface)
            .then(clickMod)
            .padding(18.dp),
        content = content
    )
}
```

- [ ] **Step 2: Refactor `ScreenTitle` (MoneyVisuals.kt)**

Replace the `color = MoneyInk` line in `ScreenTitle`'s title `Text` with a token read. At the top of the `ScreenTitle` function body (before the `Row`), add:
```kotlin
    val colors = MoneyTheme.colors
```
Then change:
```kotlin
                color = MoneyInk
```
to:
```kotlin
                color = colors.ink
```

- [ ] **Step 3: Refactor `FinanceSectionCard` (MoneyVisuals.kt)**

At the top of `FinanceSectionCard`'s body add `val colors = MoneyTheme.colors`. Then apply these replacements inside this function only:
- `shape = RoundedCornerShape(8.dp),` → `shape = RoundedCornerShape(18.dp),`
- `color = Color.White` (the `Surface` container) → `color = colors.surface`
- the title `Text(... fontWeight = FontWeight.Bold)` — add `color = colors.ink,` before `fontWeight`
- `Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MoneyMuted)` → `color = colors.muted`

- [ ] **Step 4: Refactor `MetricTile` (MoneyVisuals.kt)**

At the top of `MetricTile`'s body add `val colors = MoneyTheme.colors`. Then:
- `shape = RoundedCornerShape(8.dp),` → `shape = RoundedCornerShape(18.dp),`
- `color = Color.White` (the tile `Surface`) → `color = colors.surface`
- the value `Text(metric.value, ... fontWeight = FontWeight.Bold)` — add `color = colors.ink,`
- `color = MaterialTheme.colorScheme.onSurfaceVariant,` (helper text) → `color = colors.muted,`

- [ ] **Step 5: Refactor `TransactionRow` (MoneyVisuals.kt)**

At the top of `TransactionRow`'s body add `val colors = MoneyTheme.colors`. Then:
- merchant `Text(transaction.merchant, ...)` — add `color = colors.ink,`
- the amount `Text(formatWon(...), ...)` — change `color = if (transaction.amountWon < 0) Color(0xFFD64545) else MoneyGreen` to `color = if (transaction.amountWon < 0) colors.negative else colors.positive`
- the category/method `Text(..., color = MaterialTheme.colorScheme.onSurfaceVariant, ...)` → `color = colors.muted,`

- [ ] **Step 6: Refactor `MonthlyFlowCard` (MoneyVisuals.kt)**

At the top of `MonthlyFlowCard`'s body add `val colors = MoneyTheme.colors`. Then, in `MonthlyFlowCard` only (not `MonthlyFlowCardOld`):
- `shape = RoundedCornerShape(8.dp),` → `shape = RoundedCornerShape(20.dp),`
- `color = Color.White` (outer `Surface`) → `color = colors.surface`
- `Text("$title  >", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)` — add `color = colors.ink,`
- `Text(period, color = MoneyMuted, ...)` → `color = colors.muted,`
- `Text("남은 돈", color = MoneyInk, fontWeight = FontWeight.Medium)` → `color = colors.inkSub,`
- the remaining-amount `Text(remainingValue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)` — add `color = colors.ink,`

- [ ] **Step 7: Add the token import to `MoneyVisuals.kt`**

Ensure this import is present near the other `com.choiyoonseo.automoney.ui.*` imports:
```kotlin
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
```

- [ ] **Step 8: Verify build + existing unit tests still green**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL and all existing tests pass (no palette test asserts the neutral tokens we changed; accent `val`s are untouched).

- [ ] **Step 9: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/java/com/choiyoonseo/automoney/ui/components/SoftShadowCard.kt app/src/main/java/com/choiyoonseo/automoney/ui/components/MoneyVisuals.kt
git commit -m "feat(ui): add SoftShadowCard and migrate Home components to tokens/18dp"
```

---

### Task 6: Apply tokens to Home + bottom nav, add Home dark/light previews

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt` (canvas background → token)
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt` (Scaffold + NavigationBar colors → tokens) — **claim first**
- Modify: `docs/AI_COLLABORATION.md` (add + then note claim), `docs/visual-refresh-progress.md` (progress note)

**Interfaces:**
- Consumes: `MoneyTheme.colors` (Task 2). No new produced symbols.

- [ ] **Step 1: Claim `AppRoot.kt`**

In `docs/AI_COLLABORATION.md`, under `<!-- active claims below -->` in the "Shared File Claims" section, add:
```
- [2026-07-08] Claude claims ui/AppRoot.kt — token-based bottom nav + scaffold canvas for Toss restyle
```

- [ ] **Step 2: Point Home's canvas background at the token**

In `HomeScreen.kt`, add near the other theme import:
```kotlin
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
```
At the start of the `HomeScreen` composable body (before the `Column`), add:
```kotlin
    val colors = MoneyTheme.colors
```
Change:
```kotlin
            .background(MoneyCanvas)
```
to:
```kotlin
            .background(colors.canvas)
```
Leave the `MoneyCanvas` import in place (harmless) or remove it if now unused — if you remove it, confirm no other reference remains in the file first.

- [ ] **Step 3: Make the Scaffold + bottom bar theme-aware in `AppRoot.kt`**

Add import:
```kotlin
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
```
At the top of the `AppRoot` composable body (before `Scaffold`), add:
```kotlin
    val colors = MoneyTheme.colors
```
Then replace the Scaffold/NavigationBar color literals:
- `containerColor = MoneyCanvas,` → `containerColor = colors.canvas,`
- `NavigationBar(containerColor = Color.White) {` → `NavigationBar(containerColor = colors.surface) {`
- `selectedIconColor = MoneyBlue,` → `selectedIconColor = colors.primary,`
- `selectedTextColor = MoneyBlue,` → `selectedTextColor = colors.primary,`
- `indicatorColor = MoneySoftBlue,` → `indicatorColor = colors.primary.copy(alpha = 0.12f),`
- `unselectedIconColor = MoneyMuted,` → `unselectedIconColor = colors.muted,`
- `unselectedTextColor = MoneyMuted` → `unselectedTextColor = colors.muted`

- [ ] **Step 4: Verify build + previews render**

Run (PowerShell tool):
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL.

Then in Android Studio, open `PreviewCatalog.kt` and confirm the "홈", "홈 · 다크", "앱 전체", and "앱 전체 · 다크" previews render: light = gray `#F2F4F6` canvas + white 18–20dp cards + Toss-blue selected tab; dark = `#17171C` canvas + `#242429` cards + `#4593FC` selected tab; amounts in Pretendard tabular figures. Capture a screenshot of the light and dark Home previews.

- [ ] **Step 5: Update progress docs**

In `docs/visual-refresh-progress.md`, append:
```markdown
## Phase 8 Done (Toss token restyle — foundation + Home)

- Added `ui/theme` token layer: `MoneyColors` (light+dark), Pretendard `Typography`, `Shapes` (18-20dp), `Spacing`.
- `AutoMoneyTheme` now supports `darkTheme`; live app still defaults to light until all screens convert.
- Added `SoftShadowCard`; migrated Home components (ScreenTitle, FinanceSectionCard, MetricTile, TransactionRow, MonthlyFlowCard) to tokens + rounder cards.
- Reworked Home canvas and bottom nav to tokens; added light+dark Home previews.
- Supersedes the earlier "keep 8dp radius" rule.
```
In `docs/AI_COLLABORATION.md`, remove the claim line added in Step 1 only **after** this work merges to `main` (leave it for now; note in the PR that it should be dropped post-merge).

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/cys04/Desktop/AutoMoney"
git add app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt docs/AI_COLLABORATION.md docs/visual-refresh-progress.md
git commit -m "feat(ui): apply Toss tokens to Home + bottom nav, add dark previews"
```

---

## Self-Review

**Spec coverage:**
- Token layer (colors light+dark, typography, shapes, spacing) → Tasks 1–3. ✓
- `AutoMoneyTheme` dark support + Material mapping → Task 4. ✓
- Pretendard bundled + tabular figures → Task 1. ✓
- API-stable component restyle + `SoftShadowCard` → Task 5. ✓
- `MoneyBottomBar`: the spec proposed a new component, but `AppRoot` already has a `NavigationBar`; re-theming it in place (Task 6) achieves the same rounded/colored bottom nav with less churn and no navigation-logic risk. Deviation is intentional and lower-risk. ✓
- Home + bottom nav validation with light/dark previews → Task 6. ✓
- Icon-chip flow/row mix → already the existing structure (`FlowStep` rounded-square chips, `IconBadge` circles); tokenized, no shape change needed. ✓
- Legacy/dead components + illustrations: left untouched per plan constraints; cleanup deferred. ✓
- Codex review-flow UI notes: explicitly out of scope for this foundation plan (sequenced after Home per spec). ✓

**Placeholder scan:** No TBD/TODO; every code step shows concrete code; every command has expected output. ✓

**Type consistency:** `MoneyColors` field names (`canvas/surface/divider/ink/inkSub/muted/primary/positive/negative/isDark`) are used identically in Tasks 4–6. `MoneyTheme.colors`, `AutoMoneyTypography`, `AutoMoneyShapes`, `MoneySpacing`, `SoftShadowCard`, and `AutoMoneyTheme(darkTheme=…)` match across producing and consuming tasks. ✓

**Adaptation note:** This is a visual restyle, so tasks verify via `:app:assembleDebug` + `@Preview` + the existing unit suite rather than new unit tests — there is no new business logic to unit-test, and inventing tautology tests over color constants would be noise.
