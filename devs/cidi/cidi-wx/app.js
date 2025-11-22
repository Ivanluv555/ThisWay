App({
  onLaunch() {
    if (!wx.cloud) {
      console.error('请使用 2.2.3 或以上的基础库以使用云能力');
    } else {
      wx.cloud.init({
        // 把这里换成你的环境ID (在开发者工具顶部工具栏"云开发"里查看)
        env: 'cloud1-4gmsl9yr4579f141', 
        traceUser: true,
      });
    }
  },
  globalData: {
    userInfo: null,
    // 注意：在开发者工具中需勾选“不校验合法域名”
    apiBaseUrl: 'http://localhost:3000' 
  }
})