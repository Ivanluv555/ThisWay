const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    latitude: 29.557458,
    longitude: 106.577262,
    scale: 13,
    markers: [],
    selectedMarker: null,
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
        });
      }
    });
  },

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
      this.setData({ markers });
    }).catch(err => {
       console.error(err);
       wx.showToast({ title: '地图加载失败', icon: 'none' });
    });
  },

  // Map region changed (user pan/zoom)
  onMapRegionChange: function (e) {
    if (!this._mapReady) return;
    if (this._ignoreRegionChange) return;
    if (e.type === 'end') {
      this.focusMap();
    }
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
    if (this._timer) clearTimeout(this._timer);

    const newMarkers = this.data.markers.map(item => {
      const shouldShow = item.id === markerId;
      return {
        ...item,
        width: shouldShow ? 45 : 40,
        height: shouldShow ? 45 : 40,
        zIndex: shouldShow ? 999 : 1,
        callout: {
          ...item.callout,
          display: shouldShow ? 'ALWAYS' : 'NONE'
        }
      };
    });

    this.setData({ markers: newMarkers });

    this._timer = setTimeout(() => {
      this._hideAllCallouts();
    }, 10000);
  },

  onMapTap: function() {
    if (this._lock) return;
    this._hideAllCallouts();
    this.focusMap();
  },

  onCalloutTap: function(e) {
    if (this._lock) return;

    const markerId = e.markerId || (e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.markerId);
    const marker = (this.data.markers || []).find((item) => `${item.id}` === `${markerId}`);
    if (!marker) return;

    if (marker.markerType === 'post' && marker.postId) {
      wx.navigateTo({ url: `/pages_sub/article/article?id=${marker.postId}` });
      return;
    }
    wx.navigateTo({ url: `/pages_sub/location/location?id=${marker.id}` });
  },

  _hideAllCallouts: function() {
    if (this._timer) clearTimeout(this._timer);

    const newMarkers = this.data.markers.map(item => ({
      ...item,
      width: 40,
      height: 40,
      zIndex: 1,
      callout: { ...item.callout, display: 'NONE' }
    }));

    this.setData({ markers: newMarkers });
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
});
