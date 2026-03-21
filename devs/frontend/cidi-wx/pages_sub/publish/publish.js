const app = getApp();

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
      const cloudImageIds = [];
      
      // 1. 并发上传图片到云存储
      const uploadTasks = this.data.images.map((filePath, index) => {
        // 生成唯一文件名: posts/时间戳-索引.ext
        const ext = filePath.match(/\.[^.]+?$/)[0]; 
        const cloudPath = `posts/${Date.now()}-${index}${ext}`;
        
        return wx.cloud.uploadFile({
          cloudPath: cloudPath,
          filePath: filePath
        }).then(res => res.fileID); // 返回云端ID
      });

      // 等待所有图片传完
      const uploadedIds = await Promise.all(uploadTasks);
      
      // 2. 写入数据库 (调用 cidi-api 云函数)
      await wx.cloud.callFunction({
        name: 'cidi-api',
        data: {
          action: 'createPost',
          payload: {
            title: this.data.title || '分享图片', // 没标题就给个默认
            content: this.data.content,
            images: uploadedIds,
            userInfo: app.globalData.userInfo
          }
        }
      });

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