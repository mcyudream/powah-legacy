# Powah Legacy

![Powah Legacy](branding/cover.png)

<!-- legacy-overview:start -->
把 Powah 的分级 FE 能源体系带到 1.12.2：从基础发电与线缆开始，逐步解锁充能材料、反应堆、跨维度末影能源网络和玩家充电设备。

## 当前状态

本版本已完成当前声明范围的正式 Cleanroom 0.4.0 安装包验收。实际运行包的 SHA-256、检查项和限制见 [验收记录](LEGACY-VERIFICATION.md)。这不表示上游所有扩展或任意整合包组合均已验证。

<!-- legacy-authorship:start -->
## 作者与来源 / Authors and upstream

- 此 Legacy 版本作者与维护者 / Legacy author and maintainer：**yudream**。
- 原作者 / Original author(s)：**owmii (original), Technici4n and Powah contributors**。
- 原仓库 / Original repository：[Technici4n/Powah](https://github.com/Technici4n/Powah)。
- 上游基线：`5.1.0-alpha`。
- 固定上游提交：[`959d5657b14f`](https://github.com/Technici4n/Powah/commit/959d5657b14fada803bc06b024acb5eef8156ab7)。
- 本版本独立维护；上游版权、许可证及资源来源声明保留于 LICENSE / NOTICE 文件中。
<!-- legacy-authorship:end -->

## 运行环境与身份

| 项目 | 要求 / 标识 |
| --- | --- |
| Minecraft | 1.12.2 |
| 运行基线 | Cleanroom **0.4.0 正式版（不带 `-alpha`）** |
| Java | 25 |
| 加载器 Mod ID | `powah-legacy` |
| 资源与原注册命名空间 | `powah`（保留） |

Mod 的 alpha 版本号与加载器版本无关。开发 SDK 可以较新，兼容结论必须来自上述正式运行基线。
加载器身份改名后，安装时应替换本项目旧 jar；原资源命名空间的保留不构成跨实现的存档转换承诺。

## 功能与迁移范围

- 七个等级的发电、储能、线缆、充能球/棒与创造能量单元。
- 36 部件反应堆、末影单元与末影门、绑定卡、电池和玩家能源设备。
- 156 个工作台配方、14 个充能配方，以及中英文 28 篇使用手册。

## 与上游的具体差异

| 项目 | 上游基线 | 本 Legacy 版本 |
| --- | --- | --- |
| 运行平台 | Powah 5.1.0-alpha / Minecraft 1.21.1 | 使用 1.12 方块实体、容器、网络、世界生成和 Forge Energy 重写运行层。 |
| 材料体系 | 现代物品标签与蓝冰、深板岩地形 | 标签转矿物词典；青金石用 dye:4；新增可合成的 powah:blue_ice；自然矿脉适配 1.12 石头地形。 |
| 能量接口 | 现代能源实现 | 内部使用 long 保存能源；对外 1.12 Forge Energy 接口遵守 int 上限。 |
| 游戏内手册 | GuideME 21.1.7 | 回迁阅读布局、搜索、历史、插图与配方展示；不依赖现代 GuideME，也不提供其完整扩展 API。 |
| 渲染与教程 | 上游现代模型/界面与集成 | 转换模型和 GUI 坐标；新增五套可选 Ponder 演示，手册可跳转教程。 |
| 配方查看器 | 现代 Curios / EMI 等集成 | 标准工作台和熔炼配方可用；专用充能 JEI 分类、Curios 与 EMI 未回迁。 |

## 尚未实现与兼容边界

- 当前为 alpha；大型整合包、长时间高负载能源网络和远程多人联机仍需进一步验证。
- 能量漏斗只修改可安全写回的容器和 IItemHandlerModifiable，不写入只读处理器。
- 深板岩矿保留为可注册方块，但默认矿脉适配 1.12 世界；不表示移植现代世界生成系统。

## 依赖

- 核心玩法只需 Cleanroom。
- Ponder Legacy 可选，独立安装；启用时还需 CraftTweaker 4.1.20.698。

## 使用方式

1. 先合成绝缘膏与使用手册，再建立发电机→线缆→能量单元链路。
2. 用充能棒给充能球供能，按手册推进充能钢和各级水晶。
3. 使用末影单元的所属玩家/频道连接跨维度网络；反应堆按 3×4×3 空间自动搭建。

## 构建与 CI

在完整源码项目目录使用 Java 25 执行：

```bash
python3 tools/bootstrap_legacy.py
bash gradlew --no-daemon :build :reobfJar --console=plain
python3 tools/verify_legacy_package.py
```

CI 会准备固定版本的源码依赖，构建完整主源码与测试，并检查发行 jar 的身份、作者和图标。
仅有文档的仓库会明确报告 scaffold，不把跳过编译标为 Mod 构建成功。CI 构建不能替代客户端玩法和专服验收。

## 发布平台

CurseForge / Modrinth 的项目资料、文件、依赖和 CI 凭据配置见 [发布说明](docs/PUBLISHING.md)。平台尚未发布的状态见 PUBLICATION.md。

## 授权与图像

代码与上游资源按各自原始授权保留归属；见 LGPL-3.0 LICENSE、NOTICE.md、guide-ui-lock.json。

此版本作者为 **yudream**，上游作者与版权保留如上。`branding/icon.png` 和 `branding/cover.png` 为本项目生成的展示图，
以代表物品或概念表现项目；不作为游戏实机截图，也不改变上游游戏资源的授权。生成记录见 `branding/provenance.json`。

<!-- legacy-overview:end -->

## 详细使用与开发说明

将 [Technici4n/Powah](https://github.com/Technici4n/Powah) 的能源玩法移植到 **Minecraft 1.12.2 / Cleanroom / Java 25**，并可选接入本仓库的 Ponder。

当前版本：`5.1.0-cleanroom-alpha.2`。这是独立维护的回迁版本。上游基线为 `1.21.1` 分支的 `5.1.0-alpha`，固定提交为 [`959d5657b14fada803bc06b024acb5eef8156ab7`](https://github.com/Technici4n/Powah/tree/959d5657b14fada803bc06b024acb5eef8156ab7)。

## 安装

| 组件 | 版本 / 要求 |
| --- | --- |
| Minecraft | 1.12.2 |
| Cleanroom | **0.4.0 正式版**，当前检查状态见 PUBLICATION.md |
| Java | 25 |
| Powah | `build/libs/powah-legacy-5.1.0-cleanroom-alpha.2.jar` |
| Ponder（可选） | 本仓库 `../ponder` 的 `1.4.0-cleanroom-alpha.1` |
| CraftTweaker（使用 Ponder 时） | 4.1.20.698，Ponder 的独立运行依赖 |

将 Powah 的普通 `.jar` 放入客户端及服务器的 `mods/`。`-mcp.jar` 用于开发环境，`-sources.jar` 是源码包。

Ponder 不随 Powah 打包。不安装 Ponder 时，机器、配方、手册和存档功能仍可正常使用；安装 Ponder 时，另行安装它及 CraftTweaker。参见 [Ponder 安装说明](https://github.com/mcyudream/ponder-legacy)。这是 Java 25 / Cleanroom 工程。

本版已加入限定于正式 Cleanroom 0.4.0 专服的 Kirino 发现兼容修复。历史失败日志保留在旧报告中；当前包的检查结果单独归档。

## 已移植的内容

- 107 个方块、38 个独立物品，共 145 个物品注册项。
- 入门、基础、硬化、烈焰、钻石、绿宝石、终极七个等级，以及创造能量单元。
- 熔炉、熔岩、热力、太阳能发电机与 36 部件的晶质铀反应堆。
- 能量单元、支持混合等级的线缆、充能球与充能棒。
- 按玩家 UUID / 频道持久保存的末影单元、末影门和跨维度能源网络。
- 电池、能量漏斗、放电器、玩家能量发射器、绑定卡与跨维度绑定卡。
- 扳手旋转、侧面输入输出配置、充能棒连接、潜行拆卸，以及机器红石控制。
- 晶质铀矿、干冰生成、蓝冰补充方块、材料转换和充能雪球。
- 156 个工作台配方、14 个充能配方和粗晶质铀熔炼配方。
- 上游模型与贴图的 1.12 适配、中英文本地化、按上游贴图与坐标还原的机器界面，以及完整 28 篇中英手册。

## 开始游玩

1. 用 3 份煤、2 份黏土球和一桶熔岩合成 24 份绝缘膏；空桶会返还。
2. 一本书加一份绝缘膏，无序合成 Powah 使用手册。右键打开，用左侧目录切换章节；可滚轮阅读、点击配方物品跳转，或用 Ctrl + F 搜索。
3. 制作发电机、能量线缆和能量单元。默认一块煤产生 48,000 FE，一桶熔岩产生 100,000 FE。
4. 将铁锭和金锭分别放进充能球，从充能棒的贴附面供能；消耗 10,000 FE 得到 2 个充能钢。后续水晶沿用上游充能进阶。
5. 反应堆需要 36 个完全相同的部件和一片 3×4×3 的空地；点击底部中心自动搭建。任意部件拆除后，返还 36 个部件与内部物品，保留能量、流体和燃料状态。

手册中提供发电条件、槽位用途、冷却材料、能源端口、末影频道、绑定卡与自动化操作说明。完整充能配方见 [充能配方表](docs/ENERGIZING.md)。

## 原版机器界面

使用固定上游版本的原始 GUI 贴图、槽位和仪表坐标。能量单元有两个充电槽，发电机使用左下角的充电槽；反应堆分别显示储能、铀、碳、红石、固体冷却剂和温度。

右侧六面配置与红石图标、反应堆左侧自动模式开关、末影频道按钮均可操作。末影单元通过 Shift + 点击背包内的电池或能量单元扩容；左侧两个可见槽位用于充电。线缆按玩家点击的面打开紧凑配置界面。携带流体容器点击流体表可以注入或抽取。

布局来源、尺寸和游戏截图见 [UI 对齐记录](docs/UI.md)。

## 可选 Ponder 教程

安装 Ponder 后，悬停相关物品并按住 **W**，或点击手册中的“思索教程”链接。

| 教程 | 展示内容 |
| --- | --- |
| 发电与储存能量 | 燃料、发电机、线缆连接、储能、其他发电条件 |
| 充能合成材料 | 放入原料、充能棒供电、成品取出与循环 |
| 建造反应堆 | 分层搭建、铀燃料、冷却与能源输出位置 |
| 无线末影网络 | 扩容、所属玩家、频道和末影门供能 |
| 物品与玩家充电 | 绑定卡、跨维度升级、能量漏斗、放电器与随身电池 |

场景、NBT 结构与翻译均使用 `powah` 命名空间。兼容入口位于 `owmii.powah.compat.ponder`；客户端仅在检测到 `ponder-legacy` 后通过 IMC 注册插件。Ponder API 是编译期可选依赖，服务器不加载该客户端插件。

## 1.12 适配说明

- 通用物品标签映射为 Forge 矿物词典；青金石使用 `minecraft:dye:4`。
- 蓝冰补充为 `powah:blue_ice`，用 9 个浮冰合成；2 个蓝冰可充能为干冰。
- 贫晶质铀矿生成于 Y 4–63，普通矿 Y 4–23，富矿 Y 4–11；干冰生成于寒冷群系地下。仅影响新生成的主世界区块。
- 深板岩矿保留为可注册方块，供自定义世界使用；自然矿脉适配 1.12 的石头地形。
- 手册按上游使用的 GuideME 21.1.7 阅读布局回迁：原背景和工具栏图标、目录、历史、搜索、全宽切换、插图、等级参数表与工作台配方。渲染实现使用 1.12 API，不依赖现代 GuideME 运行库。太阳能板使用单块中心模型，玩家能量发射器使用单块模型。
- 内部能源以 `long` 保存；对外 Forge Energy 接口遵循 1.12 的 `int` 上限。
- 能量漏斗支持原版容器和 Forge `IItemHandlerModifiable`；不会修改无法安全写回物品的只读处理器。
- 普通工作台 / 熔炼配方使用标准注册表。尚未提供充能配方的专用 JEI 分类，也未回迁现代 Curios、EMI 集成；不提供现代 GuideME 的扩展 API。

该版本以 alpha 发布，大型整合包中的第三方机器、长时间高负载网络和远程多人联机未作全面组合测试。

## 配置

`config/powah.cfg`：

| 键 | 默认值 | 用途 |
| --- | --- | --- |
| `energizingRange` | 4 | 充能棒寻找充能球的距离 |
| `energyPerFuelTick` | 30 | 熔炉燃料每燃烧刻对应的 FE |
| `energizingEnergyRatio` | 1 | 充能配方耗能倍率 |
| `productionRatio` | 1 | 发电产量倍率 |
| `worldGeneration` | true | 晶质铀矿和干冰生成 |

## 构建

需要相邻的 `../ponder` 工程作为可选 API 的编译来源。首次构建需要下载 Gradle、Cleanroom 和 Minecraft 依赖。开发编译使用 `0.7.14-alpha` userdev；最低运行版本声明为 `0.4.0`，官方安装环境验证与 userdev 验证分别归档。

```bash
./tools/dev.sh build
```

脚本优先使用工作区的 `../../.dev/jdk-25.0.5+8`，也可通过 `POWAH_JAVA_HOME` 指定 Java 25；否则使用环境中的 `JAVA_HOME`。

```bash
# 日常客户端
./tools/dev.sh runClient
./tools/dev.sh runClient -PwithPonder

# 使用发布 JAR 的真实客户端 + 集成服务器验证
./tools/dev.sh runClient -PpowahSmoke -PpackagedSmoke
./tools/dev.sh runClient -PpowahSmoke -PpackagedSmoke -PwithPonder

# 使用发布 JAR 的专用服务器验证
./tools/dev.sh runServer -PpowahServerSmoke -PpackagedSmoke
./tools/dev.sh runServer -PpowahServerSmoke -PpackagedSmoke -PwithPonder
```

上述 Gradle 命令使用开发版本 Cleanroom。官方 **0.4.0** 安装包验收：

```bash
./tools/dev.sh :build :reobfDevHarnessJar :reobfServerHarnessJar :ponder:reobfJar
python3 tools/verify-cleanroom.py client
python3 tools/verify-cleanroom.py client --with-ponder
python3 tools/verify-cleanroom.py server
python3 tools/verify-cleanroom.py server --with-ponder
python3 tools/verify-cleanroom.py server --empty
```

这些运行直接使用原版游戏 JAR、官方 CR 0.4.0 和该版本运行库，记录安装包及依赖摘要，不经过 userdev 的运行时重映射。专服检查要求实际到达验证模组并写出 PASS 报告。基线目录为 `run/cleanroom-0.4.0/`。

Gradle 专用服务器验证绑定 `127.0.0.1:25597`，分别使用 `run/server-standalone/` 和 `run/server-with-ponder/`。首次运行按 CleanroomGradle 提示确认 Minecraft EULA；构建脚本不会替使用者接受协议。验证夹具仅在显式开启对应参数时安装，并在完成后正常停服；发布包不含验证代码。

资源与配方可以从固定上游重新导入：

```bash
python3 tools/import_upstream.py
```

手册数据和 UI 资源来源另见 [guide-ui-lock.json](guide-ui-lock.json)，首次导入会下载并校验固定的 GuideME 21.1.7 安装包。此命令还需要 `Pillow`、`../../upstream/Powah` 的固定提交，以及 `../../.dev/minecraft-1.21.1-client.jar` 中的对应原版材质。转换报告见 [import-report.json](docs/import-report.json)。

## 验证与许可

验证记录与截图见 [验证报告](docs/VERIFICATION.md)。分别记录官方 CR 0.4.0 基线与 0.7.14 开发环境的结果，包含核心玩法、手册、机器 UI、完整存档关闭重载、教程播放及历史专服阻塞证据。

沿用上游 **LGPL-3.0**。原作、贡献者、贴图与 Minecraft 资源归属见 [NOTICE.md](NOTICE.md) 和 [LICENSE](LICENSE)。
