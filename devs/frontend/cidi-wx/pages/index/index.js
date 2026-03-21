const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: { 
    greeting: 'Find your\nNext Journey.',

    categories: ['推荐', '附近', '热门', '美食', '视频', '门票'],
    activeCategory: 0,

    contentList: [],
    isScrolled: false
  },

  onPageScroll: function(e) {
    // 阈值设为 30pt (约60rpx)，超过则收缩
    const threshold = 30;
    if (e.scrollTop > threshold && !this.data.isScrolled) {
      this.setData({ isScrolled: true });
    } else if (e.scrollTop <= threshold && this.data.isScrolled) {
      this.setData({ isScrolled: false });
    }
  },

  onLoad: function () { this.fetchData(); },
  
  onPullDownRefresh: function() { 
      this.fetchData(true); 
  },
  
  // 切换分类（目前仅做UI交互，不真实筛选数据）
  switchCategory: function(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.index });
    // 这里未来可以调用 fetchData 传参筛选
    wx.vibrateShort(); //以此增加一点点触感反馈
  },

  fetchData: function (isRefresh = false) {
    cidiApi.getHomeFeed(20).then(uiList => {
      this.setData({ contentList: uiList });
    }).catch(err => {
      console.error(err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    }).finally(() => {
        if (isRefresh) wx.stopPullDownRefresh();
    });
},
  
  goDetail: function(e) {
    const id = e.currentTarget.dataset.id;
    if (id) {
        wx.navigateTo({ url: `/pages_sub/article/article?id=${id}` });
    }
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
})