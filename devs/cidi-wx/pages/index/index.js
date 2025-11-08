const app = getApp();
Page({
  data: {
    contentList: []
  },
  onLoad: function () {
    this.fetchHomePageData();
  },
  fetchHomePageData: function () {
    // 模拟数据
    this.setData({
      contentList: [
        { id: 1, coverUrl: '', title: '标题名称标题名称标题名称标题名称标题名称标题名称标题名', author: '作者', date: '2024/03/25', collections: 98 },
        { id: 2, coverUrl: '', title: '标题名称标题名称标题名称标题名称标题名称标题名称标题名', author: '作者', date: '2024/03/25', collections: 98 }
      ]
    });
    // wx.request({
    //   url: `${app.globalData.apiBaseUrl}/api/content/home`,
    //   success: (res) => {
    //     this.setData({ contentList: res.data });
    //   }
    // });
  }
})