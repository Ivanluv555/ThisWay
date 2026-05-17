const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    latitude: 29.557458,
    longitude: 106.577262,
    scale: 13,
    markers: [],
    selectedMarker: null,
<<<<<<< HEAD
    viewMode: 'cards',   // 'cards' | 'map' | 'fullscreen'
    drawerConfig: null   // WXS prop: { snapPoints, current }
  },

  _timer: null,
  _ignoreRegionChange: false,
  _mapReady: false,

  onLoad: function () {
    this.initDrawer();
    this.locateUser();
    this.fetchMarkers();
    // Ignore initial map render events
    setTimeout(() => { this._mapReady = true; }, 1500);
  },

  initDrawer: function () {
    const { windowHeight } = wx.getSystemInfoSync();
    // 0.25 = 75% cards, 0.75 = 75% map, 0.85 = 15vh min cards
    const snapFractions = [0.25, 0.75, 0.85];
    const snapPoints = snapFractions.map(f => Math.round(windowHeight * f));
    this.setData({
      drawerConfig: { snapPoints, current: 0 }
    });
  },

  // Called by WXS after drag ends with nearest snap index
  onDrawerSnap: function (index) {
    const modes = ['cards', 'map', 'map'];
    this.setData({ viewMode: modes[index] || 'cards' });
  },

  locateUser: function() {
    wx.getSetting({
      success: (settingRes) => {
        const auth = settingRes.authSetting || {};
        if (auth['scope.userLocation'] === false) {
          wx.showToast({ title: '请先授权定位', icon: 'none' });
          return;
        }
        this._ignoreRegionChange = true;
        wx.getLocation({
          type: 'gcj02',
          isHighAccuracy: true,
          success: (res) => {
            this.setData({
              latitude: res.latitude,
              longitude: res.longitude
            });
            setTimeout(() => { this._ignoreRegionChange = false; }, 600);
          },
          fail: () => {
            this._ignoreRegionChange = false;
          }
=======
    // WXS 配置
    drawerConfig: null
  },
  
  // 私有变量：用于逻辑控制
  _timer: null,

  onLoad: function () { 
    this.initDrawer();
    this.locateUser();
    this.fetchMarkers(); 
  },

  locateUser: function() {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (res) => {
        this.setData({
          latitude: res.latitude,
          longitude: res.longitude
>>>>>>> origin/fullcircle
        });
      }
    });
  },

<<<<<<< HEAD
  locateMe: function () {
    this.locateUser();
  },

  fetchMarkers: function () {
    Promise.all([
      cidiApi.getMapLocations(),
      cidiApi.getMapPostMarkers(),
      cidiApi.getLatestPredictions().catch(() => [])
    ]).then(([spots, posts, predictions]) => {
      const crowdMap = {};
      (predictions || []).forEach((p) => {
        crowdMap[p.spotId] = {
          level: Number(p.congestionLevel),
          density: p.crowdDensity != null ? Number(p.crowdDensity) : null
        };
      });

      const all = (spots || []).concat(posts || []);
      const markers = all.map(item => {
        const crowd = item.markerType === 'spot' ? crowdMap[item.id] : null;
        return {
          ...item,
          width: 0,
          height: 0,
          customCallout: {
            display: 'ALWAYS',
            anchorX: 0,
            anchorY: 0,
          },
          crowdLevel: crowd ? crowd.level : -1,
          crowdDensity: crowd ? crowd.density : null
        };
      });
=======
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
    Promise.all([
      cidiApi.getMapLocations(),
      cidiApi.getMapPostMarkers()
    ]).then(([spots, posts]) => {
      const all = (spots || []).concat(posts || []);
      const markers = all.map(item => ({
        ...item,
        width: 0,
        height: 0,
        customCallout: {
          display: 'ALWAYS',
          anchorX: 0,
          anchorY: 0,
        },
        heat: item.markerType === 'post' ? 'POST' : Math.floor(Math.random() * 9000) + 100
      }));
>>>>>>> origin/fullcircle
      this.setData({ markers });
    }).catch(err => {
       console.error(err);
       wx.showToast({ title: '地图加载失败', icon: 'none' });
    });
  },
<<<<<<< HEAD

  // Map region changed (user pan/zoom)
  onMapRegionChange: function (e) {
    if (!this._mapReady) return;
    if (this._ignoreRegionChange) return;
    if (e.type === 'end') {
      this.focusMap();
    }
  },

  onCardTap: function () {
    this.focusCards();
  },

  // Card list scrolled
  onCardScroll: function () {
    this.focusCards();
  },

  toggleFullscreen: function () {
    if (this.data.viewMode === 'fullscreen') {
      const cfg = this.data.drawerConfig;
      this.setData({
        viewMode: 'cards',
        drawerConfig: { ...cfg, current: 0 }
      });
    } else {
      this.setData({ viewMode: 'fullscreen' });
    }
  },

  focusMap: function () {
    if (this.data.viewMode !== 'map') {
      const cfg = this.data.drawerConfig;
      this.setData({
        viewMode: 'map',
        drawerConfig: { ...cfg, current: 1 }
      });
    }
  },

  focusCards: function () {
    if (this.data.viewMode !== 'cards') {
      const cfg = this.data.drawerConfig;
      this.setData({
        viewMode: 'cards',
        drawerConfig: { ...cfg, current: 0 }
      });
    }
  },

  onMarkerTap: function(e) {
    this._lock = true;
    setTimeout(() => { this._lock = false; }, 200);

    const markerId = e.markerId;
=======
  
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
>>>>>>> origin/fullcircle
    if (this._timer) clearTimeout(this._timer);

    const newMarkers = this.data.markers.map(item => {
      const shouldShow = item.id === markerId;
      return {
        ...item,
<<<<<<< HEAD
        width: shouldShow ? 45 : 40,
        height: shouldShow ? 45 : 40,
        zIndex: shouldShow ? 999 : 1,
=======
        // 选中变大一点点，作为视觉反馈（可选）
        width: shouldShow ? 45 : 40,
        height: shouldShow ? 45 : 40,
        zIndex: shouldShow ? 999 : 1, // 选中的在最上层
>>>>>>> origin/fullcircle
        callout: {
          ...item.callout,
          display: shouldShow ? 'ALWAYS' : 'NONE'
        }
      };
    });

    this.setData({ markers: newMarkers });

<<<<<<< HEAD
=======
    // 重置 10秒 自动关闭
>>>>>>> origin/fullcircle
    this._timer = setTimeout(() => {
      this._hideAllCallouts();
    }, 10000);
  },

<<<<<<< HEAD
  onMapTap: function() {
    if (this._lock) return;
    this._hideAllCallouts();
    this.focusMap();
  },

  onCalloutTap: function(e) {
    if (this._lock) return;
=======
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
>>>>>>> origin/fullcircle

    const markerId = e.markerId || (e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.markerId);
    const marker = (this.data.markers || []).find((item) => `${item.id}` === `${markerId}`);
    if (!marker) return;

    if (marker.markerType === 'post' && marker.postId) {
      wx.navigateTo({ url: `/pages_sub/article/article?id=${marker.postId}` });
      return;
    }
    wx.navigateTo({ url: `/pages_sub/location/location?id=${marker.id}` });
  },

<<<<<<< HEAD
  _hideAllCallouts: function() {
    if (this._timer) clearTimeout(this._timer);

    const newMarkers = this.data.markers.map(item => ({
      ...item,
      width: 40,
=======
  // 辅助方法：隐藏所有气泡
  _hideAllCallouts: function() {
    if (this._timer) clearTimeout(this._timer);
    
    const newMarkers = this.data.markers.map(item => ({
      ...item,
      width: 40, // 恢复正常大小
>>>>>>> origin/fullcircle
      height: 40,
      zIndex: 1,
      callout: { ...item.callout, display: 'NONE' }
    }));
<<<<<<< HEAD

=======
    
>>>>>>> origin/fullcircle
    this.setData({ markers: newMarkers });
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
<<<<<<< HEAD
});
=======
})
>>>>>>> origin/fullcircle
