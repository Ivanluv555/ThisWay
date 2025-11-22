const app = getApp();
import { getFuturePrediction } from '../../api/prediction.js';

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
  
  // 【核心修改】改为调用云函数
  fetchDetail: function (id) {
    wx.showLoading({ title: '加载中...' });
    
    wx.cloud.callFunction({
      name: 'getMockData',
      data: { 
        type: 'location', // 告诉云函数我要查地点详情
        id: id            // 把当前地点的 ID 传过去
      }
    }).then(res => {
      wx.hideLoading();
      const result = res.result;
      if(result.code === 0) {
        this.setData({ location: result.data });
      } else {
        wx.showToast({ title: '未找到地点信息', icon: 'none' });
      }
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