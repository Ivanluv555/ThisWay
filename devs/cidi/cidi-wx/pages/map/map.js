const app = getApp();
Page({
  data: {
    latitude: 29.557458,
    longitude: 106.577262,
    scale: 13,
    markers: []
  },
  onLoad: function () { this.fetchMarkers(); },
  
  fetchMarkers: function () {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/map/markers`,
      success: (res) => {
        if (res.data.code === 0) {
          const markers = res.data.data.map(item => ({
            ...item,
            width: 30, height: 30,
            callout: {
              content: ` ${item.title} `,
              color: '#2C2C2C', fontSize: 12, borderRadius: 4, bgColor: '#fff', padding: 8, display: 'BYCLICK', borderWidth: 1, borderColor: '#ddd', boxShadow: '0 4rpx 10rpx rgba(0,0,0,0.1)'
            }
          }));
          this.setData({ markers });
        }
      }
    });
  },
  
  onCalloutTap: function(e) {
    // 跳转到地点详情工厂页
    wx.navigateTo({ url: `/pages_sub/location/location?id=${e.markerId}` });
  }
})