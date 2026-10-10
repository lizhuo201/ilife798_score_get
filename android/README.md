# 798积分助手（Android）

`auto_score.py` 的原生 Android 移植：Kotlin + Jetpack Compose（MVVM + Repository），网络层 OkHttp + 协程，JSON 用 kotlinx.serialization。

- 包名：`com.isuncy.ilife798`，应用名：798积分助手
- minSdk 24 / targetSdk 35 / compileSdk 35，JDK 17，AGP 8.7.3，Gradle 8.10.2（wrapper 已附带）

## 功能

- 账号列表：手机号、Token 有效状态、当前积分
- 一键执行：多账号依次执行每日任务（签到 / 广告类任务），对齐 Python 版过滤、限次与流水防重规则，含随机间隔
- App 内登录：手机号 → 图形验证码（App 内显示）→ 短信验证码 → 自动保存凭证
- SAF 导入：直接读取 Python 版 `ilife_accounts.json`（格式互认，`[{"phone","token","uid"}] / [{"un","token","uid"}]`）
- 实时日志 + 历史记录（`filesDir/history.json`）
- 执行期间 ForegroundService（dataSync）保活本次执行，可锁屏/切后台

## 构建

本机需 JDK 17 + Android SDK；也可以直接推送到 GitHub，仓库根目录的
`.github/workflows/android-build.yml` 会自动执行 `./gradlew test` 与 `assembleDebug`，
产物 `798-score-helper-debug.apk` 挂在 Action 的 artifact 上。

本地构建：

```bash
cd android
./gradlew test assembleDebug
# 产物：android/app/build/outputs/apk/debug/app-debug.apk
```

## 测试

`./gradlew test`：签名向量（与 Python get_sign 一致的固定向量）、任务过滤/限次/类型、mission-lst 与流水解析。

## 数据与安全

- 凭证/历史均为本地明文 JSON（与 Python 版一致），保存在应用私有目录
- 自动化请求存在账号被平台限制或封禁的风险，请自行评估

## 协议对齐说明

请求头、get_sign、登录两步流程、mission-lst/score-lst/score-send 的字段解析与成功判定
（code/status 为 0、200、None）均按 `auto_score.py` 1:1 移植。
