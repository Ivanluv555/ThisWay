const cidiApi = require('../../api/cidi');

Page({
  data: {
    location: null,
    prediction: null,
    spotId: null
  },

  onLoad: function (options) {
    if (options.id) {
      this.setData({ spotId: options.id });
      this.fetchDetail(options.id);
    }
  },

  fetchDetail: function (id) {
    if (typeof id !== 'string' && typeof id !== 'number') {
      id = this.data.spotId;
    }
    wx.showLoading({ title: '加载中...' });

    cidiApi.getLocationDetail(id).then(result => {
      wx.hideLoading();
      this.setData({
        location: result.location,
        prediction: result.prediction
      });
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '加载失败', icon: 'none' });
      console.error(err);
    });
  },

  openNav: function () {
    const loc = this.data.location;
    if (loc) {
      wx.openLocation({
        latitude: loc.latitude,
        longitude: loc.longitude,
        name: loc.title,
        address: loc.address
      });
    }
  }
});
