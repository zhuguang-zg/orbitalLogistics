# orbitalLogistics 开发规划

> 目标版本：Mindustry **158.1**（与本地 `Mindustry-158.1源码` 及游戏目录 `D:\MindustryX158` 一致）
> 本文档是设计与技术结论的汇总。每条结论都标注了源码位置，方便随时回去核对。

---

## 一、玩法目标

**仅战役模式生效。** 每个"可见且可降落"的星球生成一个环绕其轨道的空间站。玩家通过发射台把物资送上空间站，再从空间站分发到同星球的任意区块。

```
地表: 发射台 (30s / 满100物品) ──纯动画──▶ 空间站: 接收台 ──▶ 生成上升舱载荷 ──▶ 拆解入库
空间站: 返回台 (40s / 从库存取100) ──纯动画──▶ 地表: 接收台 ──▶ 生成返回舱载荷 ──▶ 拆解入库
```

**物品分配模型**：每个地表接收台单独设置"每分钟接受数量"（100 的倍数），所有接收台配额之和受空间站"发射速度"约束。超配时全盘禁用（详见第七节）。

---

## 二、阶段划分

每个阶段**以验证一个高风险假设开头**，而不是先堆代码。四个高风险假设已在下表标注，它们必须在被依赖之前实测通过。

| 阶段 | 内容 | 验证的高风险假设 |
|---|---|---|
| 0 | 工程准备：版本对齐、改名、清理模板 | — |
| 1 | 空间站地板 + 拆除方块 | ⚠️ **地板不占方块层，机器能建在上面** |
| 2 | 最小空间站星球 + 固定地图 | ⚠️ **`updateGroup` 让空间站参与后台模拟** |
| 3 | 上升舱 / 返回舱 `UnitType` | ⚠️ **`isBanned()` 让载荷无法被 dump** |
| 4 | 构筑器 / 解构器 / 载荷装载器 | 原版解构器能否接受自定义 `UnitType` |
| 5 | 发射台 + 空间站接收台（上行链路） | — |
| 6 | 返回台 + 地表接收台（下行链路） | — |
| 7 | 配额系统 + 联锁 + 后台结算 | ⚠️ **后台区块配额可读** |
| 8 | 折叠太阳能板 | — |
| 9 | 打磨：贴图、平衡、本地化、多人 | — |

---

## 阶段 0：工程准备

**要改的**

- `build.gradle:11` —— `mindustryVersion = "v160"` → `"v158.1"`
  （依赖已在 Gradle 缓存中：`~/.gradle/caches/modules-2/files-2.1/Anuken/Mindustry/v158.1/Mindustry-v158.1.jar`，改完不需要重新联网）
- `mod.hjson` —— 五处模板值全部替换：
  - `displayName` → 你的模组名
  - `name` → `orbital-logistics`（**内部名，会被用作所有内容与贴图的前缀**，见第八节）
  - `main` → 你的主类全限定名
  - `description`、`author`
  - `minGameVersion: 160.3` → `158.1`
- 删除 `src/example/` 和 `assets/sprites/frog.png`

**包结构**（沿用 zgmod 的大写短包名风格：`ZG.Items`、`ZG.YueyunFox`、`ZG.can.CanLoad`、`ZG.planets.Planets`）

每个功能域一个子包，子包里放一个 `*Load` 类负责该域的内容注册 —— 这是 zgmod 已经在用的组织方式。

```
src/OL/
  ZG.OrbitalLogistics.java        主类, extends Mod
  Blocks.java                  全局方块注册入口
  Units.java                   两种载荷注册
  ZG.Block/
    BlockLoad.java             方块域注册
    SpaceStationFloor.java     阶段 1
    FloorRemover.java          阶段 1
    AscentLaunchPad.java       阶段 5
    AscentReceiver.java        阶段 5
    ReturnPad.java             阶段 6
    DescentReceiver.java       阶段 6
    FoldedSolarPanel.java      阶段 8
    CapsuleConstructor.java    阶段 4
    CapsuleLoader.java         阶段 4
    CapsuleDeconstructor.java  阶段 4
  type/
    AscentCapsule.java         阶段 3
    DescentCapsule.java        阶段 3
  station/
    StationLoad.java           阶段 2  空间站星球生成
    Quota.java                 阶段 7  配额快照数据结构
    QuotaManager.java          阶段 7  配额汇总、联锁、后台结算
```

> 包名要写全小写的话也行（Java 惯例），但既然要和 zgmod 统一，就按上面这套走。关键是 `mod.hjson` 的 `main` 要指向 `OL.OrbitalLogistics`。

**验收**：`gradlew jar` 成功，产物 `build/libs/orbitalLogisticsDesktop.jar` 能被游戏加载且无报错。

**部署**：复用 `D:\JavaZG\build-and-run.ps1`，参数化调用：
```
.\build-and-run.ps1 -ModDir D:\JavaZG\orbitalLogistics -DeployFileName orbitalLogisticsDesktop.zip
```
（脚本会优先找 `build\libs\zgmodDesktop.jar`，找不到则回退到该目录下最新的 `*Desktop.jar`）

> ⚠️ 你的 `%APPDATA%\Mindustry\mods` 里现在堆着 `FakePlayerModDesktop.zip` 到 `...7.zip`、`mymodDesktop.zip`、`zgmodDesktop.zip`、`假人模组.jar` 及其 `.bak`。调试本模组时建议只留正在调试的那一份，其它挪走——同源多份副本是"我的改动怎么没生效"最常见的原因。

---

## 阶段 1：空间站地板 + 拆除方块

### 1.1 SpaceStationFloor

**必须继承 `Floor`，不能继承 `Block`。** 方块层一个格子只能有一个方块，如果地板是 `Block`，机器就永远建不上去了。

`Floor` 的构造器（`Floor.java:104-117`）已经预设好我们需要的性质：

```java
placeableLiquid = true;
allowRectanglePlacement = true;   // 玩家能拖拽矩形批量铺
instantBuild = true;              // 瞬间建成
ignoreBuildDarkness = true;       // 能铺在 Blocks.space 上
obstructsLight = false;
```

还需要显式设置两件事：

- `buildVisibility = BuildVisibility.shown`（`Block` 默认是 `hidden`，`Block.java:356`）
- `category`（否则不进建造菜单）

造价：5 铜。

**构造器选择**：`Floor(String)` 默认 `variants = 3`（`Floor.java:104-106`），需要 `space-station-floor1/2/3.png` 三张贴图。想用单贴图就用 `Floor(String, int)` 传 `0`（`Blocks.space` 就是 `variants = 0`）。

### 1.2 FloorRemover（拆除方块）

`Block`，1×1，`update = true`。放在空间站地板上，2 秒后：

- 判定 `tile.floor() == spaceStationFloor`
- 成立则 `tile.setFloor(Blocks.space)`（`Tile.java:300`），然后自毁

因为改的是**地板层**，`tile.setFloor` 不碰方块层，**不需要处理地板上的机器**。每次拆一格，用完自毁。

### ✅ 验收（本阶段最关键）

| 验收点 | 依赖机制 |
|---|---|
| 能在 `Blocks.space` 上铺设 | `Floor` 走独立判定路径（`Build.java:223-226`）+ `ignoreBuildDarkness` |
| **铺完后不占方块层，机器能建在同一格** | `ConstructBlock.constructFinish` 对 floor 只调 `tile.setFloor`（`ConstructBlock.java:78-87`） |
| 能拖拽矩形批量铺 | `allowRectanglePlacement` |

**测试方法**：编辑器里铺一片 `Blocks.space`，把地板块放上去，再在同一格放一台机器。

> 🔴 **第二点是整个空间站玩法的地基。** 如果它不成立，后面所有建筑都建不起来，整个架构要重新设计。先花十分钟验证它，不要跳过。

---

## 阶段 2：最小空间站星球 + 固定地图

### 2.1 空间站星球

照抄原版 `Planets.makeAsteroid()`（`Planets.java:171-209`）的模式 —— 那是原版 Erekir 太空小行星的做法，正好是我们的场景：

```java
new Planet(name, parent, 0.12f){{
    hasAtmosphere = false;
    updateLighting = false;
    sectors.add(new Sector(this, Ptile.empty));
    camRadius = 0.68f * scale;
    minZoom = 0.6f;
    drawOrbit = false;
    accessible = false;
    clipRadius = 2f;
    defaultEnv = Env.space;      // ← 关键：整张图的环境 = 太空
    icon = "commandRally";
    generator = new AsteroidGenerator();
}}
```

`Planet(name, parent, radius)` 构造器会自动算 `orbitRadius`（`Planet.java:202`）、`orbitTime`（开普勒第三定律，`:205`），并把自己加进 `parent.children`（`:209`）——**轨道运动是白送的**。

**`defaultEnv = Env.space` 是"只能放在太空上"的实现基础**：`World.java:327` 会执行 `state.rules.env = sector.planet.defaultEnv`，于是方块的 `envRequired = Env.space`（`Block.java:272`）就生效了，判定在 `Block.supportsEnv()`（`:1015`）。

只给"可见且可降落"的母星生成空间站（遍历 `content.planets()` 过滤 `visible && isLandable()`）。

### 2.2 加入 updateGroup（⚠️ 高风险假设）

`Universe.runTurn()` 的后台模拟循环开头（`Universe.java:158-163`）：

```java
for(Planet planet : content.planets()){
    //do not update other planets
    if(current != null && current != planet && !current.updateGroup.contains(planet) && !planet.updateGroup.contains(current)){
        continue;
    }
```

`Planet.updateGroup` 是 **`public ObjectSet<Planet>`**（`Planet.java:171`），可写。**把空间站星球加进母星的 `updateGroup`，空间站就会参与后台模拟。** 判定是双向的（`||`），加一边就够。

参与后会获得原版免费提供的库存累积（`Universe.java:213`）：

```java
sector.info.production.each((item, stat) -> sector.info.items.add(item,
    Math.min((int)(stat.mean * newSecondsPassed), sector.info.storageCapacity - sector.info.items.get(item))));
```

即"速率 × 经过秒数 → 入库，按 `storageCapacity` 封顶"。

`runTurn()` 结尾 `:264` 有 `Events.fire(new TurnEvent())` —— **这是写后台配额结算逻辑的钩子**，每回合一次，且在原版第三趟之后，顺序正好。

### 2.3 固定地图

- 地图放 **`orbitalLogistics/assets/maps/station.msav`**
  （`build.gradle:82-84` 会把 `assets/` 内容平铺到 jar 根目录，所以 jar 里就是 `maps/station.msav`）
- `new SectorPreset("station", stationPlanet, 0)` —— **第三个参数是 sector 索引，`fileName` 绝对不要传**（原因见第八节）
- **地图里必须有核心**，否则 `FileMapGenerator.generate()` 在 `:143-145` 抛 `IllegalArgumentException("All maps must have a core.")`
- 地图地面铺满 `Blocks.space`，玩家再自己铺空间站地板

**"类与实例"模型**：每个空间站建一个独立的 `SectorPreset` 实例、`Planet`、`Sector`，这样独立存档互不干扰；全部指向同一个 `station.msav`，所以初始状态相同。

### ✅ 验收

- 固定地图能加载，核心存在
- 能进出空间站且存档正常
- 空间站方块上 `envRequired = Env.space` 生效（在地表星球上应该放不了）

---

## 阶段 3：上升舱 / 返回舱 UnitType

### 3.1 基础数值

| 属性 | 上升舱 | 返回舱 |
|---|---|---|
| 生命值 | 900 | 3600 |
| 护甲 | 150 | 1300 |
| 尺寸 | hitSize **24**（3.0 格） | 同 |
| 物品容量 | 100 | 100 |
| 空中单位 | 是 | 是 |
| 移动速度 / 转向 | 0 / 0 | 0 / 0 |

**尺寸必须是 3.0 格（hitSize 24），不能用你原本写的 3.5。** 原因：

`UnitPayload.size()` 返回 `unit.hitSize`（`UnitPayload.java:99-100`），`Payload.fits(s)` 是 `size() / tilesize <= s`（`Payload.java:50-52`），tilesize = 8。3.5 格 = hitSize 28，而原版载荷方块的上限是 3：

| 方块 | 字段 | 值 | 3.5 格 |
|---|---|---|---|
| `PayloadConveyor` | `payloadLimit`（`:25`） | 3f | ❌ |
| `PayloadRouter` | 继承 `PayloadConveyor` | 3f | ❌ |
| `PayloadMassDriver` | `maxPayloadSize`（`:30`） | 3 | ❌ |
| `PayloadLoader` / `PayloadUnloader` | `maxBlockSize`（`:25`） | 3 | ❌ |
| `PayloadDeconstructor` | `maxPayloadSize`（`:18`） | 4 | ✅ |

你要"都能搬运"，所以取 hitSize 24（`24 / 8 = 3 <= 3` ✅）。

### 3.2 让载荷无法被 dump（⚠️ 高风险假设）

**重写 `isBanned()` 恒返回 true。** 链条：

1. `UnitType.isBanned()`（`UnitType.java:757-760`）→ `state.rules.isBanned(this)`，**不是 final，可以重写**
2. `Units.canCreate`（`Units.java:115-117`）→ `!type.useUnitCap || (countType < cap && !type.isBanned())` → 因为 `isBanned()` 为 true 而返回 false
3. `UnitPayload.dump()`（`UnitPayload.java:104-141`）第一道关卡 `:108`：
   ```java
   if(!Units.canCreate(unit.team, unit.type)){ overlayTime = 1f; overlayRegion = null; return false; }
   ```
   **直接失败，永远不会走到 `:135` 的 `unit.add()`**

而 `UnitType.create(Team)`（`UnitType.java:553-561`）只是 `constructor.get()` + `setType(this)`，**不检查 `canCreate`** —— 所以构筑器照样能造出载荷。

**副作用清单**（全源码 13 处 `.isBanned()` 调用点已逐一核查，全部落在期望方向）：

| 调用点 | 影响 |
|---|---|
| `Units.canCreate` | ← 目标 |
| `UnitFactory` / `UnitAssembler` / `Reconstructor` | 禁止产出它 ✅ |
| `PayloadSource` / `PayloadRouter` 的 `canProduce` | 禁止载荷源把它当选项 ✅ |
| `LExecutor.java:1606`（逻辑 unit 生成） | 禁止逻辑刷出它 ✅ |
| `DatabaseDialog.java:182` | 核心数据库显示"已禁用"标记（纯视觉） |

### 3.3 自毁保底

把 `envEnabled` 设为不支持任何环境。`UnitComp.update()`（`UnitComp.java:745-749`）：

```java
if(!type.supportsEnv(state.rules.env) && !dead){
    Call.unitEnvDeath(self());
    team.data().updateCount(type, -1);
}
```

任何被调试器强行放出来的自由单位都会在一个 tick 内环境死亡。

**不会误伤载荷状态**：`UnitPayload.update()`（`UnitPayload.java:50-53`）调的是 `unit.type.updatePayload(...)`，**不是** `unit.update()`，所以 `UnitComp` 的环境检查在载荷状态下根本不跑。两条路径是分开的。

### 3.4 建造花费（`getTotalRequirements`）

`UnitType.getRequirements()`（`UnitType.java:1349-1371`）只从三处取造价：`Reconstructor` 升级链、`UnitFactory.plans`、`UnitAssembler.plans`。三者都没有 → `getTotalRequirements()` 返回 `ItemStack.empty`（`:1329`）。

**这会导致两个问题**：
1. 解构器拒绝它 —— `PayloadDeconstructor.acceptUnitPayload`（`:92-95`）要求 `unit.type.getTotalRequirements().length > 0`
2. 你写的"建造花费"没有结算依据

**解法**：override `getTotalRequirements()`（public，非 final）返回固定造价：

- 上升舱：铜 50 / 铅 50 / 钛 10
- 返回舱：铜 70 / 铅 70 / 钛 20

⚠️ 副作用：原方法会顺带累加 `buildTime`（`:1341-1343`），override 后这段被跳过 → **要自己设 `buildTime`**。

### 3.5 其他

同一条判定里还有 `!unit.spawnedByCore`（`PayloadDeconstructor.java:93`）。`UnitType.create(team)` 不设这个标志（`PayloadSource.java:137` 用的就是这条路），所以应该能过，但**要实测确认**。

### ✅ 验收

| 验收点 | 期望 |
|---|---|
| 能被原版载荷传送带搬运 | 能放上去 |
| 试图 dump | 失败，载荷留在原地 |
| 用调试器强行放出来 | 一个 tick 内环境死亡 |
| 丢进原版解构器 | 被接受并拆出材料 |

---

## 阶段 4：构筑器 / 解构器 / 载荷装载器

### 4.1 构筑器 —— 照 `PayloadSource` 抄，不要照 `BlockProducer`

`PayloadSource.updateTile`（`PayloadSource.java:132-146`）就是"怎么造单位载荷"的答案：

```java
payload = new UnitPayload(unit.create(team));   // :137
```

而且它已经具备全部需要的机制：

- `config(UnitType.class, ...)`（`:54-61`）—— config 可以直接用 `UnitType`（`UnitType` 是 `Content`，`TypeIO.writeObject` 的 type 5 支持，`TypeIO.java:57-60`）
- `canProduce(UnitType)`（`:93-94`）—— 现成的白名单过滤钩子
- `getPlanConfigs`（`:71-75`）—— 让玩家在 UI 里挑产物的现成写法

**做法**：`PayloadSource` 派生 → 去掉方块分支 → `canProduce` 只放行两种载荷 → 加上物品消耗（照 `BlockProducer` 的 `ConsumeItemDynamic`，`BlockProducer.java:39-55`，或直接读你 override 的 `getTotalRequirements()`）。

> 为什么不照 `BlockProducer`：它的配方体系完全围绕 `Block` 建 —— 配方键是 `Block`（`:37, :43-50`），产出写死 `new BuildPayload(recipe, team)`（`:117`），没有单位分支。

### 4.2 载荷装载器 —— 必须新写

原版 `PayloadLoaderBuild extends PayloadBlockBuild<BuildPayload>`（`PayloadLoader.java:86`），内部逻辑用 `payload.block().hasItems`（`:155`）—— 只对**方块载荷**有效。`PayloadUnloader` 继承它，同样不行。

### 4.3 解构器 —— 也要新写（继承原版改数值）

原版 `PayloadDeconstructorBuild extends PayloadBlockBuild<Payload>`（`PayloadDeconstructor.java:51`，泛型是 `Payload` 不是 `BuildPayload`），且有 `acceptUnitPayload(Unit)`（`:92`）—— **机制上支持单位载荷**。但你要限制只接受这两种载荷，所以继承它并重写接受判定。

**白名单是必需的**：`acceptUnitPayload` 只要求 `getTotalRequirements().length > 0`，而**任何在某个 `UnitFactory.plans` 里的原版单位都满足**。没有白名单的话，玩家可以把原版单位丢进去换材料。

### ✅ 验收

造舱 → 装物品 → 搬到接收台，全程无异常；把原版单位丢进解构器应该被拒绝。

---

## 阶段 5：发射台 + 空间站接收台（上行链路）

### 5.1 发射台（地表）

派生自 `LaunchPad`。你的数值基本就是原版 `advancedLaunchPad`（`Blocks.java:6813-6823`）的复刻：

| 你的设计 | 原版对应 |
|---|---|
| 4×4 / 物品100 / 30s / 石油 9/s / 液体50 | `size 4, itemCapacity 100, launchTime 60f*30, consumeLiquid(oil, 9f/60f), liquidCapacity 40` |
| 消耗电力 480 | `consumePower(8f)` ← **480 = 8 × 60** |
| 造价 铜350 铅300 钛200 硅250 | 原版同数值，仅顺序不同 |

**单位陷阱**：Mindustry 内部电力按"每 tick"算，UI 显示时才 ×60。写 `consumePower(480f)` 会变成 28800/s。液体同理 `9f/60f`，计时同理 30 秒 = `60f*30`。

**"一个载荷只装一种物品"原版已实现**：`LaunchPad.acceptItem()`（`LaunchPad.java:138-140`）+ `acceptMultipleItems = false`（默认，`:40`）。直接继承就有。

**发射是纯视觉的**：原版 `updateTile()`（`:143-161`）只是把物品塞进 `LaunchPayload` 特效实体，真正入账在 `remove()`（`:289-311`）。你的设计是"纯动画 + 接收后生成载荷"，正好对上。

### 5.2 空间站接收台

派生自 `PayloadBlock`，内部类 `PayloadBlockBuild<UnitPayload>`。5×5，电力 `9f`，冷却 10s，`envRequired = Env.space`。

生成载荷 = `new UnitPayload(unit.create(team))`（照 `PayloadSource.java:137`）。

⚠️ **要处理 `PayloadBlockBuild.onRemoved()`**（`PayloadBlock.java:152-155`）：方块被拆/摧毁时会执行 `payload.dump()`。虽然 `isBanned()` 已经让它必然失败，但你的接收台应该显式重写以控制行为（比如把载荷里的物品倒出来）。

**冷却时间换算**：原版 `LandingPad.setStats()`（`:122`）显示的是 `(cooldownTime + arrivalDuration) / 60f`。所以"冷却 5 秒" = `cooldownTime 150f + arrivalDuration 150f`；"10 秒" = 两者各 300f。

### ✅ 验收

同一星球内，地表发射台 → 空间站接收台，能跑通整条链路。

---

## 阶段 6：返回台 + 地表接收台（下行链路）

- **返回台**（空间站）：发射台派生，5×5，物品100，40s，电力 `10f`，`envRequired = Env.space`
- **地表接收台**：`PayloadBlock` 派生，4×4，电力 `5f`，冷却 5s，**config = `Point2(itemId, rateIndex)`**

**config 为什么用 `Point2`**：原版 `LandingPad` 的 config 是单个 `Item`（`LandingPad.java:73-78`），装不下"物品 + 速率"两个值；一个方块只能有一个 config。而 `TypeIO.writeObject`（`TypeIO.java:42-111`）支持的序列化类型是固定的，其中 `Point2` 正好合用（`:68-71`，两个 `write.i`）：

```java
config = new Point2(item.id, rateIndex)     // rate = (rateIndex + 1) * 100
```

这样自定义 config 不需要碰任何序列化代码，联机和存档都自动可用。

**命名冲突提醒**：你有两个"接收台"（空间站 5×5 / 地表 4×4）。内部 ID 必须不同，建议 `ascent-receiver` / `descent-receiver`。

### ✅ 验收

空间站 → 地表跑通，形成完整闭环。

---

## 阶段 7：配额系统 + 联锁 + 后台结算

### 7.1 配额与发射速度

- **每个接收台单独设置**每分钟接受数量，以 100 为步长
  （建议写成 `block.itemCapacity` 派生，而不是硬编码 100）
- **发射速度 = 已建成的发射台数量 × 单台速率**
  参考 `advancedLaunchPad`：`itemCapacity 100 / launchTime 60f*30` → 200/分钟/台
- **约束**：所有接收台配额之和 ≤ 发射速度
- **不匹配时**：修建接收台时，UI 最大可选值 = `发射速度 - 其他所有配额之和`

### 7.2 联锁行为

超配（接收速度 > 发射速度）时：

- 所有发射台、接收台显示**红色文字**，带上具体的发送/接收数值
- **所有接收台禁用**，无法接收物品
- 配额**只可调低，不可调高**（保证玩家能自救）

**发射速度的电力语义**（已确认）：**仅当所有发射台效率均为 0 时才判定失效**，否则按已建成数量计算。对齐原版 `SectorInfo.prepare()`（`SectorInfo.java:244-249`）的语义：

```java
var pads = indexer.getFlagged(state.rules.defaultTeam, BlockFlag.launchPad);
//disable export when launch pads are disabled, or there aren't any active ones
if(pads.size == 0 || !pads.contains(t -> t.efficiency > 0)){
    export.clear();
}
```

按 `Σ(效率 × 单台速率)` 算会导致空间站一次电力波动就让全行星接收台集体禁用，并且效率在 0/1 间抖动时红字和禁用状态会**闪烁**。所以不采用。

### 7.3 后台区块的配额快照（⚠️ 高风险假设）

**问题**：配额存在 building config 里，但玩家离开区块后区块**不会被加载**，方块实例不存在 → 无法遍历它的接收台读取配额。而 `SectorInfo` 是原版类，不能加字段。

**解法**：镜像 `Sector.saveInfo()` 的模式（`Sector.java:101-103`）：

```java
public void saveInfo(){
    Core.settings.putJson(planet.name + "-s-" + id + "-info", info);
}
```

用自己的 key：`planet.name + "-s-" + id + "-orbital-quota"`，存"该区块所有接收台的配额汇总"（按物品类型分别汇总）。

**规则**：
- **当前游玩区块永远用实时数据，绝不读快照**（否则会双重计算）
- **写入时机**：配置变更时**立即写** + 存档时再写一次
  - 存档钩子：`SaveWriteEvent`（在 `SaveIO.java:124` 触发；原版自己在 `Control.java:289-294` 用它做同类事情）
  - 立即写：`Core.settings.putJson` 很便宜，且改配额是低频操作，能把崩溃/强退的丢失窗口压到最小

### 7.4 后台结算

在 `Events.on(TurnEvent.class, ...)`（`Universe.java:264` 触发）里做：
- 读取空间站 `info.items` 库存
- 按各区块配额分配
- 调用 `Sector.addItems()` / `removeItems()`

**关键数值语义**（`Sector.java:219-232`）：

```java
public void addItems(ItemSeq items){
    if(isBeingPlayed()){
        // 加到当前队伍核心的存储，受核心 storageCapacity 限制
    }else if(hasBase()){
        items.each((item, amount) -> info.items.add(item, Math.min(info.storageCapacity - info.items.get(item), amount)));
        info.items.checkNegative();
        saveInfo();
    }
}
```

**三个坑**：

1. **`storageCapacity = 0` 时静默丢弃**（`:228`）。而 `storageCapacity` 在 `SectorInfo.prepare()`（`:224`）里由核心决定：`storageCapacity = entity != null ? entity.storageCapacity : 0;`。**空间站地图里必须有核心**，否则 `Math.min(0 - 0, amount) = 0`，物品一个都进不去且不报错。
2. **`addItems` 要求 `hasBase()`**（`:227` + `:135` = `save != null && info.hasCore`）。空间站没被访问过、没存档时，`addItems` 什么都不做。
3. **`SectorInfo.prepare()` 会 `items.clear()` 再从核心重建**（`SectorInfo.java:200-209`），且它在离开区块时调用。所以**不要在空间站地图内直接往 `info.items` 塞东西**，离开时会被核心实际内容覆盖。要么往实际核心的 `ItemModule` 塞，要么只在"不处于该区块"时用 `addItems`。

### ✅ 验收

| 验收点 | 期望 |
|---|---|
| 多区块配额分配 | 数值与实际到达量一致 |
| 超配 | 全盘禁用 + 红字显示正确数值 |
| 超配下 | 配额能调低，不能调高 |
| 拆除空间站发射台 | 触发超配联锁 |
| 切图 | 快照正确写入，后台继续结算 |

---

## 阶段 8：折叠太阳能板

本体 **1×1 `Block`**，`powerProduction = 2f`（= 120 电力/s，内部按每 tick 算）。展开后的 3×4 **纯粹是贴图**，不涉及占格——这正好绕开了"`Block.size` 只能是单个 int、多格建筑必须正方形"（`Block.java:215`）和"虚空格 `floor().placeableOn = false` 不能放置"（`Build.java:254`）两个约束。

造价：铅 80 / 硅 100 / 相织物 20，生命值 450。

**爆炸逻辑用 `updateTile()` 轮询，不要用 `TilePreChangeEvent`。** 后者（`EventType.java:393-400`）的注释明确警告：

> *WARNING! This event is special: its instance is reused! Do not cache or use with a timer.*
> *Do not modify any tiles inside listeners that use this tile.*

也就是说不能在里面直接摧毁太阳能板，必须 `Time.runTask` 延后；而且它不是可取消事件。更麻烦的是事件驱动覆盖不全（payload 放置、从存档加载时就已重叠等情况）。

**做法**：每隔 N tick 扫一遍自己覆盖的 3×4 区域（12 格），发现 `tile.build != null` 就爆炸自毁。所有放置路径都覆盖，12 格 + 节流开销可忽略。

### ✅ 验收

- 展开路径上有建筑 → 面板爆炸
- 展开后往面板占据的格子放方块 → 面板爆炸
- 加载存档时就重叠 → 也能正确处理

---

## 阶段 9：打磨

- 全部贴图（注意命名，见第八节）
- `bundle_zh_CN.properties` / `bundle.properties` 本地化
- 数值平衡（空间站库存上限 = 核心 `storageCapacity`，这是主要的平衡旋钮）
- **多人测试**：`state.rules.sector.info` 的操作在战役里是服务器权威的，客户端到服务器的操作需要 `Call.` remote 方法。参考 `LandingPad` 的 `@Remote(called = Loc.server) landingPadLanded`（`LandingPad.java:130-134`）
- 后台结算在多人和单机下的行为一致性

---

## 八、命名与前缀（会影响每一个内容）

**Java 创建的内容，内部名会自动加 mod 名前缀。**

`MappableContent` 构造器（`MappableContent.java:11`）：
```java
this.name = Vars.content.transformName(name);
```
`ContentLoader.transformName`（`ContentLoader.java:184-186`）：
```java
return currentMod == null ? name : currentMod.name + "-" + name;
```

所以 `new Floor("space-station-floor")` 在模组 `orbital-logistics` 里实际得到 **`orbital-logistics-space-station-floor`**。

**贴图同理**（`Mods.java:400`）：`assets/sprites/space-station-floor.png` → 图集里叫 `orbital-logistics-space-station-floor`。

**两者前缀规则一致**，所以你把贴图命名成 `sprites/space-station-floor.png`，内容名和贴图名自动对得上，默认的 `Core.atlas.find(name)` 直接就能找到。

### 固定地图的文件名（这个坑必须记住）

`ContentLoader.handleMappableContent`（`:199-201`）会自动设置 `content.minfo.mod = currentMod`。所以对 Java 模组动态创建的 `SectorPreset`，**`minfo.mod` 一定不是 null** → `FileMapGenerator` 里那段"去前缀"的代码**一定会执行**：

```java
// FileMapGenerator.java:33-42
if(preset.minfo.mod != null){
    String baseName = mapName.substring(1 + preset.minfo.mod.name.length());   // ← 没有长度保护
```

| 你传的 `fileName` | `mapName` | `substring(18)` 的结果 |
|---|---|---|
| **null**（不传）→ 用 `this.name` | `orbital-logistics-station`（26 字符） | `"station"` ✅ |
| `"station"`（7 字符） | `"station"` | **StringIndexOutOfBoundsException** ❌ |

**结论：`fileName` 留 null，preset 命名 `station`，地图文件放 `assets/maps/station.msav`。**

`FileMapGenerator` 的查找顺序（`:25-47`）：
1. `maps/<空间站星球名>/<mapName>.msav`
2. `maps/<mapName>.msav`
3. `maps/<空间站星球名>/station.msav`
4. **`maps/station.msav`** ← 命中这里

---

## 九、技术结论速查表

| 主题 | 结论 | 源码位置 |
|---|---|---|
| 电力单位 | 内部按每 tick，UI ×60 | `Blocks.java:6810` `consumePower(4f)` = 240/s |
| 计时单位 | 秒数 = `60f * n` | `Blocks.java:6807` |
| 显示冷却 | `(cooldownTime + arrivalDuration) / 60f` | `LandingPad.java:122` |
| 多格建筑 | 只能正方形（`size` 是单个 int） | `Block.java:215` |
| 虚空放置 | `!floor.placeableOn && !type.ignoreBuildDarkness` 才拒绝 | `Build.java:254` |
| floor 放置 | 走独立路径，不建 Building | `Build.java:223-226`、`ConstructBlock.java:78-87` |
| 载荷尺寸上限 | `size() / tilesize <= limit`，标准上限 3 | `Payload.java:50-52` |
| 载荷不被 dump | 重写 `isBanned()` → `canCreate` false | `UnitPayload.java:108` |
| 单位环境死亡 | `!supportsEnv` → `Call.unitEnvDeath` | `UnitComp.java:745-749` |
| 载荷状态下不跑 update | `UnitPayload.update` 调 `updatePayload` | `UnitPayload.java:50-53` |
| 空间站环境 | `defaultEnv = Env.space` → `rules.env` | `World.java:327` |
| 方块环境需求 | `envRequired = Env.space` | `Block.java:272, 1015` |
| 后台模拟开关 | `Planet.updateGroup` | `Planet.java:171`、`Universe.java:158-163` |
| 后台库存累积 | `production.mean × 秒数`，按 `storageCapacity` 封顶 | `Universe.java:213` |
| 每回合钩子 | `Events.fire(new TurnEvent())` | `Universe.java:264` |
| 存档钩子 | `SaveWriteEvent` | `SaveIO.java:124`、`Control.java:289-294` |
| 每区块持久化 | `Core.settings` + `<planet>-s-<id>-xxx` key | `Sector.java:101-103` |
| addItems 上限 | `storageCapacity`，为 0 时静默丢弃 | `Sector.java:219-232` |
| hasBase 条件 | `save != null && info.hasCore` | `Sector.java:135-137` |
| 行星级导入速率 | `refreshImportRates` / `eachImport` | `SectorInfo.java:141-162, 345-352` |
| 点对点目的地 | `sector.info.destination` | `SectorInfo.java:52` |
| config 序列化类型 | 固定集合，`Point2` 可用 | `TypeIO.java:42-111` |
| 单物品发射台 | `acceptMultipleItems = false` 默认 | `LaunchPad.java:40, 138-140` |
| 单位载荷构造 | `new UnitPayload(unit.create(team))` | `PayloadSource.java:137` |
| 单位造价来源 | 仅 `Reconstructor`/`UnitFactory`/`UnitAssembler` | `UnitType.java:1349-1371` |
| 内容名加前缀 | `transformName` | `ContentLoader.java:184-186` |
| 贴图名加前缀 | sprite packing | `Mods.java:400` |
| 地图查找顺序 | 4 个候选 + 无保护的 `substring` | `FileMapGenerator.java:25-47` |
| 地图必须有核心 | 否则抛异常 | `FileMapGenerator.java:143-145` |

---

## 十、已知风险

| 风险 | 影响 | 应对 |
|---|---|---|
| **地板占方块层** | 机器建不上，整个玩法不成立 | 阶段 1 第一件事就验证 |
| **`updateGroup` 不生效** | 空间站不参与后台模拟，库存不累积 | 阶段 2 验证；若失败需自写回合逻辑 |
| **`isBanned()` 副作用** | 已逐一核查 13 处调用点，结论安全 | 阶段 3 实测 |
| **`spawnedByCore` 意外为 true** | 解构器拒绝载荷 | 阶段 3 实测 |
| **超配后玩家被锁死** | 只能靠加发射台自救 | 已定"配额只可调低"作为出路 |
| **后台快照过期** | 崩溃/强退导致配额误判 | 已定"配置变更时立即写" |
| **多人不同步** | 客户端操作不生效 | 阶段 9 专门测试，服务端权威 + `Call.` remote |
| **空间站地图的核心容量** | `storageCapacity = 0` 会静默吞掉所有物品 | 地图必须有核心，且容量是主要平衡旋钮 |

---

## 十一、下一步

从 **阶段 1** 开始。第一件事：编辑器里铺一片 `Blocks.space`，把空间站地板放上去，再在同一格放一台机器。
