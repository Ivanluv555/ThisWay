// app.js
App({
  onLaunch() {
    // 可以在这里检查登录态
  },
  globalData: {
    userInfo: null,
    apiBaseUrl: 'http://localhost:8080' // 您的Spring Boot后端地址
  }
})