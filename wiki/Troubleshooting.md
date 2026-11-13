# 常见错误

按症状查。先看日志——`logs/latest.log` 里搜 `mmcr_nekojs`。

---

## 启动后日志里没有 `MMCR/NekoJS submitted ...`

说明脚本根本没执行到。

| 检查项 | 怎么查 |
|---|---|
| NekoJS 装了吗 | 日志里搜 `nekojs`；`nekojs/` 目录存在吗 |
| 脚本放对目录了吗 | 必须 `nekojs/startup_scripts/`（注意是 `nekojs` 不是 `kubejs`） |
| 脚本有语法错误吗 | NekoJS 会在日志里报解析错误，搜 `SyntaxError` / `error` |
| 真的声明了东西吗 | 空文件当然什么都不提交 |
| 是不是用了 `MMCREvents.server` | 那不会产生这两行 |

**两个 jar 都装了吗**：MMCR、NekoJS、本附属，缺一个都不会注册。

---

## 机器不出现在 JEI / 拿不到控制器

| 原因 | 解决 |
|---|---|
| 没调 `.register()` | 每个 builder 最后都要 `register()`（结构是 `build()`） |
| `displayNameKey` 没配语言条目 | 显示成原始 key，搜不到。加 `assets/<ns>/lang/zh_cn.json` |
| `addRecipePoolWorkstation` 没写 | JEI 不知道哪个物品代表这个机器 |
| 机器 ID 和配方池 ID 搞混 | 它们是两个东西，池默认等于机器 ID，但可以不同 |

---

## 结构永远拼不上

**先检查字符串长度。** 这是最常见的原因。

```js
.pattern(
  'BBB',
  'BCB',
  'BB'      // ← 少一个字符，整个结构错位
)
```

同一次 `pattern()` 里所有字符串长度必须一致。

其他可能：

| 原因 | 解决 |
|---|---|
| `pattern()` 调用次数理解错了 | 一次调用 = 一个 z 层（水平层），不是一回事 |
| 控制器符号没定义 | `.controller('C')` 但 `set('C', ...)` 没写 |
| 空格被当成"要求空气" | 空格是"跳过不检查"，要空气用 `api.air()` |
| 方块状态不对 | 熔炉 `[lit=true]` 和 `[lit=false]` 不同，用 `stateInsensitive()` |
| 写在了 `server_scripts` | 挪到 `startup_scripts`，重启 |

**调试技巧**：先用最小结构（3×1×3 地板，全用一种方块）验证能不能成型，再加复杂度。

---

## 成型失败，提示缺端口

`portRequirements` 是**校验**，不是"自动生成端口"。

```js
.portRequirements({ item_input_bus: 1 })   // 意思是"成型时必须至少有 1 个"
```

解决：在结构表面手动装对应端口方块，或者放宽/删掉这条要求。

---

## 配方不出现，但也不报错

MMCR 对坏配方是**静默丢弃**的。

| 原因 | 排查 |
|---|---|
| 流体输入写成裸字符串 | `fluid:'minecraft:water'` ❌ → `fluid:{fluid:'minecraft:water'}` ✅ |
| `recipe_pool` 拼错 | 和机器的 `recipePool()` 逐字符对 |
| 用了旧字段名 | `machine` / `inputs` / `fluid_outputs` 都已被拒绝，换成 `recipe_pool` / `requirements` |
| `tick_time` 是 0 或负数 | 必须 ≥ 1 |
| 写进了 `startup_scripts` | `ServerEvents.recipes` 在 startup 阶段不触发 |

**调试技巧**：先用原生 JSON（写法 B）跑通一条，确认池名对了，再改成 schema。

---

## 游戏崩溃 / `NoClassDefFoundError`

| 现象 | 原因 |
|---|---|
| `cn.howxu.mmcr.publicapi...` 找不到 | MMCR 没装，或版本低于 0.1.7 |
| `com.tkisor.nekojs...` 找不到 | NekoJS 没装 |
| `net.minecraft.resources.Identifier` / `ResourceLocation` 找不到 | jar 和 MC 版本不匹配 |
| 两个脚本引擎打架 | 卸掉 KubeJS / Rhino / NekuJS |

**版本一定要对上**：

```
MC 1.21.1  →  mmcr_nekojs-1.21.1-*.jar  →  Java 21
MC 26.1.2  →  mmcr_nekojs-26.1.2-*.jar  →  Java 25
```

装错版本 `mods.toml` 会拒绝加载，日志里写得很清楚。

---

## 修饰符装了没反应

| 检查 | 说明 |
|---|---|
| 机器有 `.allowModifiers()` 吗 | 没有这个开关，修饰符槽不工作 |
| 结构那格是 `modifierSlot(...)` 吗 | 普通方块格装不了 |
| `registerModifierItem` 的两个参数都是字符串吗 | 传 `Item.of()` 会失效 |
| `modifier()` 的 target/scope 合法吗 | 布尔修饰符只支持 `('parallelized','recipe',bool)` |

---

## `tickBehavior` / `recipeBehavior` 报类型错误

这两个钩子需要 NekoJS 把 JS 函数转成 Java `Consumer`。如果你的 NekoJS 版本不支持
宿主 `@FunctionalInterface` 桥接，会在这里抛类型错误。

临时方案：去掉钩子，改用配方层面的机制；或者自己写 Java 扩展。

---

## `/nekojs reload` 后结构没更新

**正常现象。** 结构是启动期注册的，改不了。

只有 `server_scripts` 里的**数据驱动配方**支持热重载。机器、结构、等级、修饰符都需要重启。

---

## 还是不行

开 issue 时带上这些信息，能省很多来回：

1. Minecraft / NeoForge / MMCR / NekoJS 版本
2. 完整的 `logs/latest.log`（或者至少 grep `mmcr_nekojs` 的部分）
3. 你的脚本文件
4. 有没有装 KubeJS / Rhino

[开 issue](https://github.com/YangShiDanny/NekoModularMachinery/issues)
