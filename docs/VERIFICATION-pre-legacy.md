> 历史记录：以下为 Legacy 身份迁移和正式 CR 0.4.0 专服兼容修复之前的检查，不能用于当前发行版的兼容结论。当前结果见 [正式基线验收](../LEGACY-VERIFICATION.md)。

# alpha.2 验证记录

日期：2026-11-03。版本：`5.1.0-cleanroom-alpha.2`。Minecraft 1.12.2 / Java 25.0.5。
最低加载器声明为 [官方 Cleanroom 0.4.0-alpha](https://github.com/CleanroomMC/Cleanroom/releases/tag/0.4.0-alpha)。

## 兼容基线结果

| 运行环境 | Ponder | 结果 | 证据 |
| --- | --- | --- | --- |
| 官方 CR 0.4.0 客户端 + 集成服务器 + 完整世界关闭重载 | 未安装 | **PASS** | [报告](verification/cleanroom-0.4.0-alpha/client-without-ponder/report.json)、[安装环境](verification/cleanroom-0.4.0-alpha/client-without-ponder/runtime.json)、[日志](verification/cleanroom-0.4.0-alpha/client-without-ponder/startup.log) |
| 官方 CR 0.4.0 客户端 + 集成服务器 + 世界重载 + 教程播放 | 安装 | **PASS** | [报告](verification/cleanroom-0.4.0-alpha/client-with-ponder/report.json)、[安装环境](verification/cleanroom-0.4.0-alpha/client-with-ponder/runtime.json)、[日志](verification/cleanroom-0.4.0-alpha/client-with-ponder/startup.log) |
| 官方 CR 0.4.0 专用服务器 | 未安装 | **上游阻塞，未通过** | [结果](verification/cleanroom-0.4.0-alpha/server-without-ponder/result.json)、[日志](verification/cleanroom-0.4.0-alpha/server-without-ponder/startup.log) |
| 官方 CR 0.4.0 专用服务器 | 安装 | **上游阻塞，未通过** | [结果](verification/cleanroom-0.4.0-alpha/server-with-ponder/result.json)、[日志](verification/cleanroom-0.4.0-alpha/server-with-ponder/startup.log) |
| 官方 CR 0.4.0 空服，不放任何外部 Mod | 未安装 | **复现相同加载器崩溃** | [结果](verification/cleanroom-0.4.0-alpha/empty-server/result.json)、[日志](verification/cleanroom-0.4.0-alpha/empty-server/startup.log) |
| CR 0.7.14 userdev 专服，使用 alpha.2 发布 JAR | 未安装 | PASS | [报告](verification/cleanroom-0.7.14-alpha/server-standalone/report.json)、[日志](verification/cleanroom-0.7.14-alpha/server-standalone/startup.log) |
| CR 0.7.14 userdev 专服，使用 alpha.2 发布 JAR | 安装 | PASS | [报告](verification/cleanroom-0.7.14-alpha/server-with-ponder/report.json)、[日志](verification/cleanroom-0.7.14-alpha/server-with-ponder/startup.log) |
| EnergyStore 单元测试 | 不依赖 | 2 项通过 | [JUnit XML](verification/EnergyStoreTest.xml) |
| 本地化 / 资源静态检查 | 不依赖 | PASS | [本地化审计](verification/localization.json) |
| 发布包隔离与来源检查 | 不依赖 | PASS | [release.json](verification/release.json) |

两组 CR 0.4.0 客户端报告各含 218 条检查记录，其中包含逐物品模型检查，不代表 218 个独立玩法。0.7.14 的两组专服各有 7 条检查记录。

**客户端最低版本验收通过；CR 0.4.0-alpha 专用服务器验收尚未通过。**
较新版本的通过结果没有用来替代 0.4.0 的专服基线验收。alpha.1 的历史结果另见 [原记录](VERIFICATION-alpha1.md)。

### 官方 0.4.0 安装环境

- 直接使用原版 Minecraft 游戏 JAR、官方 CR 0.4.0 universal 和官方 MMC 配置列出的运行库，不经过 userdev 的 SRG → MCP 运行时重映射。
- 官方配置包验证 SHA-256，各运行库和原版游戏文件验证配置中的 SHA-1；安装包摘要记录于每个变体的 `runtime.json`。
- Powah 和 Ponder 均加载重混淆的安装 JAR。测试模组独立重混淆到 SRG，不进入发布包。
- Ponder `1.4.0-cleanroom-alpha.1` 与 CraftTweaker `4.1.20.698` 独立安装，来源锁定见 [runtime-lock.json](../runtime-lock.json)。
- 编译工具链为 CleanroomGradle 0.22.0 / Gradle 9.8.1 / CR 0.7.14 userdev。它和最低运行版本分别配置。
- 图形环境为 Linux / Mesa llvmpipe，1280 × 720；中文大缩放截图遵循 1.12 Unicode 字体的有效 GUI 缩放。
- Linux 视觉验证使用 `ALSOFT_DRIVERS=null`，不验收声音输出。

### CR 0.4.0 专服阻塞

官方加载器在 `Loader.identifyMods` 调用 `KirinoCore.identifyMods` 时加载客户端类，触发：

```text
java.lang.NoClassDefFoundError: net/minecraft/client/entity/EntityPlayerSP
Caused by: java.lang.RuntimeException:
Attempted to load class bud/net.minecraft.client.entity.EntityPlayerSP for invalid side SERVER
```

安装 Powah、有 / 无 Ponder，以及空 `mods/` 目录均可复现，发生在 Powah 初始化之前。
测试没有改动官方加载器。该异常可能令进程返回 0，但验证器要求真实 started 阶段生成 PASS 报告，故仍将三次启动记录为失败。
测试服务器只绑定 `127.0.0.1`，使用临时端口；EULA 沿用用户为本项目本地测试服务器明确给出的许可。

## 本地化和手册

- 553 个语言键中英一致，145 个注册物品 / 方块名称均有简体中文，无遗漏名称键。
- 上游 28 篇手册页面全部导入英文和简体中文，页面集合与链接一致，插图文件齐全。
- 保留机器 UI、提示、Ponder 标题和讲解的中英文本；补充的操作步骤、手册工具栏和搜索提示也有本地化。
- 真实客户端检查手册的首页、原插图、参数表和实际工作台配方，执行滚动、历史后退 / 前进、全宽切换、英文和中文搜索跳转；截图包含小视口的中文搜索。
- 手册布局和两个 UI 素材源自 GuideME 21.1.7；渲染代码为 1.12 回迁实现，不声称运行了现代 GuideME 引擎或其扩展 API。来源与截图见 [UI 记录](UI.md)。

## 实际玩法验证

- 所有 107 个方块、145 个物品注册成功；156 个工作台配方实际加载。
- 逐个检查物品模型和材质；所有注册方块的状态具有可烘焙世界模型。
- 真实工作台匹配绝缘膏配方，产出 24 份，返还空桶。
- 熔炉发电机消耗煤，储能与剩余燃料合计严格为 48,000 FE。
- 熔岩发电机的储能、燃料缓存与剩余熔岩合计等于输入的能量。
- 太阳能板在日光下发电；热力发电机响应实际热源并消耗水。
- 充能棒消耗 10,000 FE，实际让充能球将铁锭和金锭变为 2 份充能钢。
- 更改充能球原料会清除进度；不消耗其他原料。四份烈焰粉需分别占槽，恰好充入 120,000 FE 后产出水晶。
- 充能球拒绝普通 FE 输入；自动化保留原料，仅模拟 / 提取成品。
- 混合等级线缆传输能量，保留全部输入能量，遵守最低等级线缆的速率。
- 在 0.7.14 userdev 的专用服务器上，线缆的仅输入面阻止穿越；改成仅输出后精确传输 2,000 FE。
- 末影单元实际消耗能量单元，扩充容量并继承已有能量；末影门将其传输给远端机器。
- 不同所属玩家和频道保持隔离；空电池也能扩容并持久保存。
- 能量漏斗向真实箱子中的电池精确传输 20,000 FE；放电器精确提取 25,000 FE；玩家发射器给被绑定玩家的背包传输 10,000 FE。
- 生存模式放置反应堆恰好消耗 36 个部件，创建全部结构和控制器链接。
- 反应堆消耗铀燃料并发电；外侧能源端口连接控制器，并遵守侧面禁用设置。
- 拆除任意一个反应堆部件会拆解整座结构，返还恰好 36 个部件；重新放置后储能不丢失、不增加。
- 实际 GUI 控制包在服务器修改红石模式；停用后能源与燃料保持不变。
- 按上游原始贴图打开全部机器界面并截图；英文 GUI 缩放 2，中文熔炉发电机 GUI 缩放 4。
- 在真实界面点击红石图标、末影频道直选、全部侧面配置与线缆方向按钮；服务端状态随操作改变。
- 能量单元的两个充电槽分别传输 1,000 FE，储能恰好减少 2,000 FE。
- 在末影单元界面实际 Shift 点击背包电池，确认隐藏的扩容槽消耗物品并为所选频道增加 1,000,000 FE 容量。
- 通过流体表实际注入和抽取熔岩桶，核对返回容器与流体量。
- 线缆按世界中点击的面打开 153×29 的紧凑界面，不添加玩家背包槽位。
- 完整退出并重载同一世界后，机器能源、红石模式、末影网络容量、无线传输后的能源和反应堆结构仍然正确。

## Ponder 验证

- `power`、`energizing`、`reactor`、`ender`、`charging` 五个结构加载无诊断。
- Powah 注册的所有教程条目在播放验证时无诊断；各物品使用唯一场景 ID。
- 编译五类教程，并逐刻执行到各自完整时间轴结束。
- 实际打开 Ponder 界面，检查英文 / 中文充能教程以及反应堆完整结构渲染。
- 未安装 Ponder 的客户端也实际打开了手册、机器 GUI 并完成玩法和重载验证。
- 在 0.7.14 userdev 的专用服务器中，包含和不包含 Ponder 时均进入 started 阶段，使用 CommonProxy，运行真实机器刻并正常保存停服。CR 0.4.0 的专服结果见上面的阻塞说明。

## 截图与重现

完整界面来源、尺寸和截图见 [UI 对齐记录](UI.md)。以下为官方 CR 0.4.0 使用安装包的原图：

| 内容 | 图片 |
| --- | --- |
| 中文手册 | [首页](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/guide-zh.png) |
| 原版插图与参数表 | [熔炉发电机](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/guide-furnator-zh.png) |
| 工作台配方 | [配方](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/guide-recipes-en.png) |
| 中文搜索 / 大缩放 | [搜索](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/guide-search-zh-large-scale.png) |
| 中文机器界面 | [熔炉发电机](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/machine-zh-large-scale.png) |
| 反应堆界面 | [UI](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/reactor-ui.png) |
| 中文思索播放 | [充能](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/ponder-zh.png) / [反应堆](verification/cleanroom-0.4.0-alpha/client-with-ponder/screenshots/ponder-reactor.png) |

```bash
./tools/dev.sh :build :reobfDevHarnessJar :reobfServerHarnessJar :ponder:reobfJar
python3 tools/verify-cleanroom.py client
python3 tools/verify-cleanroom.py client --with-ponder
python3 tools/verify-cleanroom.py server
python3 tools/verify-cleanroom.py server --with-ponder
python3 tools/verify-cleanroom.py server --empty
```

运行目录为 `run/cleanroom-0.4.0-alpha/<变体>/`，报告归档至 `build/reports/cleanroom-0.4.0-alpha/`。
测试程序通过 `loadWorld(null)` 关闭并等待内置服务器，再重新进入同一存档；单独预发断开包会令测试程序等待一个已经停止的线程，本次已修正测试程序的顺序。

大型整合包、第三方容器的全部组合、长期高负载和远程多人联机未全面测试。当前没有充能配方的专用 JEI 分类。
Ponder 在退出期间恢复 Forge 注册表时可能短暂提示 Powah 方块未注册；实际播放与加载阶段的结构和场景检查均通过。网络版本检查超时不参与玩法结果判定。
