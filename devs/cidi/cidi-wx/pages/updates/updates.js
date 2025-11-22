const app = getApp();
Page({
  data: { leftList: [], rightList: [] },
  onLoad: function () { this.fetchData(); },
  
  fetchData: function () {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/updates/list`,
      success: (res) => {
        if (res.data.code === 0) {
          const list = res.data.data;
          const left = [], right = [];
          list.forEach((item, i) => (i % 2 === 0 ? left : right).push(item));
          this.setData({ leftList: left, rightList: right });
        }
      }
    });
  },
  
  goDetail: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  }
})