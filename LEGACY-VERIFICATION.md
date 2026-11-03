# Powah Legacy 正式基线验收

- 日期：2026-11-03。
- 环境：Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25。
- 生产安装包：`powah-legacy-5.1.0-cleanroom-alpha.2.jar`。
- SHA-256：`fbd35b090fdb12f7b207ce74c28510ab9f7073ce6f90ade8b2f8ff6e3ca4e22a`。
- 作者、loader ID、展示名与打包图标另由 `tools/verify_legacy_package.py` 检查。

| 组合 | 结果 | 断言数 | 证据 |
| --- | --- | --- | --- |
| client-without-ponder | PASS | 218 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/client-without-ponder/report.json) |
| client-with-ponder | PASS | 218 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/client-with-ponder/report.json) |
| server-without-ponder | PASS | 8 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/server-without-ponder/report.json) |
| server-with-ponder | PASS | 8 | [报告](docs/verification/legacy-identity/cleanroom-0.4.0-final/server-with-ponder/report.json) |

## 覆盖范围

107 个方块及 38 个独立物品、156 个合成配方、各类发电机、反应堆、储能与线缆、电门/末影网络/玩家充电、真实 GUI 控件、英文/中文手册、模型渲染和存档；可选 Ponder 播放与两种专服组合。

每份 runtime.json 记录该组合实际安装的 jar 和哈希，包括可选 Ponder、CraftTweaker 或 JEI。验收使用官方安装器的生产运行库与 SRG 安装包，不将 userdev / 其他 Cleanroom 版本作为本基线的运行证据。

正式 0.4.0 专服的 Kirino 发现兼容修复仅在该版本的物理专服生效，不改动官方加载器 jar。客户端图像检查使用 Linux llvmpipe 和空音频后端；未覆盖全部硬件及任意整合包组合。功能差异与未实现项目仍以 README 为准。
