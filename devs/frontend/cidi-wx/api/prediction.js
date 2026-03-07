// cidi-wx/api/prediction.js

// 🚧 架构开关：演示阶段设为 true，以后有了 Spring Boot 改为 false
const USE_CLOUD_MOCK = true; 

// 你的 Spring Boot 地址 (留着以后用)
const SPRING_BOOT_URL = 'https://your-domain.com/api'; 

export const getFuturePrediction = (spotId) => {
  if (USE_CLOUD_MOCK) {
    // --- 方案 A: 走云函数 (演示用) ---
    return wx.cloud.callFunction({
      name: 'getPrediction',
      data: { spotId }
    }).then(res => {
        // 云函数返回的是 res.result
        return res.result; 
    });
  } else {
    // --- 方案 B: 走 Spring Boot (未来用) ---
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${SPRING_BOOT_URL}/predict/${spotId}`,
        method: 'GET',
        success: res => resolve(res.data),
        fail: reject
      })
    });
  }
}