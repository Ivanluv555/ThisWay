const app = getApp();
const cidiApi = require('../../api/cidi');

Page({
  data: {
    title: '',
    content: '',
    images: [] // 存临时路径
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onContentInput(e) { this.setData({ content: e.detail.value }); },
  goBack() { wx.navigateBack(); },

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

  // === 核心发布逻辑 ===
  async handlePublish() {
    if (!app.globalData.userInfo) {
       wx.showToast({ title: '请先登录', icon: 'none' });
       return;
    }

    wx.showLoading({ title: '上传中...', mask: true });

    try {
      await cidiApi.createPost({
        title: this.data.title || '分享图片',
        content: this.data.content,
        images: this.data.images,
        userId: app.globalData.userInfo.id
      });

      if (this.data.images.length > 0) {
        wx.showToast({ title: '已发布，图片需公网URL才会入库', icon: 'none' });
      }

      wx.hideLoading();
      wx.showToast({ title: '发布成功', icon: 'success' });
      
      // 3. 延迟返回，让用户看清提示
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);

    } catch (err) {
      console.error(err);
      wx.hideLoading();
      wx.showToast({ title: '发布失败，请重试', icon: 'none' });
    }
  }
})