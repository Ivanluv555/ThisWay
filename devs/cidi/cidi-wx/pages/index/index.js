const app = getApp();

Page({
  data: { 
    // 增加顶部问候语或Slogan
    greeting: 'Find your\nNext Journey.',
    
    // 增加分类标签数据
    categories: ['推荐', '当季', '露营', '咖啡', '看展', '徒步', '海岛'],
    activeCategory: 0, // 当前选中的分类索引

    contentList: [] 
  },

  onLoad: function () { this.fetchData(); },
  
  onPullDownRefresh: function() { 
      this.fetchData(true); 
  },
  
  // 切换分类（目前仅做UI交互，不真实筛选数据）
  switchCategory: function(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.index });
    // 这里未来可以调用 fetchData 传参筛选
    wx.vibrateShort(); //以此增加一点点触感反馈
  },

  fetchData: function (isRefresh = false) {
    wx.cloud.callFunction({
      name: 'cidi-api',
      data: { 
          action: 'getHomeList',
          payload: { offset: 0, limit: 20 } 
      }
    }).then(res => {
      // 数据库返回的数据在 res.result.data 里
      const posts = res.result.data;
      
      // 数据映射 (Mapping): 把数据库字段转成 UI 需要的字段
      const uiList = posts.map(item => ({
          id: item._id, // 云数据库的 ID 是 _id
          title: item.title,
          coverUrl: item.images[0], // 取第一张图做封面
          author: item.author,
          authorAvatar: item.authorAvatar,
          likes: item.likeCount,
          date: '刚刚' // 暂时简写，后面可以用日期库格式化 item.createTime
      }));

      this.setData({ contentList: uiList });
    }).finally(() => {
        if (isRefresh) wx.stopPullDownRefresh();
    });
},
  
  goDetail: function(e) {
    const id = e.currentTarget.dataset.id;
    if (id) {
        wx.navigateTo({ url: `/pages_sub/article/article?id=${id}` });
    }
  },

  goSearch: function() {
    wx.navigateTo({ url: '/pages/search/search' });
  }
})