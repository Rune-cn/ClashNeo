# 功能真实性清单

本文档回答一个问题：**哪些界面是真的在做事，哪些只是会动的装饰。**

背景：Compose 重构过程中，大量页面先搭了 UI，数据源留空或只用 `remember { mutableStateOf(...) }` 承接。
结果是开关能拨、列表能滑、按钮能按，但没有任何东西被保存或生效——看起来像坏了，其实是空的。
这份清单记录每一处的真实状态，避免下一个人再被表象误导。

最后更新：v2.11.58

---

## 一、曾经是假的，现已修好

| 功能 | 假在哪 | 现在真在哪 |
|---|---|---|
| **导入配置** | 只调 `create`，只写一条 pending 记录和空 `config.yaml`，配置永远没有内容、永远不能激活 | `importProfile` = `create` → `patch` → **`commit`**（`ProfileProcessor.apply` 真正下载并导入）→ 刷新 → 首个配置自动激活 |
| **拉取/更新配置** | 对 pending 配置调 `update`，刷新不存在的内容 | pending 走 `commit`，已导入走 `update` |
| **保存配置编辑** | 只 `patch`（写 pending 行），导入副本从不更新 | `patch` + `commit` |
| **点待导入的配置** | 弹一句"需先导入"就结束，而应用里没有导入入口 | 直接下载并激活 |
| **网络设置** | 状态只存在 Activity 里，拨动只改 UI | `UiStore.enableVpn` + `ServiceStore.{bypassPrivateNetwork, dnsHijacking, allowBypass, allowIpv6, systemProxy}` |
| **应用设置** | 同上是本地状态；`autoRestart`/`hideAppIcon` 恒从 `false` 开始 | 组件启用状态 + `UiStore.hideFromRecents` + `ServiceStore.dynamicNotification` |
| **覆写设置** | 本地状态，从不写内核 | `withClash { queryOverride / patchOverride(Persist) }`，**先读再只改本页拥有的字段**，避免冲掉 `secret`/`hosts`/CORS |
| **访问控制** | 应用列表永远为空，选中项不保存 | `PackageManager.getInstalledApplications` + `ServiceStore.{accessControlPackages, accessControlMode}` |
| **日志列表** | 绑定在没人填充的本地状态，永远空状态 | 扫描 `cacheDir/logs`，按时间倒序，反映日志服务是否在运行 |
| **删除全部日志** | 行没有 handler，点了没反应 | `deleteAllLogs()` 删除并上报结果 |
| **文件浏览** | 同理永远为空 | 列出内核工作目录 `filesDir/clash`，目录优先、带大小，支持进入/返回上级 |
| **主题设置** | `ThemeUiState` 只在 `remember` 里，**每次冷启动全部重置** | `ui/store/ThemeStore.kt` 持久化（枚举按名存取、找不到回落默认） |

---

## 二、仍然没有实现（如实标注，未用占位行伪装）

| 功能 | 为什么不能便宜地做 |
|---|---|
| **代理提供者列表** | 提供者在内核 REST 控制器后面，`IProfileManager` 没有暴露查询接口。要有 HTTP 客户端 + 内核运行中才能取。 |
| **二维码扫描** | 需要相机栈（CameraX + 解码）。当前只有生成（`ui/util/QrCode.kt`，zxing），扫描会如实提示不可用。 |
| **应用内文件查看器** | 文件页点击文件目前无反应。没有文本/配置查看器；点击不响应比做一个假的查看器好。 |
| **`authentication`（覆写里的代理认证）** | 覆写页有开关，但设置凭据需要输入用户名/密码界面；当前保留内核原值，不覆盖。 |
| **`external-controller` TLS / CORS / hosts** | 内核模型里有，界面还没做编辑器；`saveOverride` 会保留这些字段不覆盖。 |

---

## 三、判断某个控件是否是真的，怎么查

1. **看状态从哪来**：`grep -n 'mutableStateOf' <Activity>.kt`，凡是在 Activity 里 `remember` 出来的，
   若没有对应的 store 读取，就是假的。
2. **看写入写到哪**：搜索 `persistXxx` / store 赋值。只改了 `remember` 变量的，刷新就丢。
3. **看列表能否非空**：找到页面的 `state.xxx` 字段，再 grep 谁给它赋值。没人赋值 = 永远空状态。
4. **看内核侧是否真的生效**：配置类操作必须落到 `withProfile { … }` 或 `withClash { … }`；
   只调 `create` / `patch` 而不 `commit`，内核拿不到内容。

---

## 四、回归时最容易再犯的两处

- **`create` 不等于导入**。`ProfileManager.create` 只建 pending 记录，必须 `commit` 才有配置内容。
- **`patch` 不等于生效**。它只写 pending 行，必须 `commit` 才会进入导入副本。