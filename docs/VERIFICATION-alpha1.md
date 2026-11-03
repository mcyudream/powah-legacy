# alpha.1 验证记录

历史记录，对应 `5.1.0-cleanroom-alpha.1`；不代表 alpha.2 的 CR 0.4.0 基线。

日期：2026-11-03。目标：Minecraft 1.12.2、Cleanroom 0.7.14-alpha、OpenJDK 25.0.5。

运行使用 Powah 的重混淆发布 JAR；启用 `-PwithPonder -PpackagedSmoke` 时，Ponder 也使用本仓库生成的重混淆 JAR。图形验证环境为 Linux / Mesa llvmpipe，1280×720。

## 矩阵

| 场景 | Ponder | 结果 | 证据 |
| --- | --- | --- | --- |
| 客户端 + 集成服务器 + 完整世界关闭重载 | 未安装 | PASS | [report.json](verification/standalone/report.json) |
| 客户端 + 集成服务器 + 世界重载 + 教程播放 | 安装 | PASS | [report.json](verification/with-ponder/report.json) |
| 专用服务器启动、机器工作、正常保存停服 | 未安装 | PASS | [report.json](verification/server-standalone/report.json) |
| 专用服务器启动、机器工作、正常保存停服 | 安装 | PASS | [report.json](verification/server-with-ponder/report.json) |
| EnergyStore 单元测试 | 不依赖 | 2 项通过 | [JUnit XML](verification/EnergyStoreTest.xml) |
| 发布包隔离检查 | 不依赖 | PASS | [release.json](verification/release.json) |

Ponder 版本为 `1.4.0-cleanroom-alpha.1`，独立依赖 CraftTweaker `4.1.20.698`。测试夹具分别位于 `src/dev` 和 `src/serverDev`，不进入发布包。

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
- 专用服务器上，线缆的仅输入面阻止穿越；改成仅输出后精确传输 2,000 FE。
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
- 专用服务器分别在包含和不包含 Ponder 时进入 started 阶段，使用 CommonProxy，运行真实机器刻并正常保存停服。

## 截图

全部机器界面的对应上游布局、尺寸和截图见 [UI 对齐记录](UI.md)。

| 内容 | 图片 |
| --- | --- |
| 机器物品模型 | [models-machines.png](verification/with-ponder/screenshots/models-machines.png) |
| 材料物品模型 | [models-materials.png](verification/with-ponder/screenshots/models-materials.png) |
| 中文手册 | [guide-zh.png](verification/with-ponder/screenshots/guide-zh.png) |
| 中文机器界面 / 大 GUI 缩放 | [machine-zh-large-scale.png](verification/with-ponder/screenshots/machine-zh-large-scale.png) |
| 反应堆世界模型 | [reactor-world.png](verification/with-ponder/screenshots/reactor-world.png) |
| 反应堆界面 | [reactor-ui.png](verification/with-ponder/screenshots/reactor-ui.png) |
| 英文充能教程播放 | [ponder-playing.png](verification/with-ponder/screenshots/ponder-playing.png) |
| 中文充能教程播放 | [ponder-zh.png](verification/with-ponder/screenshots/ponder-zh.png) |
| 反应堆教程播放 | [ponder-reactor.png](verification/with-ponder/screenshots/ponder-reactor.png) |

## 测试范围与日志说明

这是发布 JAR 在 Cleanroom 开发启动器中的实测；尚未覆盖大型整合包的全部第三方容器、长期高负载和远程多人联机组合。当前没有充能配方的专用 JEI 分类。

日志保留在各验证目录的 `run.log`。Cleanroom 开发启动器会输出本地 Maven 缓存路径格式提示；Ponder 自带的六个 debug 教程会提示缺少声明 ID。部分退出过程还出现过 Ponder 在 Forge 注册表恢复期间短暂报告充能球未注册的提示；实际加载、存档重载及播放阶段的 Powah 场景诊断检查均通过。

图形、玩法和服务器结果以对应的 JSON 报告为准；网络版本检查的外部连接超时不参与玩法结果判定。
