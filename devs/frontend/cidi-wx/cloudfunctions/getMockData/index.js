// cloudfunctions/getMockData/index.js
const cloud = require('wx-server-sdk')
cloud.init()

// ================== 1. 数据源定义 ==================

// 文章数据 (已应用头像修复)
const articles = [
  {
    id: 101,
    coverUrl: 'https://picsum.photos/400/300?random=1',
    title: '2024 城市指南：藏在老街区里的 5 家独立咖啡馆 ☕️',
    author: 'Cidi Editorial',
    date: '2 HOURS AGO',
    likes: 342,
    authorAvatar: 'https://ui-avatars.com/api/?name=Cidi+Editorial&background=random&color=fff'
  },
  {
    id: 102,
    coverUrl: 'https://picsum.photos/400/300?random=2',
    title: '逃离地心引力：周末去山野吸氧的徒步路线 🌲',
    author: 'Urban Explorer',
    date: 'YESTERDAY',
    likes: 899,
    authorAvatar: 'https://ui-avatars.com/api/?name=Urban+Explorer&background=random&color=fff'
  },
  {
    id: 103,
    coverUrl: 'https://picsum.photos/400/300?random=3',
    title: '胶片美学：如何用手机拍出王家卫电影感 📷',
    author: 'Vision Lab',
    date: '3 DAYS AGO',
    likes: 1205,
    authorAvatar: 'https://ui-avatars.com/api/?name=Vision+Lab&background=random&color=fff'
  },
  {
    id: 104,
    coverUrl: 'https://picsum.photos/400/300?random=4',
    title: '山城味蕾：为了这碗面，我愿意再飞一次重庆 🍜',
    author: 'Foodie Club',
    date: '1 WEEK AGO',
    likes: 2200,
    authorAvatar: 'https://ui-avatars.com/api/?name=Foodie+Club&background=random&color=fff'
  },
  {
    id: 105,
    coverUrl: 'https://picsum.photos/400/300?random=5',
    title: '极简主义生活：如何断舍离',
    author: 'Life Style',
    date: '2 WEEKS AGO',
    likes: 56,
    authorAvatar: 'https://ui-avatars.com/api/?name=Life+Style&background=random&color=fff'
  }
];

// 地点数据
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

// 用户收藏
const userCollections = [101, 103];

// ================== 2. 主逻辑入口 ==================

exports.main = async (event, context) => {
  const { type, id } = event;

  switch (type) {
    // ----------------- 1. 首页 -----------------
    case 'home':
      return { code: 0, data: articles };
    
    // ----------------- 2. 地图页 -----------------
    case 'map':
      return { code: 0, data: locations };
    
    // ----------------- 3. 动态页 (本次修复重点) -----------------
    case 'updates':
      const updatesList = articles.map(a => ({
        id: a.id,
        imageUrl: a.coverUrl,
        title: a.title,
        authorAvatar: a.authorAvatar,
        authorName: a.author,
        likes: a.likes
      }));
      return { code: 0, data: updatesList };

    // ----------------- 4. 收藏页 (本次修复重点) -----------------
    case 'collections':
      const collectList = articles.filter(a => userCollections.includes(a.id));
      return { code: 0, data: collectList };
      
    // ----------------- 5. 地点详情页 -----------------
    case 'location':
      const loc = locations.find(l => l.id == id);
      if (loc) {
        const detail = {
          ...loc,
          images: [
            `https://picsum.photos/600/400?random=${id}1`,
            `https://picsum.photos/600/400?random=${id}2`
          ]
        };
        return { code: 0, data: detail };
      } else {
        return { code: 404, msg: '地点不存在' };
      }

    // ----------------- 6. 文章详情页 -----------------
    case 'article':
       const article = articles.find(a => a.id == id);
       return { code: 0, data: article || {} };
       
    default:
      return { code: 404, msg: 'Unknown type' };

    // 【新增】搜索接口
    case 'search':
      if (!keyword || keyword.trim() === '') {
        return { code: 0, data: { articles: [], locations: [] } };
      }
      
      const searchKey = keyword.trim().toLowerCase(); // 转小写，支持不区分大小写
      
      // 1. 搜索文章 (匹配标题或作者)
      const matchedArticles = articles.filter(item => 
        item.title.toLowerCase().includes(searchKey) || 
        item.author.toLowerCase().includes(searchKey)
      );

      // 2. 搜索地点 (匹配名称、地址或描述)
      const matchedLocations = locations.filter(item => 
        item.title.toLowerCase().includes(searchKey) || 
        item.address.toLowerCase().includes(searchKey) || 
        item.desc.toLowerCase().includes(searchKey)
      );

      return {
        code: 0,
        data: {
          articles: matchedArticles,
          locations: matchedLocations
        }
      };
  }
}