<<<<<<< HEAD
App({
  onLaunch() {
    const userInfo = wx.getStorageSync('userInfo');
    const apiBaseUrl = wx.getStorageSync('apiBaseUrl');
    if (userInfo) {
      this.globalData.userInfo = userInfo;
    }
    if (apiBaseUrl) {
      this.globalData.apiBaseUrl = apiBaseUrl;
    }

    this.initNetworkObserver();
  },
  globalData: {
    userInfo: null,
    apiBaseUrl: 'http://47.111.180.134:8080',
    uploadApiPath: '/api/files/upload',
    networkOnline: true
  },
  _lastNetworkToastAt: 0,

  initNetworkObserver() {
    wx.getNetworkType({
      success: (res) => {
        const online = res.networkType && res.networkType !== 'none';
        this.globalData.networkOnline = !!online;
        if (!online) {
          this.notifyNoNetwork('当前无网络连接，请检查网络设置');
        }
      }
    });

    wx.onNetworkStatusChange((res) => {
      this.globalData.networkOnline = !!res.isConnected;
      if (!res.isConnected) {
        this.notifyNoNetwork('网络已断开，请检查连接');
      }
    });
  },

  notifyNoNetwork(message = '当前无网络连接') {
    const now = Date.now();
    if (now - this._lastNetworkToastAt < 3000) {
      return;
    }
    this._lastNetworkToastAt = now;
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 2200
    });
  },

  setApiBaseUrl(url) {
    const normalized = `${url || ''}`.trim().replace(/\/$/, '');
    if (!normalized) return;
    this.globalData.apiBaseUrl = normalized;
    wx.setStorageSync('apiBaseUrl', normalized);
  },
  clearAuth() {
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  }
=======
App({
  onLaunch() {
    const userInfo = wx.getStorageSync('userInfo');
    const apiBaseUrl = wx.getStorageSync('apiBaseUrl');
    if (userInfo) {
      this.globalData.userInfo = userInfo;
    }
    if (apiBaseUrl) {
      this.globalData.apiBaseUrl = apiBaseUrl;
    }

    this.initNetworkObserver();
  },
  globalData: {
    userInfo: null,
    apiBaseUrl: 'http://127.0.0.1:8080',
    uploadApiPath: '/api/files/upload',
    networkOnline: true
  },
  _lastNetworkToastAt: 0,

  initNetworkObserver() {
    wx.getNetworkType({
      success: (res) => {
        const online = res.networkType && res.networkType !== 'none';
        this.globalData.networkOnline = !!online;
        if (!online) {
          this.notifyNoNetwork('当前无网络连接，请检查网络设置');
        }
      }
    });

    wx.onNetworkStatusChange((res) => {
      this.globalData.networkOnline = !!res.isConnected;
      if (!res.isConnected) {
        this.notifyNoNetwork('网络已断开，请检查连接');
      }
    });
  },

  notifyNoNetwork(message = '当前无网络连接') {
    const now = Date.now();
    if (now - this._lastNetworkToastAt < 3000) {
      return;
    }
    this._lastNetworkToastAt = now;
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 2200
    });
  },

  setApiBaseUrl(url) {
    const normalized = `${url || ''}`.trim().replace(/\/$/, '');
    if (!normalized) return;
    this.globalData.apiBaseUrl = normalized;
    wx.setStorageSync('apiBaseUrl', normalized);
  },
  clearAuth() {
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  }
>>>>>>> origin/fullcircle
})