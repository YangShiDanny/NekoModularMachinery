# 路线图

当前实现覆盖了 MMCR KubeJS 集成的**核心部分**：机器、结构、等级、修饰符、配方、JEI。
下面这些还没做，按实用性排序。

---

## 想要但没有的

### 1. 网络请求 / 智能接口运行时值

KubeJS 版的 `MMCR.getAPI().networkInterfaces`、`sendRequest`、`smartInterfaceInput`
这类需要**运行时访问机器状态**的 API。

**为什么没做**：这些方法签名依赖 MMCR 内部的网络包与运行时上下文，不是纯数据构造，
在 scripting 层暴露需要额外的生命周期钩子。

**现在怎么办**：写 Java 扩展，或者用 `recipeBehavior` 的钩子间接实现。

### 2. `DataValue`

KubeJS 的 `DataValue` 是可变数值容器。MMCR 的 publicapi 有对应能力，
但 NekoJS 侧没有对等的可变类型绑定。

**现在怎么办**：用 `readableNumber` 系列做只读展示。

### 3. 化学品 / 魔法类快捷工厂

`chemicalTagInput`、`manaInput`、`sourceInput`、`stressInput`、`airInput` 这类。

**为什么没做**：这些依赖具体联动模组（Mekanism / Botania / Ars Nouveau / Create / PneumaticCraft），
而且 **26.1.2 上 MMCR 直接把对应 API 砍掉了**（1.21.1 有 10 个端口族，26.1.2 只剩 6 个）。

**现在怎么办**：用 `event.custom({...})` 手写 JSON，或者用 `api.fluidInputRequirement` 的写法类推。

### 4. 控制器渲染器扩展

MMCR 的 `RegisterControllerRenderersEvent`，允许自定义控制器外观渲染。

**现在怎么办**：只能改纹理（`controllerTextures` 系列）。

### 5. KubeJS 长名别名

`anyOfItemInput()`、`anyOfItemPorts()` 这类。

**为什么没做**：本附属直接用短名（`itemInput()`、`itemPorts()`），已经够用。

**现在怎么办**：用短名。

### 6. `readableNumberBigInt` / `readableNumberForSlot`

**现在怎么办**：用 `readableNumberExact()`，输出 `1,234,567` 这种。

---

## 上游依赖的坑

### 结构不能热重载

MMCR 只在 `FMLCommonSetupEvent` 收集一次结构。KubeJS 能"在 server 阶段声明结构"是因为
MMCR 有专门的延迟收集分支，NekoJS 没有。

**能不能修**：需要 MMCR 上游放开（比如把那个 `isLoaded("kubejs")` 判断泛化）。
如果你认识 MMCR 作者，可以提一下。

### 26.1.2 少了 4 个端口族

`stress` / `source` / `mana` / `air` 在 26.1.2 的 MMCR 里不存在。本附属会自动跳过并打 warn。

---

## 想加功能？

代码在 [`common/src/main/java/cn/howxu/mmcr/nekojs/`](https://github.com/YangShiDanny/NekoModularMachinery/tree/main/common/src/main/java/cn/howxu/mmcr/nekojs)。

| 想加什么 | 改哪个文件 |
|---|---|
| 新的 `api.xxx()` 门面方法 | `MMCRApiJS.java` |
| 新的 builder 方法 | `builder/*.java`，然后接到 `DeclarationStore` |
| 新的脚本事件 | `MMCRNekoEvents.java` + `event/*.java` |
| 新的配方字段 | `recipe/MachineRecipeSchema.java` |
| 版本差异处理 | `version/VersionSupport.java` + `versions/*/VersionSupportImpl.java` |

**注意双版本约束**：共享代码里**不能出现** `ResourceLocation` 或 `Identifier` 字面量，
一律用 `var` 推断，或者走 `Ids.parse()`。这是两个 MC 版本能共用一份源码的关键。

提交前跑一遍 CI（推 PR 就会自动编译两个版本）。
