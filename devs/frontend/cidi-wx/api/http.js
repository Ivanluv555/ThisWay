function buildQueryString(query) {
  if (!query) return '';
  const parts = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`);
  return parts.length ? `?${parts.join('&')}` : '';
}

let lastOfflinePromptAt = 0;

function notifyOfflineIfNeeded() {
  const app = getApp && getApp();
  const now = Date.now();
  if (now - lastOfflinePromptAt < 3000) {
    return;
  }

  wx.getNetworkType({
    success: (res) => {
      if (res.networkType === 'none') {
        lastOfflinePromptAt = now;
        if (app && typeof app.notifyNoNetwork === 'function') {
          app.notifyNoNetwork('当前无网络连接，请稍后重试');
          return;
        }
        wx.showToast({ title: '当前无网络连接', icon: 'none' });
      }
    }
  });
}

function getBaseUrl() {
  const app = getApp && getApp();
  const base = app && app.globalData && app.globalData.apiBaseUrl
    ? app.globalData.apiBaseUrl
    : 'http://localhost:8080';
  return base.replace(/\/$/, '');
}

function request({ path, method = 'GET', data, query, withAuth = false }) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const headers = {
      'content-type': 'application/json'
    };

    if (withAuth && token) {
      headers.Authorization = `Bearer ${token}`;
    }

    wx.request({
      url: `${getBaseUrl()}${path}${buildQueryString(query)}`,
      method,
      data,
      header: headers,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
          return;
        }
        reject(new Error(`HTTP ${res.statusCode}`));
      },
      fail: (err) => {
        notifyOfflineIfNeeded();
        reject(err);
      }
    });
  });
}

function upload({ path, filePath, name = 'file', formData, withAuth = false }) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const headers = {};

    if (withAuth && token) {
      headers.Authorization = `Bearer ${token}`;
    }

    wx.uploadFile({
      url: `${getBaseUrl()}${path}`,
      filePath,
      name,
      formData,
      header: headers,
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }

        let payload = res.data;
        if (typeof payload === 'string') {
          try {
            payload = JSON.parse(payload);
          } catch (e) {
            reject(new Error('上传响应不是合法 JSON'));
            return;
          }
        }
        resolve(payload);
      },
      fail: (err) => {
        notifyOfflineIfNeeded();
        reject(err);
      }
    });
  });
}

module.exports = {
  request,
  upload
};
