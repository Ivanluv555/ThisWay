const app = getApp();

Page({
  data: {
    isLogin: false,
    userInfo: {
      username: '点击登录',
      bio: '登录探索更多精彩',
      avatarUrl: '' // 空的时候显示默认图
    },
    // 新增：模拟社交数据
    stats: {
      likes: '2.4k',
      following: 128,
      followers: 893
    }
  },

  onShow: function() {
    this.updateUserProfile();
  },

  updateUserProfile: function() {
    let userInfo = app.globalData.userInfo;
    // globalData 读取失败时从 Storage 兜底
    if (!userInfo || !userInfo.id) {
      try {
        userInfo = wx.getStorageSync('userInfo');
        if (userInfo && userInfo.id) {
          app.globalData.userInfo = userInfo;
        }
      } catch (e) {}
    }
    if (userInfo && userInfo.id) {
      this.setData({
        userInfo: userInfo,
        isLogin: true
      });
    } else {
      this.setData({ isLogin: false });
    }
  },

  navTo: function(e) {
    const page = e.currentTarget.dataset.page;
    
    // 未登录拦截
    if (!this.data.isLogin && page !== 'settings' && page !== 'feedback') {
        wx.navigateTo({ url: '/pages_sub/login/login' });
        return;
    }

    if (page === 'collections') {
        wx.switchTab({ url: '/pages/collections/collections' });
    } else {
        wx.navigateTo({ url: `/pages_sub/${page}/${page}` });
    }
  },
  
  // 点击头像区域触发登录
  handleLogin: function() {
      if (!this.data.isLogin) {
          wx.navigateTo({ url: '/pages_sub/login/login' });
      }
  }
})