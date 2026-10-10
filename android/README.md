# 798积分助手（Android）

`auto_score.py` 的原生 Android 移植：Kotlin + Jetpack Compose（MVVM + Repository），网络层 OkHttp + 协程，JSON 用 kotlinx.serialization。

- 包名：`com.isuncy.ilife798`，应用名：798积分助手
- minSdk 24 / targetSdk 35 / compileSdk 36，JDK 17，AGP 8.12.3，Gradle 8.13（wrapper 已附带），Kotlin 2.2.21，Compose BOM 2025.10.01

## 功能

- 账号列表：手机号、Token 有效状态、当前积分
- 一键执行：多账号依次执行每日任务（签到 / 广告类任务），对齐 Python 版过滤、限次与流水防重规则，含随机间隔
- App 内登录：手机号 → 图形验证码（App 内显示）→ 短信验证码 → 自动保存凭证
- SAF 导入：直接读取 Python 版 `ilife_accounts.json`（格式互认，`[{"phone","token","uid"}] / [{"un","token","uid"}]`）
- 实时日志 + 历史记录（`filesDir/history.json`）
- 执行期间 ForegroundService（dataSync）保活本次执行，可锁屏/切后台
- 液态玻璃胶囊底部导航（io.github.kyant0:backdrop 1.0.0，即 AndroidLiquidGlass；API 31+ 模糊、API 33+ 折射，低版本优雅降级）
- 首次启动免责声明：同意后进入，不同意自动退出（通知权限申请也移到同意之后）

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

`./gradlew test`：签名固定向量（含服务端时间校正）、时间工具、任务过滤/限次/类型/签到判重、mission-lst 与流水解析。

## 数据与安全

- 凭证/历史均为本地明文 JSON（与 Python 版一致），保存在应用私有目录
- 自动化请求存在账号被平台限制或封禁的风险，请自行评估

## 协议对齐说明

请求头、登录两步流程、mission-lst/score-lst/score-send 的字段解析与成功判定
（code/status 为 0、200、None）均按 `auto_score.py` 移植。

积分上报的签名与请求体已对齐 2026-10 之后的线上版本（旧脚本的算法会被服务端判为
`code=-1 msg=验签失败`，仓库里的 `auto_score.py` 本身也已失效）：

- 签名：`v20 = 10 * ((serverTs - localTs + nowMs) / 10000)`，
  `sign = md5(adId + v20 + token[-8:] + uid[-8:] + SALT)`；`localTs` 取请求 mission-lst 前的本机毫秒，
  `serverTs` 取该响应顶层 `time` 字段（毫秒）；偏差超过 30 分钟时忽略校正。
- 上报：`POST /api/v1/acc/score/score-send?sign=...&s=<s>`。第三方还原实现分成两派，
  执行器按 `MissionLogic.SEND_VARIANTS` 顺序探测并在成功后固定沿用：
  `s=1` + 极简 body（官方 App / web 工具通道）→ `s=true` + 极简 body（dex 还原写法）
  → `s=true` + 带 `addScore` → `s=1` + 带 `addScore`。失败时日志会打印所用组合，
  全部组合失败才计一次失败并跳过该任务剩余次数；被限流（`code=-98`）时等 9 秒重试同一组合（最多 2 次）。
- 请求体：普通任务 `{"adId":...,"type":101}`（部分组合再附 `addScoreType:1`/`addScore`）；
  每日签到 `{"adId":...,"weekDay":1..7}`（北京时间的 ISO 星期，任何组合都不带分数）。
  旧脚本携带的小写 `weekday` 会被服务端视为无效请求。
- 每日签到判重：`daily.ltime` 同日、今日流水计数、`daily.week` 位图（bit0 = 周一）任一成立即视为已签到。
