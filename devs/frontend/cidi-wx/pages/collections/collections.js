const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: { 
    list: [],
    loading: true 
  },

  onShow: function () { 
    // 每次显示页面时都刷新，保证收藏状态最新
    this.fetchData(); 
  },
  
  fetchData: function () {
    // 判断是否登录
    if (!app.globalData.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      this.setData({ list: [] });
      return;
    }

    wx.showLoading({ title: '加载收藏...' });

    cidiApi.getUserCollections(app.globalData.userInfo.id).then(list => {
      wx.hideLoading();
      this.setData({ 
        list,
        loading: false 
      });
    }).catch(err => {
      wx.hideLoading();
      console.error('加载收藏失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    });
  },
  
  goDetail: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  }
})