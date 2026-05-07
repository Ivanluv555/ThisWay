const { getFuturePrediction, getNextDestinationForecast } = require('../../api/prediction');
const cidiApi = require('../../api/cidi');

Page({
  data: { 
    location: null,
    prediction: null,
    spotId: null,
    nextForecast: [],
    loadingNextForecast: false
  },

  onLoad: function (options) { 
    if (options.id) {
      this.setData({ spotId: options.id });
      this.fetchDetail(options.id);
      this.fetchNextDestinationForecast(options.id);
    }
  },
  
  fetchDetail: function (id) {
    wx.showLoading({ title: '加载中...' });

    cidiApi.getLocationDetail(id).then(location => {
      wx.hideLoading();
      this.setData({ location });
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '加载失败', icon: 'none' });
      console.error(err);
    });
  },
  
  // 预测逻辑保持不变
  fetchPrediction: function() {
    wx.showLoading({ title: 'AI 计算中...' });
    getFuturePrediction(this.data.spotId)
      .then(res => {
        wx.hideLoading();
        if (res.code === 0) {
          this.setData({ prediction: res.data });
        } else {
          wx.showToast({ title: '预测繁忙', icon: 'none' });
        }
      })
      .catch(err => {
        wx.hideLoading();
        console.error(err);
      });
  },

  fetchNextDestinationForecast: function(spotId) {
    this.setData({ loadingNextForecast: true });
    getNextDestinationForecast(spotId)
      .then((list) => {
        this.setData({ nextForecast: list || [] });
      })
      .catch((err) => {
        console.error(err);
        this.setData({ nextForecast: [] });
      })
      .finally(() => {
        this.setData({ loadingNextForecast: false });
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
})