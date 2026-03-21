function buildQueryString(query) {
  if (!query) return '';
  const parts = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`);
  return parts.length ? `?${parts.join('&')}` : '';
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
      fail: reject
    });
  });
}

module.exports = {
  request
};
