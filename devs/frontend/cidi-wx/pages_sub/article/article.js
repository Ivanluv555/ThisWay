const app = getApp();

Page({
  data: { 
    article: null, 
    loading: true,
    isLiked: false,
    isCollected: false,
    comments: [], 
    showInput: false,
    commentVal: '' // 存储输入的评论
  },

  onLoad: function (options) { 
    this.articleId = options.id;
    this.fetchDetail();
    this.fetchComments();
  },

  // 1. 获取详情 (包含点赞/收藏状态)
  fetchDetail() {
    wx.cloud.callFunction({
      name: 'cidi-api',
      data: { 
          action: 'getArticleDetail', 
          payload: { id: this.articleId } 
      }
    }).then(res => {
      const { post, isLiked, isCollected } = res.result;
      
      // 容错处理：确保 images 字段存在
      let images = post.images || [];
      if (images.length === 0 && post.coverUrl) {
          images = [post.coverUrl];
      }

      this.setData({
        article: { ...post, images },
        isLiked,
        isCollected,
        loading: false
      });
    }).catch(err => {
      console.error(err);
      wx.showToast({ title: '内容不存在', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1000);
    });
  },

  // 2. 获取评论列表
  fetchComments() {
    wx.cloud.callFunction({
      name: 'cidi-api',
      data: { action: 'getComments', payload: { postId: this.articleId } }
    }).then(res => {
       const comments = res.result.data.map(c => ({
           userInfo: c.userInfo || { username: '匿名', avatarUrl: '' },
           user: c.userInfo ? c.userInfo.username : '匿名', 
           text: c.content
       }));
       this.setData({ comments });
    });
  },

  // 3. 点赞 (乐观 UI + 云同步)
  toggleLike() {
    // 检查登录
    if (!app.globalData.userInfo) return this.requireLogin();

    // 乐观更新：先改界面，让用户觉得极快
    const oldStatus = this.data.isLiked;
    const oldNum = this.data.article.likeCount || 0;
    
    this.setData({ 
        isLiked: !oldStatus,
        'article.likeCount': oldStatus ? oldNum - 1 : oldNum + 1
    });

    // 后台默默发请求
    wx.cloud.callFunction({
        name: 'cidi-api',
        data: { 
            action: 'toggleAction', 
            payload: { postId: this.articleId, type: 'like' } 
        }
    }).catch(err => {
        // 如果失败了，悄悄改回来
        console.error(err);
        this.setData({ isLiked: oldStatus, 'article.likeCount': oldNum });
    });
  },
  
  // 4. 收藏 (乐观 UI + 云同步)
  toggleCollect() {
    if (!app.globalData.userInfo) return this.requireLogin();

    const oldStatus = this.data.isCollected;
    this.setData({ isCollected: !oldStatus });
    
    wx.cloud.callFunction({
      name: 'cidi-api',
      data: { 
          action: 'toggleAction', 
          payload: { postId: this.articleId, type: 'collect' } 
      }
    }).then(res => {
      const msg = res.result.status ? '收藏成功' : '取消收藏';
      wx.showToast({ title: msg, icon: 'none' });
    }).catch(() => {
      this.setData({ isCollected: oldStatus });
    });
  },

  // 5. 评论逻辑
  showCommentInput() { 
      if (!app.globalData.userInfo) return this.requireLogin();
      this.setData({ showInput: true }); 
  },
  hideCommentInput() { this.setData({ showInput: false }); },
  
  onCommentInput(e) {
      this.setData({ commentVal: e.detail.value });
  },

  triggerSend() {
    const text = this.data.commentVal;
    if (!text || !text.trim()) {
        wx.showToast({ title: '写点内容吧', icon: 'none' });
        return;
    }
    
    wx.showLoading({ title: '发送中' });
    
    wx.cloud.callFunction({
        name: 'cidi-api',
        data: {
            action: 'addComment',
            payload: {
                postId: this.articleId,
                content: text,
                userInfo: app.globalData.userInfo
            }
        }
    }).then(() => {
        wx.hideLoading();
        this.setData({ showInput: false, commentVal: '' });
        this.fetchComments(); // 刷新评论列表
        wx.showToast({ title: '评论成功', icon: 'success' });
    }).catch(err => {
        wx.hideLoading();
        wx.showToast({ title: '发送失败', icon: 'none' });
    });
  },

  previewImage(e) {
    wx.previewImage({
      current: e.currentTarget.dataset.src,
      urls: this.data.article.images
    });
  },

  requireLogin() {
      wx.showToast({ title: '请先登录', icon: 'none' });
      // 可选：跳转登录页
      // setTimeout(() => wx.navigateTo({ url: '/pages_sub/login/login' }), 1000);
  }
})