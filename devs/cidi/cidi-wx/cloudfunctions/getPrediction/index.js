// cloudfunctions/getPrediction/index.js
const cloud = require('wx-server-sdk')
cloud.init()

exports.main = async (event, context) => {
  const { spotId } = event;

  // === 🤖 模拟建模组的算法逻辑 (Mock) ===

  // 1. 定义拥堵等级 (0: 舒适, 1: 适中, 2: 拥挤, 3: 爆满)
  // 模拟：让 ID 为 '2' (解放碑) 和 '13' (磁器口) 总是显示拥堵
  let level = 0;
  let trend = ''; // 趋势箭头

  const hotSpots = ['2', '13', '1']; // 假设这几个是热门

  if (hotSpots.includes(String(spotId))) {
      // 热门景点：随机生成 2(拥挤) 或 3(爆满)
      level = Math.floor(Math.random() * 2) + 2; 
      trend = '↗ 上升趋势';
  } else {
      // 普通景点：随机生成 0(舒适) 或 1(适中)
      level = Math.floor(Math.random() * 2);
      trend = '↘ 下降趋势';
  }

  // 2. 根据等级生成文案
  const descriptions = [
      '未来2小时人流稀疏，拍照巨出片！📸',   // Level 0
      '人流适中，游览体验舒适。😌',           // Level 1
      '有些拥挤，建议错峰出行。⚠️',           // Level 2
      '非常拥挤！建议避开此时段！⛔️'          // Level 3
  ];

  // 3. 模拟一点网络延迟，让loading转一会儿，更像在实时计算
  await new Promise(resolve => setTimeout(resolve, 800));

  return {
    code: 0,
    data: {
        spotId: spotId,
        predictTime: new Date().getHours() + 2 + ':00', // 预测未来2小时
        level: level,
        desc: descriptions[level],
        count: Math.floor(Math.random() * 2000) + 500, // 模拟一个预测人数
        trend: trend
    }
  }
}