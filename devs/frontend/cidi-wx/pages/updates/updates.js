const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: { leftList: [], rightList: [] },
  
  // 使用 onShow 替代 onLoad，确保每次发完贴回来能自动刷新看到新贴
  onShow: function () { 
    this.fetchData(); 
  },
  
  fetchData: function () {
    wx.showNavigationBarLoading(); // 顶部加载转圈
    cidiApi.getUpdatesFeed(20).then(list => {
      wx.hideNavigationBarLoading();
      
      // 数据映射：适配瀑布流需要的字段
      const uiList = list.map(item => ({
        id: item.id,
        imageUrl: item.coverUrl,
        title: item.title,
        authorAvatar: item.authorAvatar,
        likes: item.likeCount || item.likes || 0
      }));

      // 简单的左右分栏算法
      const left = [], right = [];
      uiList.forEach((item, i) => (i % 2 === 0 ? left : right).push(item));
      this.setData({ leftList: left, rightList: right });

    }).catch(err => {
      wx.hideNavigationBarLoading();
      console.error(err);
      wx.showToast({ title: '加载失败', icon: 'none' });
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