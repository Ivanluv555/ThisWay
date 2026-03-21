const app = getApp();

Page({
  // 点击“微信一键登录”按钮触发
  handleWxLogin: function () {
    wx.showLoading({ title: '安全登录中...' });

    // 【关键】直接调用云函数 'login'
    // 不需要 wx.login 拿 code，也不需要 AppSecret
    wx.cloud.callFunction({
        name: 'login',
        success: res => {
            console.log('云登录成功', res.result);
            
            const { openid, userInfo } = res.result;
            
            // 1. 保存到全局
            app.globalData.userInfo = userInfo;
            
            // 2. 持久化存储 (下次进来可以自动登录)
            wx.setStorageSync('token', 'cloud_token_' + openid); // 模拟 token
            wx.setStorageSync('userInfo', userInfo);
            
            wx.hideLoading();
            wx.showToast({ title: '欢迎回来', icon: 'success' });
            
            // 3. 返回上一页
            setTimeout(() => {
                wx.navigateBack();
            }, 1000);
        },
        fail: err => {
            console.error('云登录失败', err);
            wx.hideLoading();
            wx.showToast({ title: '登录服务异常', icon: 'none' });
        }
    })
  }
})