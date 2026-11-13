# 路线图

## 已完成

### 双版本支持

| | 1.21.1 | 26.1.2 |
|---|---|---|
| Java | 21 | 25 |
| 编译验证 | ✅ 0 错误 | ✅ 0 错误 |
| 共享源码 | ✅ | ✅ |

两个版本的 `publicapi` 差异只有「26.1.2 少了联动模组相关条目」，所以共享层能用一份源码。已用真实发布 jar 做过编译验证（不是自造 stub）。

### 注册链路

| 内容 | 状态 |
|---|---|
| 机器定义（ServiceLoader） | ✅ |
| 结构 / 等级 / 修饰符 | ✅ |
| 数据配方（`mmcr:machine_recipe`） | ✅ |
| JEI workstation | ✅ |
| JEI 配方说明（client） | ✅ |
| tick 行为 / 配方行为 | ✅ |
| 控制屏文本 | ✅ |

### 工程

- GitHub Actions：矩阵编译两个版本，产物校验（解包确认 `mods.toml` 与 ServiceLoader 文件），tag 不存在时自动发 Release
- CI 里跑 `tomllib` 校验 `neoforge.mods.toml`（这个错误只在进游戏时才暴露，必须提前拦）
- `wiki.yml` 把 `wiki/*.md` 同步到 GitHub Wiki

## 已知限制

这些是设计取舍，不是 bug。

### 1. 结构不能热重载

KubeJS 版把结构放 `server_scripts` 能 `/reload`，本附属不行。原因是 MMCR 只为 KubeJS 实现了内容重载事务。

**影响**：改结构必须重启。

**变通**：用 `/mmcr build <机器ID>` 快速验证，比手动摆快。

### 2. 没有程序化配方 builder

配方只能走数据 JSON（`ServerEvents.recipes`），不能像 KubeJS 那样用 builder 对象拼。

原因：builder 需要真实 `Item` 对象，而 MMCR 的配方窗口在服务端脚本之前关闭，startup 阶段物品注册表又未就绪。

**影响**：无。数据 JSON 反而**能** `/nekojs reload`，比程序化配方更好用。

### 3. 联动模组的 requirement 类型只在 1.21.1 可用

`botania:mana`、`ars_nouveau:source`、`pneumaticcraft:air`、`mekanism:*` 这些在 26.1.2 的 MMCR 里不存在。写了会被当成未知类型。

### 4. 客户端渲染

MMCR 的自定义 Render 是 Java API 独占的能力，KubeJS 版也拿不到，本附属同样不提供。

## 可能的方向

按价值排序，都不保证实现：

1. **结构热重载** —— 需要 MMCR 侧开放一个 NekoJS 也能用的内容重载钩子。依赖上游。
2. **程序化配方** —— 需要延迟到 `ModifyDefaultComponentsEvent` 再解析 Item。技术上可行，但收益低于数据 JSON。
3. **TypeScript 类型声明** —— NekoJS 已经会生成 `.neko_probe/*.d.ts`，给 `MMCR` / `MMCREvents` 补上 JSDoc，让补全带注释。
4. **更多示例** —— 把 MMCR 官方 wiki 的 11 个示例机器全部翻译。

## 参与

- 仓库：<https://github.com/YangShiDanny/NekoModularMachinery>
- 提交前跑一遍 CI（推 PR 会自动编译两个版本）
- 共享代码里**不能出现** `ResourceLocation` 或 `Identifier` 字面量，一律用 `var` 推断或走 `Ids.parse()`。这是两个版本共用一份源码的关键。
