const app = getApp();
Page({
  data: { location: null },
  onLoad: function (options) { this.fetchDetail(options.id); },
  
  fetchDetail: function (id) {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/location/${id}`,
      success: (res) => {
        if(res.data.code === 0) this.setData({ location: res.data.data });
      }
    });
  },
  
  openNav: function () {
    const loc = this.data.location;
    wx.openLocation({
      latitude: loc.latitude,
      longitude: loc.longitude,
      name: loc.title,
      address: loc.address
    });
  }
})