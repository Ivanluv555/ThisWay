const app = getApp();
Page({
  data: {
    username: '',
    password: ''
  },
  onInput: function (e) {
    this.setData({
      [e.currentTarget.dataset.field]: e.detail.value
    });
  },
  handleLogin: function () {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/user/login`,
      method: 'POST',
      data: {
        username: this.data.username,
        password: this.data.password
      },
      success: (res) => {
        if (res.data && res.data.token) {
          wx.setStorageSync('token', res.data.token);
          app.globalData.userInfo = res.data.userInfo;
          wx.navigateBack();
        } else {
          wx.showToast({
            title: '登录失败',
            icon: 'none'
          });
        }
      },
      fail: () => {
        wx.showToast({
          title: '请求失败',
          icon: 'none'
        });
      }
    });
  }
})