const app = getApp();
Page({
  data: { contentList: [] },
  onLoad: function () { this.fetchData(); },
  
  onPullDownRefresh: function() { 
      // 传入 true 标识这是手动刷新操作
      this.fetchData(true); 
  },
  
  fetchData: function (isRefresh = false) {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/home/list`,
      success: (res) => {
        if(res.data.code === 0) {
          this.setData({ contentList: res.data.data });
        }
      },
      fail: (err) => {
          wx.showToast({ title: '加载失败', icon: 'none' });
          console.error(err);
      },
      complete: () => {
        // 无论成功失败，都要停止下拉刷新
        if (isRefresh) wx.stopPullDownRefresh();
      }
    });
  },
  
  goDetail: function(e) {
    const id = e.currentTarget.dataset.id;
    // 增加防抖或 id 校验是更好的习惯
    if (id) {
        wx.navigateTo({ url: `/pages_sub/article/article?id=${id}` });
    }
  }
})