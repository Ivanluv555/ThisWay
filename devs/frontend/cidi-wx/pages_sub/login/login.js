const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    avatarUrl: '',
    nickname: ''
  },

  onChooseAvatar(e) {
    this.setData({ avatarUrl: e.detail.avatarUrl });
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  onNicknameBlur(e) {
    // 用户点击键盘「使用微信昵称」时通过 blur 事件回填
    const val = e.detail.value;
    if (val) {
      this.setData({ nickname: val });
    }
  },

  mockLogin() {
    const nickname = this.data.nickname.trim() || `游客${Date.now().toString().slice(-4)}`;

    // 复用已有的本地用户 ID，避免每次登录换新身份
    let existing = null;
    try {
      existing = wx.getStorageSync('userInfo');
    } catch (e) {}

    const userId = (existing && existing.id) ? existing.id : Date.now();
    const userInfo = {
      id: userId,
      username: nickname,
      avatarUrl: this.data.avatarUrl || (existing && existing.avatarUrl) || '',
      bio: existing && existing.bio ? existing.bio : '记录足迹，分享感动'
    };

    app.globalData.userInfo = userInfo;
    wx.setStorageSync('token', `mock-token-${userId}`);
    wx.setStorageSync('userInfo', userInfo);
    wx.hideLoading();
    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      wx.navigateBack();
    }, 800);
  },

  handleLogin() {
    const nickname = (this.data.nickname || '').trim();
    if (!nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '登录中...', mask: true });

    wx.login({
      success: async (loginRes) => {
        try {
          const result = await cidiApi.loginWithWeChatCode(loginRes.code, {
            nickname,
            avatarUrl: this.data.avatarUrl
          });
          app.globalData.userInfo = result.userInfo;
          wx.setStorageSync('token', result.token);
          wx.setStorageSync('userInfo', result.userInfo);

          wx.hideLoading();
          wx.showToast({ title: '欢迎回来', icon: 'success' });
          setTimeout(() => {
            wx.navigateBack();
          }, 1000);
        } catch (err) {
          console.warn('后端暂不可用，使用本地登录', (err && err.message) || err);
          this.mockLogin();
        }
      },
      fail: (err) => {
        console.error('wx.login 失败', err);
        wx.hideLoading();
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      }
    });
  }
});
