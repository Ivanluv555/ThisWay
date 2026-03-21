const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    latitude: 29.557458,
    longitude: 106.577262,
    scale: 13,
    markers: [],
    // WXS 配置
    drawerConfig: null
  },
  
  // 私有变量：用于逻辑控制
  _timer: null,

  onLoad: function () { 
    this.initDrawer();
    this.fetchMarkers(); 
  },

  initDrawer: function() {
    const { windowHeight } = wx.getSystemInfoSync();
    // 定义抽屉总高度 85vh
    const drawerHeight = windowHeight * 0.85;
    // 定义 Snap Points (吸附点)，相对于 translateY 偏移量 (px)
    // 0: 完全展开
    // validHeight * 0.4: 中间态
    // validHeight - 120: 收起态 (只露头)
    
    // 注意：translateY = 0 表示 Drawer 在 bottom:0 的位置，即完全展示
    // translateY = positive value 表示向下位移，Drawer 隐藏
    
    const snapPoints = [
      0,                            // 展开 (Top)
      drawerHeight * 0.5,           // 半开 (Mid) - 距离顶部一半
      drawerHeight - 130            // 收起 (Bottom) - 比如露出 130px 
    ];

    this.setData({
      drawerConfig: { snapPoints }
    });
  },
  
  fetchMarkers: function () {
    cidiApi.getMapLocations().then(list => {
      const markers = list.map(item => ({
        ...item,
        width: 0,
        height: 0,
        customCallout: {
          display: 'ALWAYS',
          anchorX: 0,
          anchorY: 0,
        },
        heat: Math.floor(Math.random() * 9000) + 100
      }));
      this.setData({ markers });
    }).catch(err => {
       console.error(err);
       wx.showToast({ title: '地图加载失败', icon: 'none' });
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