# 运行时交互与动效

## 从图片读取的 skill

图片提供的五项是 frontend-design、web-design-guidelines、webapp-testing、ui-ux-pro-max、vercel-react-best-practices。安装并读取了前四项的新 skill 以及 React skill；UI/UX Pro Max 沿用已安装版本。新安装的 skill 会在后续会话中可用，本轮直接读取其文件使用。

- [frontend-design](https://github.com/anthropics/skills/tree/dbd4588f9e1033efb41dad4bef2f7947c8993d44/skills/frontend-design)：采用围绕用户操作设计动效、保持视觉重点、删掉无意义装饰的原则。
- [web-design-guidelines](https://github.com/vercel-labs/agent-skills/tree/063bee94c3f4df8453406c830b0a7df0f2860278/skills/web-design-guidelines)：已读取其要求的最新 command.md；将可中断动画、减少动态效果、原生控件反馈、标签、焦点和草稿保护规则对应到 Android。HTML/CSS 专属规则不适用。
- [webapp-testing](https://github.com/anthropics/skills/tree/dbd4588f9e1033efb41dad4bef2f7947c8993d44/skills/webapp-testing)：参考先确认渲染状态、定位控件、执行操作、检查截图和日志的方法。当前是 Compose 应用，验证使用 Android 工具，未声称运行了 Playwright 网页测试。
- [ui-ux-pro-max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)：实际运行 `reduced motion animation --domain ux` 与 `animation transition --stack jetpack-compose`。采用减少无意义动效、按场景区分节奏、动画放在 UI 层、原生 Compose API 的已匹配规则。
- [vercel-react-best-practices](https://github.com/vercel-labs/agent-skills/tree/063bee94c3f4df8453406c830b0a7df0f2860278/skills/react-best-practices)：已阅读；React/Next.js API 不适用于 Kotlin。避免额外依赖、动画逐帧不重算业务数据等采用 Compose 原生实现，未引入 React。

## 实施前设计复核

产品是中文个人任务与日程工具，重点是记录、安排、开始执行。视觉延续第二轮的森林绿 #285E4A、纸面 #F5F7F5、白色 #FFFFFF、焦点 #173E33、行动 #D8ECB9 和文字 #192821；保持系统 SansSerif 语义字体与左对齐的信息结构。

动效概念：让用户能看出“去了哪里、打开了什么、按钮是否响应”。保留首页焦点为唯一较强视觉重点。取消逐张卡片自动飞入的方案，改为每次切换只移动一个内容容器；导航栏保持稳定。录入步骤有方向，展开内容从标题下展开。

拟定节奏：按下 90ms、释放 140ms、页面 240ms、展开 260ms、收起 180ms，采用平滑减速，不使用弹跳和循环装饰。具体值是本应用的选择，不是跨平台通用标准。

原版保持独立；动画只读界面状态，不延迟或触发保存，不复制业务数据。使用现有 Compose 依赖，录屏与测试均复用同一个模拟器。

## 实现与运行验证

- `theme/Motion.kt` 提供统一节奏、页面过渡、可反向展开及按钮反馈。逐帧变化读取在 graphicsLayer 中，业务计算不随动画帧重新执行。
- 内容切页与任务详情做淡入和小幅位移；底部导航固定。只保留当前页面，避免退场页面继续处理请求。
- 设置与日程显示设置从标题下展开/收起。按钮轻微压下后恢复，同时保留 Material ripple、禁用语义与 48dp 布局。
- 任务录入步骤按前进/返回方向滑入，只显示当前表单。抽屉及软键盘继续使用 Material/Android 原生行为。
- 逐帧检查后为详情信息块和录入步骤加入原生尺寸过渡，避免异步记录出现或步骤内容增高时突然推动其他控件。尺寸动画仅作用于这两类局部容器，系统关闭动画时直接呈现最终大小。
- 一个 ContentObserver 监听系统动画倍率；倍率为零时跳过新增动效，运行中更改设置即可生效，离开主题树会注销观察器。
- APK 与测试 APK 构建成功；150 项现有单元测试及离线边界检查通过。
- 10 项设备测试通过：4 项动效交互测试，以及 6 项原有录入回归，覆盖中途切页仍可点击、展开反向、系统设置即时生效、草稿往返、无效时长、保存错误重试、保存时禁用、大字体和日期保留。
- 实际录制约 30 秒运行片段，包含切页、详情、抽屉和设置展开/收起；已逐帧检查过渡与固定导航区域。也补查了深色、200% 字体且关闭动画时的首页。

这次没有跑整套仪器测试或真实模型请求。录屏来自 Android 模拟器的 debug 副本；静止画面会降低录制帧数，录屏平均帧率不代表界面帧率，也不作为真机性能保证。

## 结果

- [实际运行录屏](../../work/design-motion-preview/repland-motion-preview.mp4)，540×1200，约 2.3 MiB。
- [展开逐帧检查](../../work/design-motion-preview/expansion-frames.png)。
- [设备测试日志](../../work/design-motion-device-tests.log)。
- [构建日志](../../work/design-motion-final-build.log)。
- [当前包与原版校验](../../work/design-copy-preview-2026-10-14/motion-verification.json)。

代码、安装包和入口都在设计副本中。原版 Android 源码无本次修改，已安装原版 APK 与之前保存的哈希相同。临时视频解码工具使用既有 Android Studio 内置文件，验收后移除；不新增常驻服务。
