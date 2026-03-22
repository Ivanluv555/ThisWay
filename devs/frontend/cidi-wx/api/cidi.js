const { request } = require('./http');

const DEFAULT_COVER = 'https://picsum.photos/640/480?blur=1';

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

async function loadFeedMaterials() {
  const [posts, users, postImages] = await Promise.all([
    request({ path: '/api/posts' }),
    request({ path: '/api/users' }),
    request({ path: '/api/post-images' })
  ]);
  return { posts: posts || [], users: users || [], postImages: postImages || [] };
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

function mapPostToCard(post, usersMap, postImageMap) {
  const user = usersMap[post.userId] || {};
  const images = (postImageMap[post.id] || []).map((item) => item.imageUrl).filter(Boolean);
  return {
    id: post.id,
    title: post.title,
    content: post.content,
    coverUrl: images[0] || DEFAULT_COVER,
    images,
    author: user.username || '匿名用户',
    authorAvatar: user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.username || 'U')}`,
    likes: post.likeCount || 0,
    likeCount: post.likeCount || 0,
    date: formatDateTime(post.createTime),
    createTime: post.createTime
  };
}

async function getHomeFeed(limit = 20) {
  const { posts, users, postImages } = await loadFeedMaterials();
  const usersMap = buildUsersMap(users);
  const postImageMap = buildPostImageMap(postImages);

  return posts
    .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
    .slice(0, limit)
    .map((post) => mapPostToCard(post, usersMap, postImageMap));
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

async function getLocationDetail(locationId) {
  const [location, locationImages] = await Promise.all([
    request({ path: `/api/locations/${locationId}` }),
    request({ path: '/api/location-images' })
  ]);
  const images = (locationImages || [])
    .filter((img) => `${img.locationId}` === `${locationId}`)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map((img) => img.imageUrl)
    .filter(Boolean);

  return {
    id: location.id,
    title: location.title,
    address: location.address || '',
    desc: `${location.category || '未知分类'} · 评分 ${location.rating || '-'} `,
    latitude: Number(location.latitude),
    longitude: Number(location.longitude),
    images: images.length ? images : [DEFAULT_COVER]
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
  const [post, users, postImages, actions] = await Promise.all([
    request({ path: `/api/posts/${postId}` }),
    request({ path: '/api/users' }),
    request({ path: '/api/post-images' }),
    request({ path: '/api/actions' })
  ]);

  const usersMap = buildUsersMap(users || []);
  const postImageMap = buildPostImageMap(postImages || []);
  const mapped = mapPostToCard(post, usersMap, postImageMap);

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

async function createPost({ title, content, images, userId, categoryId = 1, locationId = null }) {
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

  const httpImages = (images || []).filter((item) => /^https?:\/\//i.test(item));
  if (httpImages.length) {
    await Promise.all(httpImages.map((imageUrl, index) => request({
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
  getFuturePrediction
};
