const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  mockLogin(profile = {}) {
    const nickName = profile.nickName || profile.nickname || `游客${Date.now().toString().slice(-4)}`;
    const userInfo = {
      id: Date.now(),
      username: nickName,
      avatarUrl: profile.avatarUrl || '',
      bio: '本地模式登录'
    };
    app.globalData.userInfo = userInfo;
    wx.setStorageSync('token', `mock-token-${Date.now()}`);
    wx.setStorageSync('userInfo', userInfo);
    wx.hideLoading();
    wx.showToast({ title: '已本地登录', icon: 'none' });
    setTimeout(() => {
      wx.navigateBack();
    }, 800);
  },

  // 点击“微信一键登录”按钮触发
  handleWxLogin: function () {
    wx.showLoading({ title: '安全登录中...' });

    wx.getUserProfile({
      desc: '用于完善用户资料',
      success: (profileRes) => {
        wx.login({
          success: async (loginRes) => {
            try {
              const result = await cidiApi.loginWithWeChatCode(loginRes.code, profileRes.userInfo);
              app.globalData.userInfo = result.userInfo;
              wx.setStorageSync('token', result.token);
              wx.setStorageSync('userInfo', result.userInfo);

              wx.hideLoading();
              wx.showToast({ title: '欢迎回来', icon: 'success' });
              setTimeout(() => {
                wx.navigateBack();
              }, 1000);
            } catch (err) {
              console.error('后端登录失败', err);
              this.mockLogin(profileRes.userInfo);
            }
          },
          fail: (err) => {
            console.error('wx.login 失败', err);
            wx.hideLoading();
            wx.showToast({ title: '微信登录失败', icon: 'none' });
          }
        });
      },
      fail: (err) => {
        console.error('获取用户信息失败', err);
        wx.hideLoading();
        wx.showToast({ title: '已取消登录', icon: 'none' });
      }
    });
  }
})