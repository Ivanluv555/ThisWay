const app = getApp();

Page({
  data: {
    keyword: '',
    historyList: [],
    hasResult: false,
    results: { articles: [], locations: [] }
  },

  onLoad: function () {
    // 1. 读取本地历史记录
    const history = wx.getStorageSync('search_history') || [];
    this.setData({ historyList: history });
  },

  onInput: function(e) {
    this.setData({ keyword: e.detail.value });
    if (!e.detail.value) {
      this.setData({ hasResult: false }); // 清空搜索状态
    }
  },

  clearInput: function() {
    this.setData({ keyword: '', hasResult: false });
  },

  goBack: function() {
    wx.navigateBack();
  },

  // 点击历史标签
  tapHistory: function(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ keyword: val });
    this.executeSearch(val);
  },

  // 回车搜索
  doSearch: function(e) {
    this.executeSearch(e.detail.value);
  },

  // 核心搜索逻辑
  executeSearch: function(keyword) {
    if (!keyword || keyword.trim() === '') return;

    // 1. 保存历史记录 (去重 + 截取前10条)
    let history = this.data.historyList;
    history = history.filter(item => item !== keyword);
    history.unshift(keyword);
    if (history.length > 10) history.pop();
    
    this.setData({ historyList: history });
    wx.setStorageSync('search_history', history);

    // 2. 调用云函数
    wx.showLoading({ title: '搜索中...' });
    
    wx.cloud.callFunction({
      name: 'getMockData',
      data: { type: 'search', keyword: keyword }
    }).then(res => {
      wx.hideLoading();
      if (res.result.code === 0) {
        this.setData({
          hasResult: true,
          results: res.result.data
        });
      }
    }).catch(err => {
      wx.hideLoading();
      console.error(err);
    });
  },

  // 跳转逻辑
  goArticle: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  },
  goLocation: function(e) {
    wx.navigateTo({ url: `/pages_sub/location/location?id=${e.currentTarget.dataset.id}` });
  },
  clearHistory: function() {
    this.setData({ historyList: [] });
    wx.removeStorageSync('search_history');
  }
})