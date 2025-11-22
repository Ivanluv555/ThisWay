const app = getApp();
Page({
  data: { list: [] },
  onShow: function () { this.fetchData(); },
  
  fetchData: function () {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/user/collections`,
      success: (res) => {
        if(res.data.code === 0) this.setData({ list: res.data.data });
      }
    });
  },
  goDetail: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  }
})