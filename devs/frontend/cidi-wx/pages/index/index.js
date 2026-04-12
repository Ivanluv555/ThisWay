const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: { 
    greeting: 'Find your\nNext Journey.',

    categories: ['推荐', '攻略', '美食', '摄影', '避坑'],
    activeCategory: 0,

    contentList: [],
    fullContentList: [],
    headerStyle: '',
    heroAreaStyle: '',
    titleStyle: '',
    searchStyle: ''
  },

  _lastScrollTick: 0,

  onPageScroll: function(e) {
    const now = Date.now();
    if (now - this._lastScrollTick < 16) return;
    this._lastScrollTick = now;

    const progress = Math.min(1, Math.max(0, (e.scrollTop || 0) / 120));
    this.setSmoothHeader(progress);
  },

  setSmoothHeader(progress) {
    const headerPad = 14 - 6 * progress;
    const heroTop = 30 - 18 * progress;
    const heroBottom = 10 - 6 * progress;
    const titleSize = 60 - 26 * progress;
    const titleMarginBottom = 30 - 24 * progress;
    const titleScale = 1 - 0.18 * progress;
    const searchPaddingV = 24 - 10 * progress;
    const searchRadius = 40 - 8 * progress;
    const shadowOpacity = 0.03 + 0.07 * progress;

    this.setData({
      headerStyle: `padding-bottom:${headerPad}rpx;box-shadow:0 4rpx 20rpx rgba(0,0,0,${shadowOpacity.toFixed(3)});`,
      heroAreaStyle: `padding:${heroTop}rpx 40rpx ${heroBottom}rpx;`,
      titleStyle: `font-size:${titleSize}rpx;margin-bottom:${titleMarginBottom}rpx;transform:scale(${titleScale.toFixed(3)});transform-origin:left center;`,
      searchStyle: `padding:${searchPaddingV}rpx 30rpx;border-radius:${searchRadius}rpx;`
    });
  },

  applyCategoryFilter() {
    const currentCategory = this.data.categories[this.data.activeCategory] || '推荐';
    if (currentCategory === '推荐') {
      this.setData({ contentList: this.data.fullContentList });
      return;
    }

    const filtered = (this.data.fullContentList || []).filter((item) => `${item.category || ''}`.includes(currentCategory));
    this.setData({ contentList: filtered });
  },

  onLoad: function () { this.fetchData(); },
  
  onPullDownRefresh: function() { 
      this.fetchData(true); 
  },
  
  // 切换分类（目前仅做UI交互，不真实筛选数据）
  switchCategory: function(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.index });
    this.applyCategoryFilter();
    wx.vibrateShort(); //以此增加一点点触感反馈
  },

  fetchData: function (isRefresh = false) {
    cidiApi.getHomeFeed(20).then(uiList => {
      this.setData({ fullContentList: uiList || [] });
      this.applyCategoryFilter();
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