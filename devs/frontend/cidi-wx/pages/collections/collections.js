const cidiApi = require('../../api/cidi');

function toRad(value) {
  return (Number(value) * Math.PI) / 180;
}

function calcDistanceKm(lat1, lng1, lat2, lng2) {
  const a1 = Number(lat1);
  const b1 = Number(lng1);
  const a2 = Number(lat2);
  const b2 = Number(lng2);
  if ([a1, b1, a2, b2].some((v) => Number.isNaN(v))) return null;
  const r = 6371;
  const dLat = toRad(a2 - a1);
  const dLng = toRad(b2 - b1);
  const m =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(a1)) * Math.cos(toRad(a2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(m), Math.sqrt(1 - m));
  return r * c;
}

function formatDistance(km) {
  if (km == null) return '未知';
  if (km < 1) return `${Math.round(km * 1000)}m`;
  return `${km.toFixed(1)}km`;
}

function levelToText(level) {
  const n = Number(level);
  if (n === 0) return '舒适';
  if (n === 1) return '适中';
  if (n === 2) return '拥挤';
  return '爆满';
}

function trendToLabel(trend) {
  if (trend === '上升') return '人流上升中';
  if (trend === '下降') return '人流下降中';
  return '人流稳定';
}

Page({
  data: {
    loading: true,
    currentSpot: null,
    currentLocationLabel: '定位中...',
    locationDenied: false,
    bestSpot: null,
    topList: [],
    updatedAt: '',
    errorMsg: ''
  },

  onShow: function () {
    this.fetchData();
  },

  async fetchData() {
    this.setData({
      loading: true,
      errorMsg: '',
      topList: []
    });

    wx.showLoading({ title: '正在规划...' });

    try {
      const [spots, geo] = await Promise.all([
        cidiApi.getMapLocations(),
        cidiApi.getCurrentLocationInfo()
      ]);

      const validSpots = (spots || []).filter(
        (item) => !Number.isNaN(Number(item.latitude)) && !Number.isNaN(Number(item.longitude))
      );

      if (!validSpots.length) {
        this.setData({
          loading: false,
          errorMsg: '暂无景区数据'
        });
        return;
      }

      const hasGeo = geo && geo.ok && !Number.isNaN(Number(geo.latitude)) && !Number.isNaN(Number(geo.longitude));
      const locationDenied = !!(geo && geo.denied);

      const withDistance = validSpots.map((spot) => {
        const distanceKm = hasGeo
          ? calcDistanceKm(geo.latitude, geo.longitude, spot.latitude, spot.longitude)
          : null;
        return {
          ...spot,
          distanceKm,
          distanceText: formatDistance(distanceKm)
        };
      });

      const sortedByDistance = withDistance.slice().sort((a, b) => {
        if (a.distanceKm == null && b.distanceKm == null) return 0;
        if (a.distanceKm == null) return 1;
        if (b.distanceKm == null) return -1;
        return a.distanceKm - b.distanceKm;
      });

      const currentSpot = hasGeo ? sortedByDistance[0] : null;
      const currentLocationLabel = hasGeo
        ? `你在 ${currentSpot ? currentSpot.title : '未知位置'} 附近`
        : locationDenied
          ? '定位未授权，点击此处授权'
          : '未授权定位，已按景区预测推荐';

      const latestPredictions = await cidiApi.getLatestPredictions();
      const predictionMap = {};
      (latestPredictions || []).forEach((p) => {
        predictionMap[p.spotId] = p;
      });

      const forecastResults = withDistance.map((spot) => {
        const raw = predictionMap[spot.id] || null;

        const congestionLevel = raw ? Number(raw.congestionLevel) : 3;
        const crowdDensity = raw ? Number(raw.crowdDensity) : null;
        const visitorCount = raw ? Number(raw.visitorCount) : null;
        const trend = raw ? raw.trend : '未知';
        const predictTime = raw ? raw.predictTime : '--';

        const score = congestionLevel * 100 + (spot.distanceKm == null ? 30 : Math.min(spot.distanceKm, 30));

        return {
          id: spot.id,
          title: spot.title,
          address: spot.address || '',
          category: spot.category || '',
          image: spot.image,
          distanceText: spot.distanceText,
          distanceKm: spot.distanceKm,
          congestionLevel,
          crowdDensity,
          visitorCount,
          levelText: levelToText(congestionLevel),
          predictTime,
          trend,
          trendLabel: trendToLabel(trend),
          score
        };
      });

      const topList = forecastResults
        .sort((a, b) => a.score - b.score)
        .slice(0, 5)
        .map((item, index) => ({
          ...item,
          rank: index + 1
        }));

      this.setData({
        loading: false,
        currentSpot,
        currentLocationLabel,
        locationDenied,
        bestSpot: topList[0] || null,
        topList,
        updatedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false })
      });
    } catch (err) {
      console.error('生成推荐失败', err);
      this.setData({
        loading: false,
        errorMsg: '推荐生成失败，请稍后重试'
      });
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading();
      wx.stopPullDownRefresh();
    }
  },

  onPullDownRefresh() {
    this.fetchData();
  },

  handleLocationAuth() {
    cidiApi.openLocationSetting().then(() => {
      this.fetchData();
    });
  },

  goLocation(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages_sub/location/location?id=${id}`,
      fail: () => {
        wx.showToast({ title: '暂无法打开景区页', icon: 'none' });
      }
    });
  }
});
