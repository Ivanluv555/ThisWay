<<<<<<< HEAD
const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    keyword: '',
    historyList: [],
    hotList: [
      { rank: 1, text: '城市周末微旅行', hot: '2.3w' },
      { rank: 2, text: '春季拍照打卡地', hot: '1.8w' },
      { rank: 3, text: '本地高分咖啡店', hot: '1.5w' },
      { rank: 4, text: '夜市美食攻略', hot: '1.2w' },
      { rank: 5, text: '治愈系散步路线', hot: '9.8k' }
    ],
    hasSearched: false,
    isLoading: false,
    errorMsg: '',
    results: { articles: [], locations: [] },
    activeTab: 'all'
  },

  _requestSerial: 0,

  onLoad: function () {
    const history = wx.getStorageSync('search_history') || [];
    this.setData({ historyList: history });

    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    if (userId) {
      cidiApi.getSearchHistory(userId).then((remoteHistory) => {
        if (remoteHistory.length) {
          this.setData({ historyList: remoteHistory });
          wx.setStorageSync('search_history', remoteHistory);
        }
      }).catch(() => {});
    }

    cidiApi.getHotSearchList().then((hotList) => {
      if (hotList.length) {
        this.setData({ hotList });
      }
    }).catch(() => {});
  },

  onInput: function(e) {
    const nextKeyword = e.detail.value;
    this.setData({ keyword: nextKeyword });
    if (!nextKeyword.trim()) {
      this.setData({
        hasSearched: false,
        isLoading: false,
        errorMsg: '',
        results: { articles: [], locations: [] }
      });
    }
  },

  clearInput: function() {
    this.setData({
      keyword: '',
      hasSearched: false,
      isLoading: false,
      errorMsg: '',
      results: { articles: [], locations: [] }
    });
  },

  goBack: function() {
    wx.navigateBack();
  },

  tapHistory: function(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ keyword: val });
    this.executeSearch(val);
  },

  tapHotItem: function(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ keyword: val });
    this.executeSearch(val);
  },

  doSearch: function(e) {
    const value = (e && e.detail && e.detail.value) ? e.detail.value : this.data.keyword;
    this.executeSearch(value);
  },

  executeSearch: function(keyword) {
    const normalizedKeyword = (keyword || '').trim();
    if (!normalizedKeyword) return;

    const serial = ++this._requestSerial;
    this.persistHistory(normalizedKeyword);

    this.setData({
      hasSearched: true,
      isLoading: true,
      errorMsg: '',
      activeTab: 'locations'
    });

    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    cidiApi.searchKeyword(normalizedKeyword, userId).then(resultData => {
      if (serial !== this._requestSerial) return;
      const locations = resultData.locations || [];
      const articles = resultData.articles || [];
      const activeTab = locations.length > 0 ? 'locations' : (articles.length > 0 ? 'articles' : 'locations');

      this.setData({
        results: { articles, locations },
        activeTab,
        errorMsg: ''
      });
    }).catch(err => {
      if (serial !== this._requestSerial) return;
      console.error(err);
      this.setData({
        results: { articles: [], locations: [] },
        errorMsg: '网络异常，请检查后重试'
      });
    }).finally(() => {
      if (serial !== this._requestSerial) return;
      this.setData({ isLoading: false });
    });
  },

  persistHistory: function(keyword) {
    let history = this.data.historyList || [];
    history = history.filter(item => item !== keyword);
    history.unshift(keyword);
    history = history.slice(0, 10);

    this.setData({ historyList: history });
    wx.setStorageSync('search_history', history);
  },

  switchTab: function(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab) {
      this.setData({ activeTab: tab });
    }
  },

  goArticle: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  },

  goLocation: function(e) {
    wx.navigateTo({ url: `/pages_sub/location/location?id=${e.currentTarget.dataset.id}` });
  },

  clearHistory: function() {
    this.setData({ historyList: [] });
    wx.removeStorageSync('search_history');
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    if (userId) {
      cidiApi.clearSearchHistory(userId).catch(() => {});
    }
  }
=======
const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    keyword: '',
    historyList: [],
    hotList: [
      { rank: 1, text: '城市周末微旅行', hot: '2.3w' },
      { rank: 2, text: '春季拍照打卡地', hot: '1.8w' },
      { rank: 3, text: '本地高分咖啡店', hot: '1.5w' },
      { rank: 4, text: '夜市美食攻略', hot: '1.2w' },
      { rank: 5, text: '治愈系散步路线', hot: '9.8k' }
    ],
    hasSearched: false,
    isLoading: false,
    errorMsg: '',
    results: { articles: [], locations: [] }
  },

  _requestSerial: 0,

  onLoad: function () {
    const history = wx.getStorageSync('search_history') || [];
    this.setData({ historyList: history });

    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    if (userId) {
      cidiApi.getSearchHistory(userId).then((remoteHistory) => {
        if (remoteHistory.length) {
          this.setData({ historyList: remoteHistory });
          wx.setStorageSync('search_history', remoteHistory);
        }
      }).catch(() => {});
    }

    cidiApi.getHotSearchList().then((hotList) => {
      if (hotList.length) {
        this.setData({ hotList });
      }
    }).catch(() => {});
  },

  onInput: function(e) {
    const nextKeyword = e.detail.value;
    this.setData({ keyword: nextKeyword });
    if (!nextKeyword.trim()) {
      this.setData({
        hasSearched: false,
        isLoading: false,
        errorMsg: '',
        results: { articles: [], locations: [] }
      });
    }
  },

  clearInput: function() {
    this.setData({
      keyword: '',
      hasSearched: false,
      isLoading: false,
      errorMsg: '',
      results: { articles: [], locations: [] }
    });
  },

  goBack: function() {
    wx.navigateBack();
  },

  tapHistory: function(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ keyword: val });
    this.executeSearch(val);
  },

  tapHotItem: function(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ keyword: val });
    this.executeSearch(val);
  },

  doSearch: function(e) {
    const value = (e && e.detail && e.detail.value) ? e.detail.value : this.data.keyword;
    this.executeSearch(value);
  },

  executeSearch: function(keyword) {
    const normalizedKeyword = (keyword || '').trim();
    if (!normalizedKeyword) return;

    const serial = ++this._requestSerial;
    this.persistHistory(normalizedKeyword);

    this.setData({
      hasSearched: true,
      isLoading: true,
      errorMsg: ''
    });

    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    cidiApi.searchKeyword(normalizedKeyword, userId).then(resultData => {
      if (serial !== this._requestSerial) return;
      this.setData({
        results: {
          articles: resultData.articles || [],
          locations: resultData.locations || []
        },
        errorMsg: ''
      });
    }).catch(err => {
      if (serial !== this._requestSerial) return;
      console.error(err);
      this.setData({
        results: { articles: [], locations: [] },
        errorMsg: '网络异常，请检查后重试'
      });
    }).finally(() => {
      if (serial !== this._requestSerial) return;
      this.setData({ isLoading: false });
    });
  },

  persistHistory: function(keyword) {
    let history = this.data.historyList || [];
    history = history.filter(item => item !== keyword);
    history.unshift(keyword);
    history = history.slice(0, 10);

    this.setData({ historyList: history });
    wx.setStorageSync('search_history', history);
  },

  goArticle: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  },

  goLocation: function(e) {
    wx.navigateTo({ url: `/pages_sub/location/location?id=${e.currentTarget.dataset.id}` });
  },

  clearHistory: function() {
    this.setData({ historyList: [] });
    wx.removeStorageSync('search_history');
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    if (userId) {
      cidiApi.clearSearchHistory(userId).catch(() => {});
    }
  }
>>>>>>> origin/fullcircle
})