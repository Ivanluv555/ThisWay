Page({
  data: {
    appNameCn: '此地',
    appNameEn: 'CIDI',
    sliderTip: '即将进入...',
    entering: false,
    knobX: 0,
    maxX: 0,
    fillWidth: 54,
    knobSize: 44,
    trackPadding: 5
  },

  onReady() {
    this.tryAutoEnter();
  },

  tryAutoEnter(retries = 5) {
    const query = this.createSelectorQuery();
    query
      .select('.glass-slider')
      .boundingClientRect((rect) => {
        if (rect && rect.width > 0) {
          const maxX = Math.max(rect.width - this.data.knobSize - this.data.trackPadding * 2, 0);
          this.setData({ maxX });
          this.startAutoEnter(maxX);
        } else if (retries > 0) {
          setTimeout(() => this.tryAutoEnter(retries - 1), 200);
        } else {
          // 测量失败，直接进入
          this.enterApp();
        }
      })
      .exec();
  },

  startAutoEnter(maxX) {
    if (this._entering || this.data.entering) return;
    this._entering = true;

    const duration = 1200;
    const stepInterval = 16;
    const totalSteps = Math.ceil(duration / stepInterval);
    let step = 0;

    this._animTimer = setInterval(() => {
      step++;
      const progress = Math.min(step / totalSteps, 1);
      // easeOutCubic
      const eased = 1 - Math.pow(1 - progress, 3);
      const knobX = eased * maxX;

      this.setData({
        knobX,
        fillWidth: knobX + this.data.knobSize + this.data.trackPadding * 2
      });

      if (step >= totalSteps) {
        clearInterval(this._animTimer);
        this._animTimer = null;
        this.enterApp();
      }
    }, stepInterval);
  },

  enterApp() {
    this.setData({
      entering: true,
      knobX: this.data.maxX,
      fillWidth: this.data.maxX + this.data.knobSize + this.data.trackPadding * 2,
      sliderTip: '欢迎探索'
    });

    setTimeout(() => {
      wx.switchTab({
        url: '/pages/index/index'
      });
    }, 300);
  },

  onUnload() {
    if (this._animTimer) {
      clearInterval(this._animTimer);
      this._animTimer = null;
    }
  }
});
