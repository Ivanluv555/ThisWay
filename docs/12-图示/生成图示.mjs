// 使用官方 Excalidraw 转换与导出 API；所有渲染在本机浏览器中完成。
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const here = path.dirname(fileURLToPath(import.meta.url));
// 可指定已在临时目录安装的依赖，无需向工程加入 node_modules。
const require = createRequire(path.join(process.env.THISWAY_DIAGRAM_DEPS || here, 'package.json'));
const { build } = require('esbuild');
const { chromium: playwright } = require('playwright');
const chromium = require('@sparticuz/chromium');

let nextId = 0;
const elements = [];
const colors = { ink: '#24364b', muted: '#617187', blue: '#1971c2', green: '#2b8a3e', orange: '#d97706' };
function reset() { elements.length = 0; nextId = 0; }
function add(data) {
  elements.push({ id: `cidi-${++nextId}`, strokeColor: colors.ink, backgroundColor: 'transparent', fillStyle: 'solid', strokeWidth: 1.5, strokeStyle: 'solid', roughness: 0.7, opacity: 100, seed: 400 + nextId, ...data });
}
function text(x, y, value, size = 20, color = colors.ink, width = 1400) {
  add({ type: 'text', x, y, text: value, fontSize: size, fontFamily: 2, lineHeight: 1.35, width, strokeColor: color });
}
function box(x, y, width, height, fill, stroke = colors.ink, dashed = false) {
  add({ type: 'rectangle', x, y, width, height, backgroundColor: fill, strokeColor: stroke, strokeStyle: dashed ? 'dashed' : 'solid', roundness: { type: 3 } });
}
function arrow(points, color = colors.blue, dashed = false) {
  const [x, y] = points[0];
  add({ type: 'arrow', x, y, points: points.map(([a, b]) => [a - x, b - y]), strokeColor: color, strokeStyle: dashed ? 'dashed' : 'solid', startArrowhead: null, endArrowhead: 'arrow' });
}
function card(x, y, title, detail, planned = false) {
  box(x, y, 306, 100, '#ffffff', planned ? colors.orange : '#9caec1', planned);
  text(x + 18, y + 14, title, 22, planned ? colors.orange : colors.ink, 275);
  text(x + 18, y + 53, detail, 16, colors.muted, 275);
}
function business() {
  reset();
  text(44, 28, '此地 CIDI  /  业务功能总览', 34);
  text(46, 83, '发现景点、理解拥挤、获得建议，由游客自主选择', 19, colors.muted);
  const cols = [
    { x: 44, fill: '#edf5ff', title: '01  游客服务', sub: '发现与出行参考', cards: [['景点发现', '地图 · 搜索 · 详情'], ['出行参考', '拥挤指数 · 替代目的地'], ['个人服务', '登录 · 收藏 · 设置页面']] },
    { x: 424, fill: '#edf9f0', title: '02  内容社区', sub: '旅行信息与用户互动', cards: [['内容浏览', '动态 · 文章 · 分类'], ['内容互动', '发布 · 评论 · 点赞'], ['关注关系', '关注 / 粉丝接口已实现']] },
    { x: 804, fill: '#fff7e8', title: '03  数据智能', sub: '当前包含模板与模拟', cards: [['客流预测', 'LSTM · 下一小时人数'], ['分流建议', '转移概率 · 人数 · 优惠强度'], ['数据生产', '爬虫 · 模拟历史 · 定时任务']] },
    { x: 1184, fill: '#f5f0ff', title: '04  景区运营', sub: '管理闭环仍待完善', cards: [['管理员资源', '已有基础管理数据接口'], ['区域数据与监测', '实测接入 · 管理看板 · 告警', true], ['售票与预约联动', '限流建议 → 渠道执行与反馈', true]] },
  ];
  for (const c of cols) {
    box(c.x, 157, 350, 475, c.fill, '#ccd6e1');
    text(c.x + 22, 181, c.title, 26);
    text(c.x + 22, 224, c.sub, 16, colors.muted);
    c.cards.forEach(([title, detail, planned], i) => card(c.x + 22, 276 + i * 114, title, detail, planned));
  }
  box(44, 675, 1490, 106, '#f8fafc', '#ccd6e1');
  text(68, 692, '业务主线', 19, colors.blue);
  text(215, 706, '采集 / 历史  →  客流预测  →  拥挤与分流建议  →  游客自主选择', 24);
  text(45, 817, '实线：已有代码覆盖    橙色域：含模拟 / 模板    虚线：规划功能', 17, colors.muted);
  text(45, 851, '边界：六小时推荐复用同轮数据；不代表实际发券、强制分配或售票控制已实现。', 17, colors.muted);
  text(45, 894, '实现范围图  ·  2026-11-06  ·  运行阻碍与验证范围见 08-项目状态与待办', 15, colors.muted);
  return structuredClone(elements);
}
function technical() {
  reset();
  text(44, 28, '此地 CIDI  /  技术栈与数据流', 34);
  text(46, 84, '当前主链路：原生微信小程序 + Spring Boot + FastAPI + MySQL', 19, colors.muted);
  box(490, 143, 600, 115, '#edf5ff', colors.blue);
  text(515, 161, '表现层 · 微信小程序', 26, colors.blue);
  text(515, 208, 'JavaScript / WXML / WXSS · 原生地图 · wx.request', 18);
  arrow([[790, 268], [790, 311], [376, 311], [376, 381]]);
  text(64, 327, 'JSON · /api/*', 18, colors.blue);

  box(44, 392, 650, 217, '#edf5ff', colors.blue);
  text(70, 414, '业务服务 · Spring Boot 3.5.14', 27, colors.blue);
  text(70, 462, 'Java 17 · REST 控制器 · JPA / JDBC', 21);
  text(70, 504, '用户 / 景点 / 内容 / 历史客流 / 预测 / 转移', 19);
  text(70, 547, 'JWT 工具与微信认证 · springdoc / OpenAPI', 18, colors.muted);

  box(904, 392, 650, 217, '#fff7e8', colors.orange);
  text(930, 414, '模型与采集 · FastAPI', 27, colors.orange);
  text(930, 462, 'PyTorch LSTM · 拥挤指数 · 分流策略', 21);
  text(930, 504, 'pandas / NumPy / scikit-learn / httpx', 19);
  text(930, 547, '进程内模拟 / 调度 · requests / BeautifulSoup', 18, colors.muted);
  arrow([[892, 506], [706, 506]]);
  text(721, 415, 'HTTP', 20, colors.blue, 180);
  text(721, 451, '读历史', 17, colors.muted, 180);
  text(721, 539, '写预测 / 转移', 17, colors.muted, 180);

  box(44, 786, 650, 138, '#edf9f0', colors.green);
  text(70, 808, 'MySQL · 业务数据', 27, colors.green);
  text(70, 856, '用户 / 内容 / 景点 / 小时客流 / 预测 / 转移', 20);
  arrow([[376, 622], [376, 772]], colors.green);
  text(407, 674, 'JPA / JDBC', 18, colors.green, 260);

  box(904, 786, 650, 138, '#edf9f0', colors.green);
  text(930, 808, 'MySQL · spider_* 采集表', 27, colors.green);
  text(930, 856, '地区景点 / 景点详情 · 库实例由运行配置决定', 20);
  arrow([[1230, 622], [1230, 772]], colors.orange);
  text(1261, 657, 'PyMySQL', 18, colors.orange, 280);
  text(1261, 694, '采集同步直连例外', 17, colors.muted, 280);

  box(44, 986, 1490, 155, '#f5f6f8', '#98a4b3', true);
  text(68, 1007, '独立历史原型 · 不作为当前主链路依赖', 23, colors.muted);
  text(68, 1057, 'Flask / PyYAML / requests / NumPy', 20);
  text(68, 1093, '景点抓取 · 旧复杂拥挤模型 · /api/v1', 17, colors.muted);
  text(872, 1057, 'Node.js / Express', 20);
  text(872, 1093, '固定响应 · 前端早期联调', 17, colors.muted);
  text(45, 1180, '边界：图示为代码关系；统一 JWT 鉴权、实际缓存、WebSocket 和云函数未作为现有能力列入。', 17, colors.muted);
  text(45, 1220, '2026-11-06  ·  无业务数据上传  ·  运行状态与接口差异见文档 02 / 03 / 08', 15, colors.muted);
  return structuredClone(elements);
}

const entry = `import {convertToExcalidrawElements, exportToSvg, exportToBlob, serializeAsJSON} from ${JSON.stringify(require.resolve('@excalidraw/excalidraw'))}; window.CidiDraw={convertToExcalidrawElements,exportToSvg,exportToBlob,serializeAsJSON};`;
const built = await build({ stdin: { contents: entry, resolveDir: path.dirname(require.resolve('@excalidraw/excalidraw')) }, bundle: true, write: false, format: 'iife', platform: 'browser', define: { 'process.env.NODE_ENV': '"production"' }, loader: { '.woff2': 'dataurl', '.css': 'empty' }, logLevel: 'silent' });
const browser = await playwright.launch({ executablePath: await chromium.executablePath(), args: chromium.args, headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1700, height: 1400 } });
  // 导出过程拒绝网络请求，所有项目内容留在本机。
  await page.route('**/*', route => route.abort());
  await page.setContent('<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body></body></html>');
  await page.addScriptTag({ content: built.outputFiles[0].text });
  for (const [name, skeleton] of [['01-业务功能总览', business()], ['02-技术栈与数据流', technical()]]) {
    const out = await page.evaluate(async (skeleton) => {
      const { convertToExcalidrawElements, exportToSvg, exportToBlob, serializeAsJSON } = window.CidiDraw;
      const scene = convertToExcalidrawElements(skeleton, { regenerateIds: false });
      const appState = { viewBackgroundColor: '#ffffff', exportBackground: true, exportWithDarkMode: false, exportScale: 1.5 };
      const svg = await exportToSvg({ elements: scene, appState, files: {}, exportPadding: 32 });
      const blob = await exportToBlob({ elements: scene, appState, files: {}, mimeType: 'image/png', exportPadding: 32 });
      const png = await new Promise(resolve => { const reader = new FileReader(); reader.onload = () => resolve(reader.result.split(',')[1]); reader.readAsDataURL(blob); });
      return { svg: svg.outerHTML, png, json: serializeAsJSON(scene, appState, {}, 'local'), count: scene.length };
    }, skeleton);
    await fs.writeFile(path.join(here, name + '.excalidraw'), out.json + '\n');
    await fs.writeFile(path.join(here, name + '.svg'), out.svg + '\n');
    await fs.writeFile(path.join(here, name + '.png'), Buffer.from(out.png, 'base64'));
    console.log(`${name}: ${out.count} editable Excalidraw elements; SVG + PNG exported`);
  }
} finally { await browser.close(); }
