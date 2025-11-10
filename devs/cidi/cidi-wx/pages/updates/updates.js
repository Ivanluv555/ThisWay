const app = getApp();
Page({
  data: {
    leftList: [],
    rightList: []
  },
  onLoad: function () {
    this.fetchUpdates();
  },
  fetchUpdates: function () {
    // 模拟数据
    const mockData = [
      { id: 1, imageUrl: '', title: '标题标题标题标题标题标题标题', authorAvatar: '', authorName: '用户名', likes: 120 },
      { id: 2, imageUrl: '', title: '标题标题标题标题标题标题标题', authorAvatar: '', authorName: '用户名', likes: 120 },
      { id: 3, imageUrl: '', title: '标题标题标题标题标题标题标题', authorAvatar: '', authorName: '用户名', likes: 120 },
      { id: 4, imageUrl: '', title: '标题标题标题标题标题标题标题', authorAvatar: '', authorName: '用户名', likes: 120 }
    ];
    // 简单的瀑布流分配
    const leftList = [];
    const rightList = [];
    mockData.forEach((item, index) => {
      if (index % 2 === 0) {
        leftList.push(item);
      } else {
        rightList.push(item);
      }
    });
    this.setData({ leftList, rightList });
    // wx.request({
    //   url: `${app.globalData.apiBaseUrl}/api/content/updates`,
    //   success: (res) => { ... }
    // });
  }
})