const app = getApp(); // 必须获取 App 实例

Page({
  data: {
    userInfo: {
      // 设置默认未登录状态或占位符
      username: '点击登录',
      bio: '登录后查看更多精彩内容',
      avatarUrl: '/images/default-avatar.png' // 建议放一张本地默认图，或者用网络图
    },
    isLogin: false
  },

  // 使用 onShow 而不是 onLoad，确保从登录页返回时能刷新数据
  onShow: function() {
    this.updateUserProfile();
  },

  updateUserProfile: function() {
    if (app.globalData.userInfo) {
      this.setData({
        userInfo: app.globalData.userInfo,
        isLogin: true
      });
    } else {
      // 处理未登录态
      this.setData({ isLogin: false });
    }
  },

  navTo: function(e) {
    const page = e.currentTarget.dataset.page;
    
    // 如果未登录且点击的不是通用设置，建议跳转登录
    if (!this.data.isLogin && page !== 'settings' && page !== 'feedback') {
        wx.navigateTo({ url: '/pages_sub/login/login' });
        return;
    }

    if (page === 'collections') {
        wx.switchTab({ url: '/pages/collections/collections' });
    } else {
        // 这里的路径拼接逻辑是正确的，前提是目录结构严格对应
        wx.navigateTo({ url: `/pages_sub/${page}/${page}` });
    }
  },
  
  // 增加点击头像登录的逻辑（可选）
  handleLogin: function() {
      if (!this.data.isLogin) {
          wx.navigateTo({ url: '/pages_sub/login/login' });
      }
  }
})