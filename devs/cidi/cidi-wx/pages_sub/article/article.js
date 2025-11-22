const app = getApp();
Page({
  data: { article: null, loading: true },
  onLoad: function (options) { this.fetchDetail(options.id); },
  
  fetchDetail: function (id) {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/article/${id}`,
      success: (res) => {
        if(res.data.code === 0) this.setData({ article: res.data.data, loading: false });
      }
    });
  }
})