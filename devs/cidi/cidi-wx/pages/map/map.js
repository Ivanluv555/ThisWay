const app = getApp();

Page({
  data: {
    latitude: 29.557458,
    longitude: 106.577262,
    scale: 13,
    markers: []
  },
  
  // 私有变量：用于逻辑控制
  _timer: null,
  _lock: false, // 【新增】防冲突锁

  onLoad: function () { 
    this.fetchMarkers(); 
  },
  
  fetchMarkers: function () {
    // 【修改】改为调用云函数 getMockData
    wx.cloud.callFunction({
      name: 'getMockData',
      data: { type: 'map' } // 告诉云函数我要地图数据
    }).then(res => {
      const result = res.result;
      if (result.code === 0) {
        // 下面这段数据处理逻辑保持不变
        const markers = result.data.map(item => ({
          ...item,
          width: 40, 
          height: 40,
          anchor: { x: 0.5, y: 1 }, 
          callout: {
            content: ` ${item.title} \n 点击查看详情 `, 
            // ... (保留你之前的样式设置) ...
            display: 'NONE'
          }
        }));
        this.setData({ markers });
      }
    }).catch(err => {
       console.error(err);
    });
  },
  
  // 1. 点击标记点
  onMarkerTap: function(e) {
    // 【核心修复】一旦点击了标记，立马上锁
    this._lock = true;
    
    // 200毫秒后解锁。这个时间足够让紧随其后的 onMapTap 执行并被拦截
    setTimeout(() => {
      this._lock = false;
    }, 200);

    const markerId = e.markerId;
    
    // 清除之前的自动关闭定时器
    if (this._timer) clearTimeout(this._timer);

    const newMarkers = this.data.markers.map(item => {
      const shouldShow = item.id === markerId;
      return {
        ...item,
        // 选中变大一点点，作为视觉反馈（可选）
        width: shouldShow ? 45 : 40,
        height: shouldShow ? 45 : 40,
        zIndex: shouldShow ? 999 : 1, // 选中的在最上层
        callout: {
          ...item.callout,
          display: shouldShow ? 'ALWAYS' : 'NONE'
        }
      };
    });

    this.setData({ markers: newMarkers });

    // 重置 10秒 自动关闭
    this._timer = setTimeout(() => {
      this._hideAllCallouts();
    }, 10000);
  },

  // 2. 点击地图空白处
  onMapTap: function() {
    // 【核心修复】如果有锁，说明刚才点的是标记，不是空白处，直接返回，不做任何操作
    if (this._lock) {
      return;
    }
    
    this._hideAllCallouts();
  },

  // 3. 点击气泡跳转
  onCalloutTap: function(e) {
    // 增加防抖锁，防止多次跳转
    if (this._lock) return; 
    
    wx.navigateTo({ url: `/pages_sub/location/location?id=${e.markerId}` });
  },

  // 辅助方法：隐藏所有气泡
  _hideAllCallouts: function() {
    if (this._timer) clearTimeout(this._timer);
    
    const newMarkers = this.data.markers.map(item => ({
      ...item,
      width: 40, // 恢复正常大小
      height: 40,
      zIndex: 1,
      callout: { ...item.callout, display: 'NONE' }
    }));
    
    this.setData({ markers: newMarkers });
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
})