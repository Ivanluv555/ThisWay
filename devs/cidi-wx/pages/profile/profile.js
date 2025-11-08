const app = getApp();
Page({
  data: {
    userInfo: null
  },
  onShow: function () {
    // 检查登录态
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.navigateTo({
        url: '/pages_sub/login/login',
      });
    } else {
      this.fetchProfile();
    }
  },
  fetchProfile: function () {
    // 模拟数据
    this.setData({
      userInfo: {
        username: '用户名',
        bio: '一句话用户介绍',
        avatarUrl: ''
      }
    });
    // wx.request({
    //   url: `${app.globalData.apiBaseUrl}/api/user/profile`,
    //   header: { 'Authorization': 'Bearer ' + wx.getStorageSync('token') },
    //   success: (res) => {
    //     this.setData({ userInfo: res.data });
    //   }
    // });
  }
})