const app = getApp();

Page({
  data: { leftList: [], rightList: [] },
  
  // 使用 onShow 替代 onLoad，确保每次发完贴回来能自动刷新看到新贴
  onShow: function () { 
    this.fetchData(); 
  },
  
  fetchData: function () {
    wx.showNavigationBarLoading(); // 顶部加载转圈
    wx.cloud.callFunction({
      name: 'cidi-api', // 记得确认你的云函数叫 cidi-api
      data: { 
        action: 'getHomeList', // 复用获取列表的接口
        payload: { limit: 20 } 
      }
    }).then(res => {
      wx.hideNavigationBarLoading();
      const list = res.result.data || [];
      
      // 数据映射：适配瀑布流需要的字段
      const uiList = list.map(item => ({
        id: item._id,
        imageUrl: item.images[0], // 取第一张图
        title: item.title,
        authorAvatar: item.authorAvatar,
        likes: item.likeCount || 0
      }));

      // 简单的左右分栏算法
      const left = [], right = [];
      uiList.forEach((item, i) => (i % 2 === 0 ? left : right).push(item));
      this.setData({ leftList: left, rightList: right });

    }).catch(err => {
      wx.hideNavigationBarLoading();
      console.error(err);
    });
  },
  
  // 【新增】跳转去发布页
  goPublish: function() {
    // 检查登录状态 (可选)
    if (!app.globalData.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => wx.navigateTo({ url: '/pages_sub/login/login' }), 1000);
      return;
    }
    wx.navigateTo({ url: '/pages_sub/publish/publish' });
  },

  goDetail: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
})