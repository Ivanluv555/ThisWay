const { request, upload } = require('./http');

const DEFAULT_COVER = 'https://picsum.photos/640/480?blur=1';
const POST_EXTRA_KEY = 'post_extra_meta_map';
const LOCAL_SCENIC_PREFIX = 'local-scenic-';
const LOCAL_SCENIC_POSTS = [
  {
    id: `${LOCAL_SCENIC_PREFIX}hongyadong`,
    title: '洪崖洞夜景打卡全攻略：灯火层叠的山城名片',
    content:
      '洪崖洞依山而建，夜幕亮灯后像一座现实版宫崎骏城堡。建议傍晚先到千厮门大桥拍全景，再下到沿江步道拍近景，错峰时间为工作日下午4:30前。',
    coverUrl: '/images/hongyadong.jpg',
    location: '重庆 洪崖洞',
    category: '摄影',
    likes: 268
  },
  {
    id: `${LOCAL_SCENIC_PREFIX}ciqikou`,
    title: '磁器口古镇慢游路线：老街烟火与地道小吃',
    content:
      '磁器口适合步行慢逛，主街看热闹、支巷找安静。推荐从东门进入，依次打卡老茶馆、手作店和临江观景点，陈麻花与毛血旺可避开饭点排队。',
    coverUrl: '/images/ciqikou.jpg',
    location: '重庆 磁器口古镇',
    category: '美食',
    likes: 194
  },
  {
    id: `${LOCAL_SCENIC_PREFIX}changjiangsuodao`,
    title: '长江索道体验指南：俯瞰两江与城市天际线',
    content:
      '长江索道是感受重庆立体交通的经典方式。建议南站上车、北站下车，单程体验更从容。晴天上午光线通透，阴天傍晚更有雾都层次感。',
    coverUrl: '/images/changjiangsuodao.jpg',
    location: '重庆 长江索道',
    category: '攻略',
    likes: 156
  },
  {
    id: `${LOCAL_SCENIC_PREFIX}wulong`,
    title: '武隆天生三桥一日游：喀斯特地貌震撼现场',
    content:
      '武隆天生三桥以峡谷与天坑闻名，步道上下坡较多，建议穿防滑运动鞋。路线可先三桥后地缝，节省体力且观景体验更完整。',
    coverUrl: '/images/wulong.jpg',
    location: '重庆 武隆天生三桥',
    category: '推荐',
    likes: 221
  }
];

const SCENIC_META_RULES = [
  {
    keywords: ['洪崖洞', 'hongyadong'],
    location: '重庆 洪崖洞',
    coverUrl: '/images/hongyadong.jpg',
    intro: '洪崖洞依山临江，夜景层叠灯火是重庆最具辨识度的地标之一。'
  },
  {
    keywords: ['磁器口', 'ciqikou'],
    location: '重庆 磁器口古镇',
    coverUrl: '/images/ciqikou.jpg',
    intro: '磁器口古镇兼具老街风貌与市井烟火，适合慢节奏步行体验。'
  },
  {
    keywords: ['长江索道', '索道', 'changjiangsuodao'],
    location: '重庆 长江索道',
    coverUrl: '/images/changjiangsuodao.jpg',
    intro: '长江索道横跨两岸，短时间即可获得山城立体城市景观视角。'
  },
  {
    keywords: ['武隆', '天生三桥', 'wulong'],
    location: '重庆 武隆天生三桥',
    coverUrl: '/images/wulong.jpg',
    intro: '武隆天生三桥以喀斯特地貌闻名，峡谷与天坑景观极具震撼力。'
  }
];
const DEFAULT_CATEGORIES = [
  { id: 1, name: '推荐' },
  { id: 2, name: '攻略' },
  { id: 3, name: '美食' },
  { id: 4, name: '摄影' },
  { id: 5, name: '避坑' }
];

function safeRequest(path, fallback = []) {
  return request({ path }).catch(() => fallback);
}

function readPostExtrasMap() {
  try {
    return wx.getStorageSync(POST_EXTRA_KEY) || {};
  } catch (e) {
    return {};
  }
}

function writePostExtrasMap(map) {
  try {
    wx.setStorageSync(POST_EXTRA_KEY, map || {});
  } catch (e) {}
}

function upsertPostExtra(postId, extra = {}) {
  if (!postId) return;
  const current = readPostExtrasMap();
  current[postId] = {
    ...(current[postId] || {}),
    ...extra,
    updatedAt: new Date().toISOString()
  };
  writePostExtrasMap(current);
}

function buildLocationsMap(locations) {
  const map = {};
  (locations || []).forEach((item) => {
    map[item.id] = item;
  });
  return map;
}

function buildCategoriesMap(categories) {
  const map = {};
  (categories || []).forEach((item) => {
    map[item.id] = item;
  });
  return map;
}

function toRad(value) {
  return (Number(value) * Math.PI) / 180;
}

function calcDistanceKm(lat1, lng1, lat2, lng2) {
  const a1 = Number(lat1);
  const b1 = Number(lng1);
  const a2 = Number(lat2);
  const b2 = Number(lng2);
  if ([a1, b1, a2, b2].some((v) => Number.isNaN(v))) return null;
  const r = 6371;
  const dLat = toRad(a2 - a1);
  const dLng = toRad(b2 - b1);
  const m =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(a1)) * Math.cos(toRad(a2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(m), Math.sqrt(1 - m));
  return r * c;
}

function formatDistance(km) {
  if (km == null) return '';
  if (km < 1) return `${Math.round(km * 1000)}m`;
  return `${km.toFixed(1)}km`;
}

function formatDateTime(value) {
  if (!value) return '刚刚';
  try {
    const date = new Date(value);
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${month}-${day}`;
  } catch (e) {
    return '刚刚';
  }
}

function getDisplayUser(user) {
  return {
    id: user.id,
    username: user.username || '匿名用户',
    avatarUrl: user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.username || 'User')}`,
    bio: user.bio || '登录探索更多精彩'
  };
}

function buildUsersMap(users) {
  const map = {};
  (users || []).forEach((item) => {
    map[item.id] = item;
  });
  return map;
}

function isLocalScenicPostId(postId) {
  return `${postId || ''}`.startsWith(LOCAL_SCENIC_PREFIX);
}

function buildLocalScenicFeed(limit = 20) {
  const now = Date.now();
  return LOCAL_SCENIC_POSTS.slice(0, limit).map((item, index) => ({
    id: item.id,
    title: item.title,
    content: item.content,
    summary: item.content,
    coverUrl: item.coverUrl,
    images: [item.coverUrl],
    author: '此地编辑部',
    authorAvatar: 'https://ui-avatars.com/api/?name=CIDI',
    likes: item.likes,
    likeCount: item.likes,
    categoryId: null,
    category: item.category,
    locationId: null,
    location: item.location,
    geo: null,
    date: formatDateTime(new Date(now - index * 3600 * 1000).toISOString()),
    createTime: new Date(now - index * 3600 * 1000).toISOString()
  }));
}

function resolveScenicMeta(rawText = '') {
  const text = `${rawText}`.toLowerCase();
  return SCENIC_META_RULES.find((rule) =>
    (rule.keywords || []).some((keyword) => text.includes(`${keyword}`.toLowerCase()))
  ) || null;
}

async function loadFeedMaterials() {
  const [posts, users, postImages, locations, categories] = await Promise.all([
    safeRequest('/api/posts', []),
    safeRequest('/api/users', []),
    safeRequest('/api/post-images', []),
    safeRequest('/api/locations', []),
    safeRequest('/api/categories', DEFAULT_CATEGORIES)
  ]);
  return {
    posts: posts || [],
    users: users || [],
    postImages: postImages || [],
    locations: locations || [],
    categories: categories || []
  };
}

function buildPostImageMap(postImages) {
  const map = {};
  (postImages || []).forEach((img) => {
    if (!map[img.postId]) map[img.postId] = [];
    map[img.postId].push(img);
  });
  Object.keys(map).forEach((key) => {
    map[key].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  });
  return map;
}

function getUploadPath() {
  const app = getApp && getApp();
  const uploadPath = app && app.globalData && app.globalData.uploadApiPath;
  return uploadPath || '/api/files/upload';
}

function parseUploadUrl(result) {
  if (!result) return '';
  if (typeof result === 'string') return result;
  if (result.url) return result.url;
  if (result.imageUrl) return result.imageUrl;
  if (result.data && result.data.url) return result.data.url;
  if (result.data && result.data.imageUrl) return result.data.imageUrl;
  return '';
}

function mapPostToCard(post, usersMap, postImageMap, locationsMap = {}, categoriesMap = {}, postExtras = {}) {
  const user = usersMap[post.userId] || {};
  const images = (postImageMap[post.id] || []).map((item) => item.imageUrl).filter(Boolean);
  const location = locationsMap[post.locationId] || {};
  const category = categoriesMap[post.categoryId] || {};
  const extra = postExtras[post.id] || {};
  const scenicMeta = resolveScenicMeta(
    `${post.title || ''} ${post.content || ''} ${location.title || ''} ${extra.locationName || ''}`
  );
  const locationName = location.title || extra.locationName || (scenicMeta && scenicMeta.location) || '';
  const coverUrl = images[0] || (scenicMeta && scenicMeta.coverUrl) || DEFAULT_COVER;
  const summary =
    (extra.summary && `${extra.summary}`.trim()) ||
    (scenicMeta && scenicMeta.intro) ||
    `${post.content || ''}`.trim();

  return {
    id: post.id,
    title: post.title,
    content: post.content,
    summary,
    coverUrl,
    images,
    author: user.username || '匿名用户',
    authorAvatar: user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.username || 'U')}`,
    likes: post.likeCount || 0,
    likeCount: post.likeCount || 0,
    categoryId: post.categoryId || null,
    category: category.name || extra.categoryName || '推荐',
    locationId: post.locationId || null,
    location: locationName,
    geo: extra.geo || null,
    date: formatDateTime(post.createTime),
    createTime: post.createTime
  };
}

async function getHomeFeed(limit = 20) {
  const { posts, users, postImages, locations, categories } = await loadFeedMaterials();
  const usersMap = buildUsersMap(users);
  const postImageMap = buildPostImageMap(postImages);
  const locationsMap = buildLocationsMap(locations);
  const categoriesMap = buildCategoriesMap(categories);
  const postExtras = readPostExtrasMap();

  const mapped = posts
    .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
    .slice(0, limit)
    .map((post) => mapPostToCard(post, usersMap, postImageMap, locationsMap, categoriesMap, postExtras));

  return mapped.length ? mapped : buildLocalScenicFeed(limit);
}

async function getUpdatesFeed(limit = 20) {
  return getHomeFeed(limit);
}

async function getMapLocations() {
  const [locations, locationImages] = await Promise.all([
    request({ path: '/api/locations' }),
    request({ path: '/api/location-images' })
  ]);

  const imagesMap = {};
  (locationImages || []).forEach((item) => {
    if (!imagesMap[item.locationId]) imagesMap[item.locationId] = [];
    imagesMap[item.locationId].push(item);
  });
  Object.keys(imagesMap).forEach((key) => {
    imagesMap[key].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  });

  return (locations || []).map((item) => {
    const imgs = (imagesMap[item.id] || []).map((img) => img.imageUrl).filter(Boolean);
    return {
      id: item.id,
      markerType: 'spot',
      latitude: Number(item.latitude),
      longitude: Number(item.longitude),
      title: item.title,
      address: item.address || '',
      desc: `${item.category || '未知分类'} · 评分 ${item.rating || '-'} `,
      images: imgs,
      image: imgs[0] || DEFAULT_COVER,
      callout: {
        content: item.title,
        display: 'NONE'
      }
    };
  });
}

async function getMapPostMarkers(limit = 40) {
  const [posts, users, locations, postImages] = await Promise.all([
    safeRequest('/api/posts', []),
    safeRequest('/api/users', []),
    safeRequest('/api/locations', []),
    safeRequest('/api/post-images', [])
  ]);

  const usersMap = buildUsersMap(users);
  const locationsMap = buildLocationsMap(locations);
  const postImageMap = buildPostImageMap(postImages);
  const postExtras = readPostExtrasMap();

  return (posts || [])
    .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
    .slice(0, limit)
    .map((post) => {
      const extra = postExtras[post.id] || {};
      const loc = locationsMap[post.locationId] || {};
      const geo = extra.geo || {};
      const lat = Number(loc.latitude || geo.latitude);
      const lng = Number(loc.longitude || geo.longitude);
      if (Number.isNaN(lat) || Number.isNaN(lng)) return null;

      const card = mapPostToCard(post, usersMap, postImageMap, locationsMap, {}, postExtras);
      return {
        id: 1000000 + Number(post.id),
        markerType: 'post',
        postId: post.id,
        latitude: lat,
        longitude: lng,
        title: card.title,
        address: card.location || extra.ipLocation || '动态位置',
        image: card.coverUrl,
        desc: `${card.author} · ${card.category || '推荐'}`,
        callout: {
          content: `帖子: ${card.title}`,
          display: 'NONE'
        }
      };
    })
    .filter(Boolean);
}

async function getLocationDetail(locationId) {
  const [location, locationImages, predictions] = await Promise.all([
    request({ path: `/api/locations/${locationId}` }),
    request({ path: '/api/location-images' }),
    request({ path: '/api/spot-traffic-predictions' }).catch(() => [])
  ]);
  const images = (locationImages || [])
    .filter((img) => `${img.locationId}` === `${locationId}`)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map((img) => img.imageUrl)
    .filter(Boolean);

  const spotPredictions = (predictions || [])
    .filter((item) => `${item.spotId}` === `${locationId}`)
    .sort((a, b) => new Date(b.predictTime || 0) - new Date(a.predictTime || 0));
  const latest = spotPredictions[0] || null;

  let prediction = null;
  if (latest) {
    const level = latest.congestionLevel != null ? Number(latest.congestionLevel) : Number(latest.trafficLevel || 0);
    prediction = {
      predictTime: formatDateTime(latest.predictTime),
      level,
      trend: latest.trend || '',
      desc: `预测人流密度 ${latest.crowdDensity || '-'}，请合理规划出行时间。`,
      visitorCount: latest.visitorCount || 0
    };
  }

  return {
    location: {
      id: location.id,
      title: location.title,
      address: location.address || '',
      description: location.description || '',
      category: location.category || '',
      rating: location.rating != null ? Number(location.rating) : null,
      latitude: Number(location.latitude),
      longitude: Number(location.longitude),
      images: images.length ? images : [DEFAULT_COVER]
    },
    prediction
  };
}

async function getHotSearchList() {
  const list = await request({ path: '/api/hot-searches' });
  return (list || [])
    .filter((item) => Number(item.isActive || 0) === 1)
    .sort((a, b) => (b.hotValue || 0) - (a.hotValue || 0))
    .slice(0, 10)
    .map((item, index) => ({
      rank: index + 1,
      text: item.keyword,
      hot: `${item.hotValue || 0}`
    }));
}

async function getCategories() {
  const categories = await safeRequest('/api/categories', DEFAULT_CATEGORIES);
  return (categories || []).map((item, index) => ({
    id: item.id || index + 1,
    name: item.name || item.title || `分类${index + 1}`
  }));
}

function getCurrentLocationInfo() {
  return new Promise((resolve) => {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (res) => {
        resolve({
          ok: true,
          latitude: Number(res.latitude),
          longitude: Number(res.longitude),
          ipLocation: '当前位置',
          source: 'gps'
        });
      },
      fail: () => {
        resolve({
          ok: false,
          latitude: null,
          longitude: null,
          ipLocation: '未授权定位',
          source: 'none'
        });
      }
    });
  });
}

async function getPublishOptions(currentGeo = null) {
  const [categories, locations] = await Promise.all([
    getCategories(),
    getMapLocations()
  ]);

  const withDistance = (locations || []).map((item) => {
    const distanceKm = currentGeo && currentGeo.latitude && currentGeo.longitude
      ? calcDistanceKm(currentGeo.latitude, currentGeo.longitude, item.latitude, item.longitude)
      : null;
    return {
      id: item.id,
      title: item.title,
      distanceKm,
      distance: formatDistance(distanceKm),
      latitude: item.latitude,
      longitude: item.longitude
    };
  });

  withDistance.sort((a, b) => {
    if (a.distanceKm == null && b.distanceKm == null) return 0;
    if (a.distanceKm == null) return 1;
    if (b.distanceKm == null) return -1;
    return a.distanceKm - b.distanceKm;
  });

  return {
    categories,
    nearbyLocations: withDistance.slice(0, 20)
  };
}

async function searchKeyword(keyword, userId) {
  const q = (keyword || '').trim().toLowerCase();
  if (!q) {
    return { articles: [], locations: [] };
  }

  const [posts, users, postImages, locations] = await Promise.all([
    request({ path: '/api/posts' }),
    request({ path: '/api/users' }),
    request({ path: '/api/post-images' }),
    request({ path: '/api/locations' })
  ]);

  const usersMap = buildUsersMap(users || []);
  const postImageMap = buildPostImageMap(postImages || []);

  const articles = (posts || [])
    .filter((post) => `${post.title || ''} ${post.content || ''}`.toLowerCase().includes(q))
    .slice(0, 20)
    .map((post) => mapPostToCard(post, usersMap, postImageMap));

  const locationResults = (locations || [])
    .filter((item) => `${item.title || ''} ${item.address || ''} ${item.category || ''}`.toLowerCase().includes(q))
    .slice(0, 20)
    .map((item) => ({
      id: item.id,
      title: item.title,
      address: item.address || ''
    }));

  if (userId) {
    request({
      path: '/api/search-history',
      method: 'POST',
      data: {
        userId,
        keyword,
        isDeleted: 0,
        createTime: new Date().toISOString()
      }
    }).catch(() => {});
  }

  return {
    articles,
    locations: locationResults
  };
}

async function getSearchHistory(userId) {
  if (!userId) return [];
  const list = await request({ path: '/api/search-history' });
  const result = (list || [])
    .filter((item) => `${item.userId}` === `${userId}` && Number(item.isDeleted || 0) === 0)
    .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
    .map((item) => item.keyword)
    .filter(Boolean);
  return [...new Set(result)].slice(0, 10);
}

async function clearSearchHistory(userId) {
  if (!userId) return;
  const list = await request({ path: '/api/search-history' });
  const ids = (list || [])
    .filter((item) => `${item.userId}` === `${userId}`)
    .map((item) => item.id);
  await Promise.all(ids.map((id) => request({ path: `/api/search-history/${id}`, method: 'DELETE' })));
}

async function loginWithWeChatCode(code, profile = {}) {
  const username = profile.nickName || profile.nickname || '微信用户';
  const avatarUrl = profile.avatarUrl || '';
  const loginRes = await request({
    path: '/api/auth/wechat/login',
    method: 'POST',
    data: {
      code,
      nickname: username,
      avatarUrl
    }
  });
  const user = loginRes && loginRes.user ? loginRes.user : {};

  return {
    token: (loginRes && loginRes.token) || '',
    userInfo: getDisplayUser(user)
  };
}

async function getArticleDetail(postId, currentUserId) {
  if (isLocalScenicPostId(postId)) {
    const localPost = buildLocalScenicFeed(50).find((item) => `${item.id}` === `${postId}`);
    if (localPost) {
      return {
        post: localPost,
        isLiked: false,
        isCollected: false
      };
    }
  }

  const [post, users, postImages, actions, locations, categories] = await Promise.all([
    request({ path: `/api/posts/${postId}` }),
    request({ path: '/api/users' }),
    request({ path: '/api/post-images' }),
    request({ path: '/api/actions' }),
    safeRequest('/api/locations', []),
    safeRequest('/api/categories', DEFAULT_CATEGORIES)
  ]);

  const usersMap = buildUsersMap(users || []);
  const postImageMap = buildPostImageMap(postImages || []);
  const locationsMap = buildLocationsMap(locations || []);
  const categoriesMap = buildCategoriesMap(categories || []);
  const postExtras = readPostExtrasMap();
  const mapped = mapPostToCard(post, usersMap, postImageMap, locationsMap, categoriesMap, postExtras);

  const myActions = (actions || []).filter((item) =>
    `${item.userId}` === `${currentUserId}` && `${item.postId}` === `${postId}`
  );

  return {
    post: mapped,
    isLiked: myActions.some((item) => item.type === 'like'),
    isCollected: myActions.some((item) => item.type === 'collect')
  };
}

async function getCommentsByPostId(postId) {
  if (isLocalScenicPostId(postId)) return [];

  const [comments, users] = await Promise.all([
    request({ path: '/api/comments' }),
    request({ path: '/api/users' })
  ]);
  const usersMap = buildUsersMap(users || []);

  return (comments || [])
    .filter((item) => `${item.postId}` === `${postId}`)
    .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
    .map((item) => {
      const user = usersMap[item.userId] || {};
      return {
        id: item.id,
        userInfo: {
          username: user.username || '匿名',
          avatarUrl: user.avatarUrl || ''
        },
        user: user.username || '匿名',
        text: item.content
      };
    });
}

async function togglePostAction({ postId, userId, type }) {
  if (isLocalScenicPostId(postId)) return true;

  const actions = await request({ path: '/api/actions' });
  const existed = (actions || []).find((item) =>
    `${item.userId}` === `${userId}` &&
    `${item.postId}` === `${postId}` &&
    item.type === type
  );

  if (existed) {
    await request({ path: `/api/actions/${existed.id}`, method: 'DELETE' });
    return false;
  }

  await request({
    path: '/api/actions',
    method: 'POST',
    data: {
      userId,
      postId: Number(postId),
      type,
      createTime: new Date().toISOString()
    }
  });
  return true;
}

async function addComment({ postId, userId, content }) {
  if (isLocalScenicPostId(postId)) {
    return {
      id: `local-comment-${Date.now()}`,
      postId,
      userId,
      content,
      createTime: new Date().toISOString()
    };
  }

  return request({
    path: '/api/comments',
    method: 'POST',
    data: {
      postId: Number(postId),
      userId,
      content,
      createTime: new Date().toISOString()
    }
  });
}

async function getUserCollections(userId) {
  const [actions, posts, users, postImages] = await Promise.all([
    request({ path: '/api/actions' }),
    request({ path: '/api/posts' }),
    request({ path: '/api/users' }),
    request({ path: '/api/post-images' })
  ]);
  const usersMap = buildUsersMap(users || []);
  const postImageMap = buildPostImageMap(postImages || []);
  const collectedIds = (actions || [])
    .filter((item) =>
      `${item.userId}` === `${userId}` &&
      item.type === 'collect'
    )
    .map((item) => item.postId);

  return (posts || [])
    .filter((item) => collectedIds.includes(item.id))
    .map((post) => {
      const mapped = mapPostToCard(post, usersMap, postImageMap);
      return {
        id: mapped.id,
        title: mapped.title,
        coverUrl: mapped.coverUrl,
        author: mapped.author,
        date: `收藏于 ${mapped.date}`
      };
    });
}

async function createPost({
  title,
  content,
  images,
  userId,
  categoryId = 1,
  categoryName = '推荐',
  locationId = null,
  locationName = '',
  geo = null,
  ipLocation = ''
}) {
  const now = new Date().toISOString();
  const post = await request({
    path: '/api/posts',
    method: 'POST',
    data: {
      userId,
      categoryId,
      locationId,
      title,
      content,
      viewCount: 0,
      likeCount: 0,
      createTime: now
    }
  });

  const remoteImages = (images || []).filter((item) => /^https?:\/\//i.test(item));
  const localImages = (images || []).filter((item) => !/^https?:\/\//i.test(item));

  let uploadedImages = [];
  if (localImages.length) {
    const uploadPath = getUploadPath();
    const uploadResults = await Promise.all(localImages.map((filePath) =>
      upload({ path: uploadPath, filePath, name: 'file', withAuth: true })
    ));
    uploadedImages = uploadResults.map((item) => parseUploadUrl(item)).filter(Boolean);
  }

  const allImageUrls = remoteImages.concat(uploadedImages);

  if (allImageUrls.length) {
    await Promise.all(allImageUrls.map((imageUrl, index) => request({
      path: '/api/post-images',
      method: 'POST',
      data: {
        postId: post.id,
        imageUrl,
        width: null,
        height: null,
        sortOrder: index + 1
      }
    })));
  }

  upsertPostExtra(post.id, {
    locationId,
    locationName,
    categoryId,
    categoryName,
    geo,
    ipLocation
  });

  return post;
}

async function getFuturePrediction(spotId) {
  const list = await request({ path: '/api/spot-traffic-predictions' });
  const sorted = (list || [])
    .filter((item) => `${item.spotId}` === `${spotId}`)
    .sort((a, b) => new Date(a.predictTime || 0) - new Date(b.predictTime || 0));
  const picked = sorted[0];
  if (!picked) {
    return {
      code: 1,
      message: '暂无预测数据'
    };
  }
  const level = Number(picked.trafficLevel || 0);
  return {
    code: 0,
    data: {
      predictTime: formatDateTime(picked.predictTime),
      level,
      trend: picked.trend || (level >= 3 ? '拥挤' : level >= 2 ? '较忙' : '通畅'),
      desc: `预测人流密度 ${picked.crowdDensity || '-'}，请合理规划出行时间。`
    }
  };
}

async function getNextDestinationForecast(spotId) {
  const locations = await safeRequest('/api/locations', []);
  const peers = (locations || []).filter((item) => `${item.id}` !== `${spotId}`);
  if (!peers.length) {
    return [];
  }

  const now = new Date();
  const rows = [];
  for (let i = 1; i <= 6; i += 1) {
    const date = new Date(now.getTime() + i * 60 * 60 * 1000);
    const seed = Number(spotId || 1) * 17 + date.getHours() * 31 + i * 11;
    const first = peers[seed % peers.length];
    const second = peers[(seed + 3) % peers.length];
    const third = peers[(seed + 7) % peers.length];

    const total = 60 + (seed % 80);
    const firstRate = 0.42;
    const secondRate = 0.33;
    const thirdRate = 0.25;

    rows.push({
      hour: `${`${date.getHours()}`.padStart(2, '0')}:00`,
      transitions: [
        {
          locationId: first.id,
          locationName: first.title,
          ratio: firstRate,
          ratioText: `${Math.round(firstRate * 100)}%`,
          users: Math.round(total * firstRate)
        },
        {
          locationId: second.id,
          locationName: second.title,
          ratio: secondRate,
          ratioText: `${Math.round(secondRate * 100)}%`,
          users: Math.round(total * secondRate)
        },
        {
          locationId: third.id,
          locationName: third.title,
          ratio: thirdRate,
          ratioText: `${Math.round(thirdRate * 100)}%`,
          users: Math.round(total * thirdRate)
        }
      ]
    });
  }

  return rows;
}

module.exports = {
  getHomeFeed,
  getUpdatesFeed,
  getMapLocations,
  getLocationDetail,
  getHotSearchList,
  searchKeyword,
  getSearchHistory,
  clearSearchHistory,
  loginWithWeChatCode,
  getArticleDetail,
  getCommentsByPostId,
  togglePostAction,
  addComment,
  getUserCollections,
  createPost,
  getFuturePrediction,
  getCategories,
  getCurrentLocationInfo,
  getPublishOptions,
  getMapPostMarkers,
  getNextDestinationForecast
};
