# 0.1.35 用户功能复测证据

日期：2026-10-11（Asia/Shanghai）。[完整功能与工作流报告](../../USER_FEATURE_WORKFLOWS_2026-10-11.md)。

| 文件 | 含义 |
| --- | --- |
| source-check.json | 261 项源码 / 构建输入与此前保留 APK 验收源码的 LF 哈希核对，0 差异 |
| unit-tests.json | 本次重新执行的 150 JVM 单元用例全部通过 |
| device-tests.json | 动画关闭、与项目配置一致的本次完整 310 项仪器测试，全部通过 |
| feature-test-map.json | 80 项文档到关联测试类与本次结果的映射；关联类通过不表示功能所有分支已实测 |
| device-instrumentation.raw.log | 动画关闭全套原始 runner 输出，272.832 秒 |
| device-tests-animation-on.json | 第一轮 310 项，309 通过 / 1 失败 |
| device-instrumentation-animation-on.raw.log | 第一轮原始输出，保留旋转断言失败，429.156 秒 |
| rotation-repeat.log | 动画开启的原用例复测，仍在动画过程中失败 |
| rotation-no-animation.log | 关闭动画后同 APK、同原用例通过，8.175 秒 |
| animation-original.json | 操作前动画倍率；测试后已恢复为 1.0 |
| 02-full-suite-rotation.png / .txt | 第一轮旋转动画中的截图及窗口几何 |
| 03-repeat-rotation.png / .txt | 同配置单项复测的截图及窗口几何 |
| 01、04–18 的 .png / .ui.json | 当前 internal 包的页面和系统入口操作；字段为公开 UI 文本，不含 Key |
| 19–24 的 .png / .ui.json | 模拟器重启后恢复候选、候选引用异常、未确认移除及原有 2 项任务检查 |
| final-installation.json | 最终包清单、版本、已卸载的测试包及实际 base.apk 哈希比对 |
| 25-final-home.png / .ui.json | 最终仅当前版保留、模拟器开启时的今日页 |

没有读取应用私有目录或密钥，没有发布导出的当前业务 JSON。单元构建原始日志另保留在本机工作区 `work/audit-unit-2026-10-11.log`。此目录的 runner 原始日志不包含 JUnit 全量 system-out，测试时长与状态以相应 runner / XML 为准。

环境：Android 16 / API 36 / x86_64；只有一台模拟器，不扩大为真机、OEM、全部外部服务或完全人工覆盖。安装包身份见报告；本次不重建并替换已验收 APK。
