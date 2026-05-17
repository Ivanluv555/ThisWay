const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: { 
    greeting: 'Find your\nNext Journey.',

    categories: ['推荐', '攻略', '美食', '摄影', '避坑'],
    activeCategory: 0,

    contentList: [],
    fullContentList: [],
    isPullRefreshing: false,
    headerStyle: '',
    heroAreaStyle: '',
    titleStyle: '',
    searchStyle: ''
  },

  _lastScrollTick: 0,
  _refreshStartAt: 0,
  _refreshVisualTimer: null,

  onPageScroll: function(e) {
    const now = Date.now();
    if (now - this._lastScrollTick < 16) return;
    this._lastScrollTick = now;

    const progress = Math.min(1, Math.max(0, (e.scrollTop || 0) / 120));
    this.setSmoothHeader(progress);
  },

  setSmoothHeader(progress) {
    const headerPad = 16.8 - 7.2 * progress;
    const heroTop = 36 - 21.6 * progress;
    const heroBottom = 12 - 7.2 * progress;
    const titleSize = 72 - 31.2 * progress;
    const titleMarginBottom = 36 - 28.8 * progress;
    const titleScale = 1 - 0.18 * progress;
    const searchPaddingV = 28.8 - 12 * progress;
    const searchRadius = 48 - 9.6 * progress;
    const shadowOpacity = 0.03 + 0.07 * progress;

    this.setData({
      headerStyle: `padding-bottom:${headerPad}rpx;box-shadow:0 4rpx 20rpx rgba(0,0,0,${shadowOpacity.toFixed(3)});`,
      heroAreaStyle: `padding:${heroTop}rpx 48rpx ${heroBottom}rpx;`,
      titleStyle: `font-size:${titleSize}rpx;margin-bottom:${titleMarginBottom}rpx;transform:scale(${titleScale.toFixed(3)});transform-origin:left center;`,
      searchStyle: `padding:${searchPaddingV}rpx 36rpx;border-radius:${searchRadius}rpx;`
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
      this._refreshStartAt = Date.now();
      this.setData({ isPullRefreshing: true });
      this.fetchData(true); 
  },
  
  // 切换分类（目前仅做UI交互，不真实筛选数据）
  switchCategory: function(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.index });
    this.applyCategoryFilter();
  },

  fetchData: function (isRefresh = false) {
    cidiApi.getHomeFeed(20).then(uiList => {
      this.setData({ fullContentList: uiList || [] });
      this.applyCategoryFilter();
    }).catch(err => {
      console.error(err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    }).finally(() => {
        if (isRefresh) {
          wx.stopPullDownRefresh();
          const elapsed = Date.now() - (this._refreshStartAt || Date.now());
          const delay = Math.max(0, 550 - elapsed);
          if (this._refreshVisualTimer) {
            clearTimeout(this._refreshVisualTimer);
          }
          this._refreshVisualTimer = setTimeout(() => {
            this.setData({ isPullRefreshing: false });
            this._refreshVisualTimer = null;
          }, delay);
        }
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
  },

  onUnload() {
    if (this._refreshVisualTimer) {
      clearTimeout(this._refreshVisualTimer);
      this._refreshVisualTimer = null;
    }
  }
})