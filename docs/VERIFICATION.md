# 验证索引

## 当前发行版

版本：`5.1.0-cleanroom-alpha.2`。基线：Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（不带 -alpha）** / Java 25。

生产包 SHA-256、四种组合的报告和准确依赖摘要见 [正式基线验收](../LEGACY-VERIFICATION.md)。最终发行包证据位于 [releases/5.1.0-cleanroom-alpha.2](verification/releases/5.1.0-cleanroom-alpha.2)。

- 有 / 无 Ponder 的真实客户端：各 218 条检查，覆盖机器、模型、配方、UI、双语手册和世界重载；安装 Ponder 时包含场景播放。
- 有 / 无 Ponder 的专用服务器：各 8 条检查，运行机器刻、确认服务端代理及正常保存停服。
- 2 项 EnergyStore 单元测试；553 个双语语言键、145 个内容名称和 28 篇双语手册的静态检查。
- 发布 CI 核对 loader ID、作者、图标、正式基线、可选依赖隔离、发行标签及被验收 JAR 的 SHA-256。

兼容修复仅作用于正式 CR 0.4.0 的物理专服 Kirino 发现路径，使用官方加载器安装包。客户端验证环境是 Linux / Mesa llvmpipe、1280 × 720 和空音频后端，未验收声音输出。没有覆盖任意整合包、所有第三方机器或长期高负载网络。

## 重现

按 [README](../README.md#本地运行与验收) 构建生产包与独立测试夹具，执行 `tools/verify-cleanroom.py`。通过 `LEGACY_PONDER_JAR` 指定实际的 Ponder 发行 JAR，准确哈希记录在各组合的 `runtime.json`。验证程序要求 JVM 正常退出并写出 PASS 报告；仅启动进程不算通过。

原始报告包含测试夹具但不将其装入发行包。界面来源、尺寸和截图见 [UI 对齐记录](UI.md)。

## 历史记录

- [Legacy 身份迁移后的首轮正式 0.4.0 验收](verification/legacy-identity/cleanroom-0.4.0-final)：旧生产包和旧 Ponder 摘要保留，当前发布以最终发行包记录为准。
- [迁移早期 alpha.2 记录](VERIFICATION-pre-legacy.md)：含旧身份、0.4.0-alpha 客户端和当时的专服发现阻塞。旧文中的简称及结论不用于当前正式 0.4.0 的验收。
- [alpha.1 记录](VERIFICATION-alpha1.md)。
