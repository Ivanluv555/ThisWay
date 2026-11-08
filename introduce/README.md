# 此地 ThisWay · 产品介绍页

面向介绍页维护者。这是独立的 React 静态产品页，所有源码、配置、原创插画与测试均位于 `introduce/`，不依赖业务服务运行。

**当前状态：页面实现完成，静态检查完成；生产构建与浏览器验证因执行权限限制尚未进行。** 详见 [验证报告](docs/验证报告.md)。

## 启动

安装 Node.js 22.12+，进入本目录：

```sh
cd introduce
npm ci --ignore-scripts
npm run dev
```

终端显示本地地址，默认使用 Vite 的本地端口。预览服务器只绑定回环地址。

## 构建与预览

```sh
npm run build
npm run preview
```

构建输出目录为 `dist/`。将其内容发布到静态 HTTP 服务，不要直接双击源码 `index.html`。配置使用相对资源路径，适合在站点目录中承载。页面仅使用锚点导航，无需业务后端或路由回退服务。未替用户公开部署。

## 浏览器验证

先完成构建，再安装测试浏览器。浏览器文件保留在本目录，不写入项目其他包。

```sh
PLAYWRIGHT_BROWSERS_PATH="$PWD/.cache/ms-playwright" npx playwright install chromium
npm test
npm run test:report
```

测试配置自动读取同一浏览器缓存路径，启动本地生产预览。测试项目包括桌面、移动端与减少动态效果模式。结果保存在 `artifacts/`，已忽略提交。若系统缺 Chromium 运行库，需要在获准后配置运行环境。

当前环境已允许禁用生命周期脚本的依赖安装，但拦截了构建工具执行及浏览器安装，以上步骤不能视为已经验证通过。

## 文件组织

```text
introduce/
├── src/
│   ├── App.tsx                  页面结构与交互
│   ├── content.ts               集中的产品文案与示例数据
│   ├── components/Landscape.tsx 原创场景插画
│   ├── main.tsx                 React 与 Motion 入口
│   └── styles.css               响应式样式与减少动态效果规则
├── public/favicon.svg          本地品牌图形
├── docs/                       考察方案与验证报告
├── tests/site.spec.ts          浏览器测试
├── playwright.config.ts        测试矩阵与输出路径
├── vite.config.ts              静态构建配置
├── tsconfig.json               严格类型检查配置
├── package.json
└── package-lock.json
```

## 内容与设计

- 主题：“风景值得，人潮不必”。品牌色沿用 Flutter 移动端的暖白与森林绿方向。
- 结构：首屏、产品能力、交互旅程、原理、理念、项目状态、FAQ、页脚。
- 动画只使用 Motion，图标只使用 Lucide React。支持系统减少动态效果偏好。
- 交互包括旅程切换、临时收藏、自主选择、移动菜单和键盘可展开 FAQ。
- 无外部字体、远程图片、定位、分析脚本或业务 API 请求。
- 所有拥挤状态均为示例，SVG 是场景意象而非实景照片或真实地图。
- 没有虚构下载链接、预测准确率、已上线声明或票务服务。收藏刷新即重置，不保存用户数据。

[项目考察与页面方案](docs/项目考察与页面方案.md) 记录了内容依据、页面结构与实现边界。
