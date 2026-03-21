const app = getApp();

Page({
  data: { 
    list: [],
    loading: true 
  },

  onShow: function () { 
    // 每次显示页面时都刷新，保证收藏状态最新
    this.fetchData(); 
  },
  
  fetchData: function () {
    // 判断是否登录
    if (!app.globalData.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      this.setData({ list: [] });
      return;
    }

    wx.showLoading({ title: '加载收藏...' });

    // 【核心修改】调用云函数 cidi-api
    wx.cloud.callFunction({
      name: 'cidi-api',
      data: { 
        action: 'getUserCollections' 
      }
    }).then(res => {
      wx.hideLoading();
      const result = res.result;
      
      if (result.code === 0) {
        // 数据映射：转换成 UI 需要的格式
        const uiList = result.data.map(item => ({
          id: item._id, // 云数据库 ID
          title: item.title,
          coverUrl: item.images[0], // 取第一张图
          author: item.author,
          date: '收藏于近日' // 这里可以加个时间格式化函数
        }));

        this.setData({ 
          list: uiList,
          loading: false 
        });
      }
    }).catch(err => {
      wx.hideLoading();
      console.error('加载收藏失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    });
  },
  
  goDetail: function(e) {
    wx.navigateTo({ url: `/pages_sub/article/article?id=${e.currentTarget.dataset.id}` });
  }
})