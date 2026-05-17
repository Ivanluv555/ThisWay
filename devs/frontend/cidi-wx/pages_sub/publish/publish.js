const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    title: '',
    content: '',
    images: [],
    categories: [],
    activeCategoryId: 1,
    locationOptions: [],
    selectedLocationIndex: 0,
    currentGeo: null,
    locationLabel: '未定位',
    statusBarHeight: 44,
    navBarHeight: 88,
    navPaddingRight: 0,
    submitting: false
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync();
    const menuRect = wx.getMenuButtonBoundingClientRect();
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight || 44,
      navBarHeight: (menuRect.bottom - sysInfo.statusBarHeight) + menuRect.height + 8,
      navPaddingRight: sysInfo.windowWidth - menuRect.left
    });
    this.initPublishMeta();
  },

  async initPublishMeta() {
    const geoInfo = await cidiApi.getCurrentLocationInfo();
    const options = await cidiApi.getPublishOptions(geoInfo.ok ? geoInfo : null);

    const categories = options.categories || [];
    const activeCategoryId = categories.length ? categories[0].id : 1;
    const locationOptions = [
      { id: null, title: '不关联地点', distance: '' }
    ].concat(options.nearbyLocations || []);

    this.setData({
      categories,
      activeCategoryId,
      locationOptions,
      selectedLocationIndex: 0,
      currentGeo: geoInfo.ok ? {
        latitude: geoInfo.latitude,
        longitude: geoInfo.longitude
      } : null,
      locationLabel: geoInfo.ipLocation || '未定位'
    });
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onContentInput(e) { this.setData({ content: e.detail.value }); },
  goBack() { wx.navigateBack(); },

  selectCategory(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ activeCategoryId: id });
  },

  onLocationChange(e) {
    this.setData({ selectedLocationIndex: Number(e.detail.value || 0) });
  },

  async locateMe() {
    const geoInfo = await cidiApi.getCurrentLocationInfo();
    this.setData({
      currentGeo: geoInfo.ok ? { latitude: geoInfo.latitude, longitude: geoInfo.longitude } : null,
      locationLabel: geoInfo.ipLocation || '未定位'
    });

    const options = await cidiApi.getPublishOptions(geoInfo.ok ? geoInfo : null);
    this.setData({
      locationOptions: [{ id: null, title: '不关联地点', distance: '' }].concat(options.nearbyLocations || []),
      selectedLocationIndex: 0
    });
  },

  // 选择图片
  chooseImage() {
    wx.chooseMedia({
      count: 9 - this.data.images.length,
      mediaType: ['image'],
      success: (res) => {
        const paths = res.tempFiles.map(f => f.tempFilePath);
        this.setData({ images: this.data.images.concat(paths) });
      }
    });
  },

  // 删除图片
  removeImage(e) {
    const idx = e.currentTarget.dataset.index;
    const list = this.data.images;
    list.splice(idx, 1);
    this.setData({ images: list });
  },

  async handlePublish() {
    if (this.data.submitting) return;

    if (!app.globalData.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const title = (this.data.title || '').trim();
    const content = (this.data.content || '').trim();
    if (!title && !content && this.data.images.length === 0) {
      wx.showToast({ title: '请填写标题/内容或上传图片', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '发布中...', mask: true });

    try {
      const selectedLocation = this.data.locationOptions[this.data.selectedLocationIndex] || { id: null, title: '' };
      const selectedCategory = (this.data.categories || []).find((item) => Number(item.id) === Number(this.data.activeCategoryId));

      await cidiApi.createPost({
        title: title || '分享图片',
        content,
        images: this.data.images,
        userId: app.globalData.userInfo.id,
        categoryId: this.data.activeCategoryId,
        categoryName: selectedCategory ? selectedCategory.name : '推荐',
        locationId: selectedLocation.id,
        locationName: selectedLocation.title || '',
        geo: this.data.currentGeo,
        ipLocation: this.data.locationLabel
      });

      wx.hideLoading();
      wx.showToast({ title: '发布成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);

    } catch (err) {
      console.error('发布失败', err);
      wx.hideLoading();
      const msg = (err && err.message) ? err.message : '发布失败，请重试';
      wx.showToast({ title: msg, icon: 'none', duration: 2000 });
      this.setData({ submitting: false });
    }
  }
})