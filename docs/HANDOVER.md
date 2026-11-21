# ClashNeo — 交接文档

本文件面向后续维护者。内容涵盖项目来源、模块结构、Compose UI 的架构约定、当前迁移进度、已知缺口与后续建议。

最后更新：v2.11.49

---

## 1. 项目来源与许可

ClashNeo 是 [MetaCubeX/ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid) 的 fork，
包名由 `com.github.metacubex.clash` 改为 `io.github.neo.clash`，UI 层被替换为
[Miuix](https://github.com/compose-miuix-ui/miuix)（MIUI/HyperOS 风格的 Compose 组件库）。

**许可证：GPL-3.0。** 这不是可选项：

- `LICENSE` 与 `NOTICE` 必须保留；
- 分发修改版时须提供对应源码；
- 界面中的「关于」页必须保留上游与内核的署名（已实现，勿删）。

代理内核 `mihomo` 以 git submodule 形式挂在 `core/src/foss/golang/clash`，**不要修改该目录内的任何文件**，
也不要在全局 grep/替换时漏掉对它的排除——它的路径里含有 `clash`，早期一次重命名曾因此误跳过新目录。

---

## 2. 模块结构

| 模块 | 职责 |
|---|---|
| `app` | 应用模块。启动入口为 `ui/ClashNeoComposeActivity`（Compose）。同时仍保留旧的 XML Activity。 |
| `design` | **历史遗留的 XML/View UI**：34 个 layout、87 个 Kotlin 文件（`*Design` 类 + adapter + dialog + preference）。正在逐步拆除。 |
| `service` | 后台服务、TUN、配置管理、Room 数据库。 |
| `core` | 内核桥接（Go + JNI）、数据模型。 |
| `common` | 常量、工具、Intent action 定义。 |
| `hideapi` | 隐藏 API 的编译期桩。 |

**关键约束**：`service` / `core` / `common` **不依赖 `design`**。这条边界必须守住，否则 UI 代码会渗进业务层，
以后想彻底删除 `design` 就不可能了。改动前可用
`grep -rn 'io.github.neo.clash.design' service/src core/src common/src` 复核，应当无输出。

---

## 3. Compose UI 架构

### 3.1 分层

```
ui/
  ClashNeoComposeActivity.kt   Activity：拥有导航栈、主题状态、子页面状态
  ClashUiController.kt         状态所有者：tunnel / proxies / profiles 的 StateFlow
  UiState.kt                   数据类
  navigation/Routes.kt         @Serializable 的 Route 密封接口
  theme/                       主题、颜色、形状、backdrop 模糊
  widget/                      可复用组件（顶栏、页面骨架、卡片、选择行…）
  page/<feature>/              各页面
  library/                     FloatingBottomBar（液态玻璃底栏）
  library/liquid/              透镜、内阴影、vibrancy 着色器
  animation/                   拖拽物理、按压高亮、预测性返回过渡
```

### 3.2 页面约定

每个页面遵循同一形状：

- 一个 `@Immutable data class XxxUiState`，字段全部是不可变值；
- 一个 `@Composable fun XxxPage(state, onStateChange / onXxx, modifier, outerPadding, onBack)`；
- **一切副作用走回调**（对话框、文件选择器、内核调用），页面本身不做 IO；
- 一律通过 `MiuixPageScaffold` 渲染，它负责顶栏、backdrop 层、滚动行为与安全区 inset。

`outerPadding` 必须由宿主传入底栏高度，否则列表最后几行会被底栏遮挡。这是曾经真实出现过的缺陷。

### 3.3 导航

导航栈是 `SnapshotStateList<NavKey>`，由 `NavDisplay` 渲染。要点：

- **不要在 entry 内部注册 `BackHandler` / `PredictiveBackHandler`**。`NavDisplay` 内部的
  `PredictiveBackHandlerWithSessions(enabled = size > 1)` 是唯一的手势驱动者，任何 entry 级返回处理器
  都会抢先消费手势，导致预测性返回没有预览动画。
- 使用 `mutableStateListOf` 而非 `rememberNavBackStack`。后者在恢复已保存内容时会**再次加入初始路由**，
  而深色模式切换会触发 `recreate()`，于是栈里出现两个 `Main`，`NavDisplay` 抛
  `Duplicate contentKey on the back stack`。
- 预测性返回依赖 manifest 的 `android:enableOnBackInvokedCallback="true"`。缺了它系统根本不会上报手势进度。

### 3.4 主题

`ClashNeoTheme` 用 Miuix 的 `ThemeController` 驱动配色，而不是传一个现成的 `Colors`——只有 controller 会把
种子色、调色板风格和动态取色真正送进配色生成。

两个开关是**独立**的，不要混：

- `useMiuixMonet` 选择引擎（是否由种子色派生整组配色）；
- `useDynamicColor` 只决定**种子色来源**（系统壁纸 accent vs 品牌色）。

早期实现把二者合并，结果「调色板风格」选择器看起来能动、屏幕却毫无变化。

### 3.5 模糊与底栏

底栏有三种呈现，是**三个不同的组件**，不是同一个换参数：

| 条件 | 组件 |
|---|---|
| OS4 | `MiuixGlassNavigationBar` — 逐项玻璃药丸 |
| OS3 + 悬浮 | `FloatingBottomBar` — 液态玻璃药丸，滑动指示器 |
| OS3 + 平铺 | `NavigationBar` + 背景模糊 |

backdrop 的接线是模糊能否生效的关键：**只挂一个**，且由底栏样式决定——

- 悬浮 → 整页记录进 `floatingBackdrop`（药丸要折射整个页面）；
- 平铺 → 只记录模糊面 `miuixBackdrop`。

采样者与被记录内容必须是**兄弟节点**。早期把宿主通用 backdrop 挂上去，采样者与内容落在相同或无关节点，
模糊等于采样空层，所以「开关都开了却毫无变化」。

---

## 4. 迁移进度

### 已完成（Compose）

控制、代理、配置、设置、关于、主题、网络、访问控制、覆写、应用设置、配置属性、日志列表、
实时日志、代理提供者、文件浏览、新建配置、Meta 功能、开源许可 —— 共 18 个页面，
`ui/` 下 54 个文件约 8700 行。

### 已删除的 XML 页面

`ProfilesActivity`、`ProvidersActivity`、`NewProfileActivity`、`HelpActivity` 及其
`*Design` 类、adapter、layout（layout 43 → 34）。

**删除方法**：Activity 与它的 `*Design` 必须**同一次提交**删除，因为 Activity 是 `*Design` 唯一的存活引用。
只删 `*Design` 会立刻构建失败。

### 仍为 XML

`LogsActivity`、`LogcatActivity`、`PropertiesActivity`、`FilesActivity`、`AppSettingsActivity`、
`MetaFeatureSettingsActivity`、`NetworkSettingsActivity`、`OverrideSettingsActivity`、
`AccessControlActivity`、`ProxyActivity`、`SettingsActivity`。

它们与 Compose 版本**并存**，属于重复实现。清理时按「成对删除」处理，并注意：

- `PropertiesActivity` 有外部入口：`ExternalControlActivity`（处理 `clash://` 外部 intent）；
- `FilesActivity` 只被 `PropertiesActivity` 引用，所以两者要一起处理；
- `ApkBrokenActivity` / `AppCrashedActivity` **不是死代码**，`Remote.kt` 在服务异常或崩溃时启动它们，
  必须保留。

---

## 5. 已知缺口（明确记录，未实现）

1. **分享配置为二维码**：原本只在已删除的 Profiles 菜单里，Compose 配置页没有，需要重新实现。
2. **新建配置的上拉控制台**：目前 FAB 与 `NewProfilePage` 尚未按上游的底部弹出面板形态接好。
3. **许可证列表是手写的**，不是由构建元数据生成。项目没有引入 licence-metadata 插件；若要自动化，
   需评估 `aboutlibraries` 之类的依赖。
4. **实时日志是轮询**（每秒重读最新日志文件），不是订阅流。`LogcatReader` 是文件读取器，
   服务端没有暴露可供 UI 订阅的流。
5. **发布签名**：`keystore` 与密码通过 GitHub Secrets 注入（`SIGNING_KEYSTORE_BASE64`、
   `SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`）。
   **务必离线备份 keystore**，丢失将无法覆盖安装后续版本。

---

## 6. 构建与 CI

- Gradle Wrapper 9.8.1（AGP 9.4.1 要求 ≥ 9.6）；
- `compileSdk = 37` + `compileSdkMinor = 2`（miuix AAR 声明 `minCompileSdk=37.2`，只写 37 会被 AAR 元数据检查拒绝）；
- `targetSdk = 35`，`minSdk = 24`；
- `android.builtInKotlin=false` 与 `android.newDsl=false` 必须成对设置，否则 `kotlin("kapt")` 无法应用；
- miuix 依赖位于 GitHub Packages，**必须认证**，CI 中用 `packages: read` 权限 + 写入
  `~/.gradle/gradle.properties` 的 `gpr.user` / `gpr.key`；
- `miuix-blur` 声明 minSdk 33，manifest 用 `tools:overrideLibrary="top.yukonga.miuix.kmp.blur"`
  覆盖，运行时由 `isOs4PlatformSupported` 兜底。

工作流：`build-debug`、`build-release`（自动改版本号并打 tag）、`build-compose-debug`、
`update-dependencies`。Release 工作流需要 `contents: write`，否则版本改动无法 push。

---

## 7. 排查经验

- **编译通过 ≠ 运行时正确**：`@Serializable` 缺编译器插件时编译无警告，启动即崩
  （`Serializer for class 'Route$Main' is not found`）。服务端要验证生成物里确实有 `serializer()`。
- **数据绑定会暴露悬空资源**：删除 `*Design` 后重跑 kapt，它会指出仍在引用已删类的 layout
  （例如根节点绑定了 `master` 变量）。
- **只按 Kotlin 引用判断资源是否可删会误判**：`common_activity_bar` 之类没有 Kotlin 引用，却被其他
  layout 通过 `<include>` 使用。删除前要同时检查 `@layout/` 引用。
- **沙箱内 AAPT2 无法启动**，因此本地只能验证到 Kotlin 编译；打包与 R8 验证必须在 CI 完成。