Page({
  data: {
    appNameCn: '此地',
    appNameEn: 'CIDI',
    sliderTip: '向右滑动进入',
    entering: false,
    isDragging: false,
    knobX: 0,
    startTouchX: 0,
    startKnobX: 0,
    maxX: 0,
    sliderLeft: 0,
    progress: 0,
    openThreshold: 0.68,
    knobSize: 44,
    trackPadding: 5,
    fillWidth: 54
  },

  onReady() {
    this.measureSlider();
  },

  onShow() {
    this.measureSlider();
  },

  measureSlider() {
    const query = this.createSelectorQuery();
    query
      .select('.glass-slider')
      .boundingClientRect((rect) => {
        if (!rect) return;

        const maxX = Math.max(rect.width - this.data.knobSize - this.data.trackPadding * 2, 0);
        this.setData({
          maxX,
          sliderLeft: rect.left,
          fillWidth: this.data.knobSize + this.data.trackPadding * 2
        });
      })
      .exec();
  },

  clampValue(value, min, max) {
    return Math.min(Math.max(value, min), max);
  },

  updateSliderPosition(nextX) {
    const clampedX = this.clampValue(nextX, 0, this.data.maxX);
    const progress = this.data.maxX > 0 ? clampedX / this.data.maxX : 0;
    const fillWidth = clampedX + this.data.knobSize + this.data.trackPadding * 2;

    this.setData({
      knobX: clampedX,
      progress,
      fillWidth
    });
  },

  onDragStart(e) {
    if (this.data.entering) return;

    if (!this.data.maxX) {
      this.measureSlider();
    }

    const touchX = e.touches[0].clientX;
    const jumpX = touchX - this.data.sliderLeft - this.data.trackPadding - this.data.knobSize / 2;
    const clampedJumpX = this.clampValue(jumpX, 0, this.data.maxX);

    clearTimeout(this._tipTimer);
    this.setData({
      isDragging: true,
      startTouchX: touchX,
      startKnobX: clampedJumpX,
      sliderTip: '向右滑动进入'
    });

    this.updateSliderPosition(clampedJumpX);
  },

  onDragMove(e) {
    if (this.data.entering || !this.data.isDragging) return;

    const touchX = e.touches[0].clientX;
    const deltaX = touchX - this.data.startTouchX;
    const nextX = this.data.startKnobX + deltaX;
    this.updateSliderPosition(nextX);
  },

  onDragEnd() {
    if (this.data.entering || !this.data.isDragging) return;

    this.setData({ isDragging: false });

    if (this.data.progress >= this.data.openThreshold || this.data.knobX >= this.data.maxX - 2) {
      this.enterApp();
      return;
    }

    this.resetSlider('再滑一点即可进入');
  },

  resetSlider(tipText = '向右滑动进入') {
    this.setData({
      knobX: 0,
      progress: 0,
      fillWidth: this.data.knobSize + this.data.trackPadding * 2,
      sliderTip: tipText
    });

    clearTimeout(this._tipTimer);
    this._tipTimer = setTimeout(() => {
      this.setData({ sliderTip: '向右滑动进入' });
    }, 900);
  },

  enterApp() {
    this.setData({
      entering: true,
      knobX: this.data.maxX,
      progress: 1,
      fillWidth: this.data.maxX + this.data.knobSize + this.data.trackPadding * 2,
      sliderTip: '正在进入...'
    });

    wx.switchTab({
      url: '/pages/index/index',
      fail: () => {
        this.setData({ entering: false });
        this.resetSlider('进入失败，请重试');
      }
    });
  },

  onUnload() {
    clearTimeout(this._tipTimer);
  }
});