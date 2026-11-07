## 12 图示资产

| 图示 | 可编辑源文件 | 查看/引用 |
| --- | --- | --- |
| 业务功能总览 | [Excalidraw](./01-业务功能总览.excalidraw) | [SVG](./01-业务功能总览.svg) · [PNG](./01-业务功能总览.png) |
| 技术栈与数据流 | [Excalidraw](./02-技术栈与数据流.excalidraw) | [SVG](./02-技术栈与数据流.svg) · [PNG](./02-技术栈与数据流.png) |

源文件可直接在 Excalidraw 中打开编辑；主文档使用 SVG，PNG 用于导出或分享。业务图用实线表示已有代码、橙色域表示包含模拟/模板、虚线表示规划；技术图标明主链路和爬虫直连数据库的例外。

### 重生成

[生成图示.mjs](./生成图示.mjs) 使用 Excalidraw 0.18.0 的官方 `convertToExcalidrawElements`、`serializeAsJSON`、`exportToSvg`、`exportToBlob`，在本地无头浏览器中导出。[导出 API 文档](https://docs.excalidraw.com/docs/@excalidraw/excalidraw/api/utils/export)说明其接口。脚本阻断浏览器网络请求，不上传项目内容。

```bash
cd docs/12-图示
npm install
npm run draw
```

工具依赖与业务工程隔离，不参与主应用构建。也可在临时目录安装依赖，将 `THISWAY_DIAGRAM_DEPS` 指向该目录，再从仓库根目录执行 `node docs/12-图示/生成图示.mjs`。

脚本会覆盖这两张图的源文件和图片。若在 Excalidraw 手工修改源图，应从修改后的源图重新导出；不要直接运行脚本覆盖手工改动。渲染需支持 Chromium 的本地环境及中文字体；浏览器或字体不同可能产生排版差异。
