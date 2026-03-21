const express = require('express');
const cors = require('cors');
const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

// ================= 数据源 (Mock Database) =================

// 1. 文章数据 (用于首页推荐、动态流、文章详情页工厂)
const articles = [
  {
    id: 101,
    type: 'article',
    coverUrl: 'https://picsum.photos/400/300?random=1',
    title: '2024 城市指南：藏在老街区里的 5 家独立咖啡馆 ☕️',
    author: 'Cidi Editorial',
    date: '2 HOURS AGO',
    likes: 342,
    authorAvatar: 'https://api.multiavatar.com/User101.png',
    content: [ 
      { type: 'text', value: '在城市的喧嚣中，寻找一片宁静的角落。这家店位于老城区的深处，保留了百年的砖墙结构，是周末放松的绝佳去处。' },
      { type: 'image', value: 'https://picsum.photos/600/400?random=1001' },
      { type: 'text', value: '推荐尝试他们的手冲瑰夏，风味独特，回味悠长。在这里，时间仿佛慢了下来。' }
    ]
  },
  {
    id: 102,
    type: 'article',
    coverUrl: 'https://picsum.photos/400/300?random=2',
    title: '逃离地心引力：周末去山野吸氧的徒步路线 🌲',
    author: 'Urban Explorer',
    date: 'YESTERDAY',
    likes: 899,
    authorAvatar: 'https://api.multiavatar.com/User102.png',
    content: [
      { type: 'text', value: '这个周末，我们决定向山而行。远离电子屏幕，拥抱大自然的怀抱。' },
      { type: 'image', value: 'https://picsum.photos/600/400?random=1002' },
      { type: 'text', value: '整条路线全程约 5 公里，爬升不大，非常适合新手。沿途可以看见壮观的云海。' }
    ]
  },
  {
    id: 103,
    type: 'article',
    coverUrl: 'https://picsum.photos/400/300?random=3',
    title: '胶片美学：如何用手机拍出王家卫电影感 📷',
    author: 'Vision Lab',
    date: '3 DAYS AGO',
    likes: 1205,
    authorAvatar: 'https://api.multiavatar.com/User103.png',
    content: [
       { type: 'text', value: '光影是摄影的灵魂。利用百叶窗的阴影，你可以轻松制造出电影般的质感。' },
       { type: 'image', value: 'https://picsum.photos/600/400?random=1003' },
       { type: 'text', value: '色调方面，建议高对比度，低饱和度，再加上一点点颗粒感。' }
    ]
  },
  {
    id: 104,
    type: 'article',
    coverUrl: 'https://picsum.photos/400/300?random=4',
    title: '山城味蕾：为了这碗面，我愿意再飞一次重庆 🍜',
    author: 'Foodie Club',
    date: '1 WEEK AGO',
    likes: 2200,
    authorAvatar: 'https://api.multiavatar.com/User104.png',
    content: [
       { type: 'text', value: '重庆的小面，重在佐料。红油、花椒、芝麻酱，缺一不可。' },
       { type: 'image', value: 'https://picsum.photos/600/400?random=1004' }
    ]
  },
  {
    id: 105,
    type: 'article',
    coverUrl: 'https://picsum.photos/400/300?random=5',
    title: '极简主义生活：如何断舍离',
    author: 'Life Style',
    date: '2 WEEKS AGO',
    likes: 56,
    authorAvatar: 'https://api.multiavatar.com/User105.png',
    content: [
       { type: 'text', value: '少即是多。' }
    ]
  }
];

// 2. 地点数据 (从KML解析的21个精准坐标，用于地图和地点详情页工厂)
const locations = [
  { id: 1, title: '洪崖洞', latitude: 29.561147, longitude: 106.579508, address: '重庆市渝中区嘉陵江滨江路88号', desc: '巴渝传统建筑“吊脚楼”的现实版，千与千寻的梦幻场景。' },
  { id: 2, title: '解放碑', latitude: 29.557458, longitude: 106.577262, address: '重庆市渝中区民族路177号', desc: '重庆的精神堡垒，繁华商业中心的各种大牌云集。' },
  { id: 3, title: '长江索道', latitude: 29.557368, longitude: 106.583227, address: '重庆市渝中区新华路151号', desc: '万里长江第一条空中走廊，飞越山城的独特体验。' },
  { id: 4, title: '朝天门广场', latitude: 29.567106, longitude: 106.588020, address: '重庆市渝中区朝天门', desc: '两江交汇处，俯瞰嘉陵江与长江的壮丽景色。' },
  { id: 5, title: '江北嘴公园', latitude: 29.567581, longitude: 106.578313, address: '重庆市江北区', desc: '城市绿肺，与大剧院相邻，适合散步。' },
  { id: 6, title: '观音桥', latitude: 29.576438, longitude: 106.534378, address: '重庆市江北区', desc: '热闹非凡的商圈，美食与购物的天堂。' },
  { id: 7, title: '大礼堂', latitude: 29.561842, longitude: 106.553758, address: '重庆市渝中区人民路173号', desc: '宏伟的仿古民族建筑，重庆的标志性建筑之一。' },
  { id: 8, title: '十八梯', latitude: 29.552311, longitude: 106.573895, address: '重庆市渝中区', desc: '连接上下半城的梯坎，承载着老重庆的记忆。' },
  { id: 9, title: '白公馆渣滓洞', latitude: 29.561612, longitude: 106.403719, address: '重庆市歌乐山', desc: '红岩精神的象征，历史教育基地。' },
  { id: 10, title: '歌乐山革命纪念馆', latitude: 29.572268, longitude: 106.442305, address: '重庆市沙坪坝区', desc: '缅怀先烈，铭记历史。' },
  { id: 11, title: '南山植物园（北门）', latitude: 29.556436, longitude: 106.632417, address: '重庆市南岸区', desc: '四季花开，登高望远的好去处。' },
  { id: 12, title: '重庆大剧院', latitude: 29.569986, longitude: 106.581178, address: '重庆市江北区', desc: '现代建筑艺术的杰作，夜景璀璨。' },
  { id: 13, title: '磁器口', latitude: 29.577684, longitude: 106.448482, address: '重庆市沙坪坝区', desc: '千年古镇，品尝陈麻花，感受老街风情。' },
  { id: 14, title: '建川博物馆', latitude: 29.519989, longitude: 106.523219, address: '重庆市九龙坡区', desc: '由防空洞改建的博物馆聚落，极具特色。' },
  { id: 15, title: '鸿恩寺', latitude: 29.570069, longitude: 106.505315, address: '重庆市江北区', desc: '登临鸿恩阁，鸟瞰江北全景。' },
  { id: 16, title: '鹅岭公园', latitude: 29.549796, longitude: 106.536354, address: '重庆市渝中区', desc: '私家园林，俯瞰两江景色的绝佳机位。' },
  { id: 17, title: '鹅岭印刷二厂', latitude: 29.551038, longitude: 106.540646, address: '重庆市渝中区', desc: '文艺青年的打卡圣地，《从你的全世界路过》取景地。' },
  { id: 18, title: '三峡博物馆', latitude: 29.562078, longitude: 106.550478, address: '重庆市渝中区', desc: '了解巴渝文化和三峡工程历史的最佳场所。' },
  { id: 19, title: '龙门浩', latitude: 29.556425, longitude: 106.594802, address: '重庆市南岸区', desc: '历史文化街区，依山而建，风景独好。' },
  { id: 20, title: '南滨路', latitude: 29.543807, longitude: 106.583700, address: '重庆市南岸区', desc: '重庆的外滩，观赏渝中半岛夜景的最佳位置。' },
  { id: 21, title: '金刚碑', latitude: 29.843229, longitude: 106.425171, address: '重庆市北碚区', desc: '清幽古朴的古村落，远离尘嚣。' }
];

// 3. 用户收藏 (存储文章ID)
let userCollections = [101, 103];

// ================= API 接口 =================

// [新增] 0. 用户登录接口 (修复前端 404 错误)
app.post('/api/user/login', (req, res) => {
  const { username, password } = req.body;
  // 简单的模拟验证
  if (username && password) {
    res.json({
      code: 0,
      token: 'mock_token_' + Date.now(),
      userInfo: {
        username: username || 'Cidi User',
        bio: 'Explorer | Front-end Developer',
        avatarUrl: 'https://api.multiavatar.com/' + username + '.png'
      }
    });
  } else {
    res.status(400).json({ code: 400, msg: '用户名或密码不能为空' });
  }
});

// 1. 首页推荐列表 (精简版)
app.get('/api/home/list', (req, res) => {
  const list = articles.map(a => ({
    id: a.id,
    coverUrl: a.coverUrl,
    title: a.title,
    author: a.author,
    date: a.date
  }));
  res.json({ code: 0, data: list });
});

// 2. 动态瀑布流列表 (完整版)
app.get('/api/updates/list', (req, res) => {
  const list = articles.map(a => ({
    id: a.id,
    imageUrl: a.coverUrl,
    title: a.title,
    authorAvatar: a.authorAvatar,
    authorName: a.author,
    likes: a.likes
  }));
  res.json({ code: 0, data: list });
});

// 3. 地图标记点
app.get('/api/map/markers', (req, res) => {
  res.json({ code: 0, data: locations });
});

// 4. [页面工厂] 文章内容详情
app.get('/api/article/:id', (req, res) => {
  const id = parseInt(req.params.id);
  const article = articles.find(a => a.id === id);
  if (article) {
    res.json({ code: 0, data: article });
  } else {
    res.status(404).json({ code: 404, msg: '文章不存在' });
  }
});

// 5. [页面工厂] 地点详情
app.get('/api/location/:id', (req, res) => {
  const id = parseInt(req.params.id);
  const loc = locations.find(l => l.id === id);
  if (loc) {
    // 模拟该地点相关的多张图片
    const detail = {
      ...loc,
      images: [
        `https://picsum.photos/600/400?random=${id}1`,
        `https://picsum.photos/600/400?random=${id}2`
      ]
    };
    res.json({ code: 0, data: detail });
  } else {
    res.status(404).json({ code: 404, msg: '地点不存在' });
  }
});

// 6. 用户收藏列表
app.get('/api/user/collections', (req, res) => {
  const list = articles.filter(a => userCollections.includes(a.id));
  res.json({ code: 0, data: list });
});

app.listen(port, () => {
  console.log(`CIDI Mock Server running at http://localhost:${port}`);
});