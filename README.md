# 鱼的梦（fishdream）

一个「用完即走」的本地临时笔记 App：不需要账号、不需要后端、不需要联网。

## 交付物

- APK：`D:\安装包\鱼的梦-debug.apk`（debug 签名，可直接安装）
- 工程：本目录（`D:\tonggong\deepcode2\fishdream`）

## 已实现的功能（第一版）

- 新建 / 编辑 / 删除笔记（编辑即续期，按当前时间重算到期时刻）
- 保留时间：今天 / 3 天 / 一周 / 本月 / 永久
- 到期自动进入回收站（**惰性结算**：每次打开、刷新列表时按时间戳比较，不需要后台任务）
- 回收站：恢复 / 永久删除 / 一键清空（都带二次确认）
- 标签：创建、给笔记加多个标签、按标签筛选、删除标签
- 搜索：按正文与标签名模糊匹配
- 本地持久化：系统自带 SQLite（`SQLiteOpenHelper` + 手写 DAO）
- JSON 导入 / 导出备份（通过系统文件选择器，追加导入不覆盖）
- 反馈入口：主界面顶栏的信封图标「反馈与建议」，提示 QQ 搜索 535391647 加入群聊，并可一键复制群号
- 完全离线：**不声明 INTERNET 权限**，`allowBackup=false`，关闭云备份与设备迁移

## 关键设计约定

| 项 | 约定 |
|---|---|
| 默认保留时间 | 新建笔记默认「一周」 |
| 今天 | 当天 23:59:59.999 |
| 3 天 | 创建时间 + 72 小时 |
| 一周 | 创建时间 + 7 天 |
| 本月 | 当月最后一天 23:59:59.999 |
| 永久 | `expires_at = NULL`，永不过期 |
| 标签 | 只做分类，**不影响**过期时间 |
| 恢复 | 从回收站恢复后转为「永久保存」 |
| 回收站 | 永久保留，直到手动删除或清空 |
| 过期判定 | 存绝对时间戳 `expires_at`，用 `now` 比较；`Clock` 可注入，便于测试 |

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose（BOM 2025.01.00）+ Material 3
- AGP 8.7.3 / Gradle 9.3.0 / JDK 17（JBR 25 运行）
- compileSdk 35、minSdk 26、targetSdk 35
- 第三方依赖仅 androidx 与 kotlinx-coroutines（**没有 Room / KSP / WorkManager / Navigation**，因为构建环境无法联网，只能用本机 Gradle 缓存里已有的依赖）

## 重新构建（离线）

```powershell
$env:JAVA_HOME='D:\App\APP_studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
cd D:\tonggong\deepcode2\fishdream
.\gradlew.bat assembleDebug --offline
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

## 已知限制

- 没有 JUnit（本机 Gradle 缓存缺失），因此没有自动化单元测试；过期逻辑已写成注入 `Clock` 的纯函数，联网后可补测试。
- 未做真机运行验证：本机没有可用 AVD，也没有连接的调试设备。
- 临近午夜创建的「今天」笔记会很快到期，界面上有提示，但未做最小保留时长保护。
