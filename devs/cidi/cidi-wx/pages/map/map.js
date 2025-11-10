const app = getApp();

Page({
  data: {
    // 默认聚焦到北碚缙云山
    latitude: 29.841,
    longitude: 106.392,
    scale: 15,
    
    // 1. 预先标定您的 "圣地"
    markers: [
      {
        id: 1,
        latitude: 22.5233, // 腾讯大厦的示例纬度
        longitude: 113.9386, // 腾讯大厦的示例经度
        title: '腾讯大厦', // 标记点标题
        // iconPath: '/images/marker_icon.png', // 您可以自己放一个标记点图标
        width: 30,
        height: 30,
        callout: { // 气泡
          content: '点击查看“腾讯大厦”详情',
          color: '#ffffff',
          fontSize: 13,
          borderRadius: 10,
          bgColor: '#1296db', // 蓝色背景
          padding: 8,
          display: 'BYCLICK' // 点击 marker 时才显示
        }
      },
      {
        id: 2,
        latitude: 22.5199, // 海岸城购物中心示例纬度
        longitude: 113.9358, // 海岸城购物中心示例经度
        title: '海岸城购物中心',
        // iconPath: '/images/marker_icon.png',
        width: 30,
        height: 30,
        callout: {
          content: '点击查看“海岸城”详情',
          color: '#ffffff',
          fontSize: 13,
          borderRadius: 10,
          bgColor: '#1296db',
          padding: 8,
          display: 'BYCLICK'
        }
      },
      {
        id: 3,
        latitude: 29.841, // 北碚缙云山纬度
        longitude: 106.392, // 北碚缙云山经度
        title: '北碚缙云山',
        // iconPath: '/images/marker_icon.png',
        width: 30,
        height: 30,
        callout: {
          content: '点击查看“缙云山”详情',
          color: '#ffffff',
          fontSize: 13,
          borderRadius: 10,
          bgColor: '#1296db',
          padding: 8,
          display: 'BYCLICK'
        }
      }
    ]
  },

  onLoad: function () {
    // 页面加载时，尝试获取用户位置并将地图中心移过去
    this.centerOnUserLocation();
  },
  
  /**
   * 尝试获取用户当前位置，并移动地图中心
   */
  centerOnUserLocation: function () {
    wx.getLocation({
      type: 'gcj02', // 必须是 gcj02
      success: (res) => {
        this.setData({
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: (err) => {
        // 如果失败（例如用户拒绝授权），地图中心将保持在默认的缙云山
        console.warn('wx.getLocation 失败:', err);
      }
    });
  },
  
  // 2. 定义交互事件
  
  /**
   * 用户点击了标记点
   */
  onMarkerTap: function(e) {
    console.log('用户点击了标记点，markerId为:', e.markerId);
    // 微信原生组件会自动处理气泡(callout)的显示
  },

  /**
   * 用户点击了气泡
   */
  onCalloutTap: function(e) {
    const markerId = e.markerId;
    console.log('用户点击了气泡，markerId为:', markerId);
    
    // 在这里实现您的核心交互逻辑
    // 比如：跳转到一个新的“圣地详情页”
    wx.navigateTo({
      url: `/pages_sub/locationDetail/locationDetail?id=${markerId}`
      // (您需要自行创建 pages_sub/locationDetail 页面)
    });
  }
})