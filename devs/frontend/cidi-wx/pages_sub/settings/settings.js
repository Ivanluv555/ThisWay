const app = getApp();

Page({
	data: {
		apiBaseUrl: '',
		testing: false,
		testResult: '',
		testStatus: ''
	},

	onShow() {
		this.setData({
			apiBaseUrl: app.globalData.apiBaseUrl || 'http://127.0.0.1:8080'
		});
	},

	onApiInput(e) {
		this.setData({ apiBaseUrl: e.detail.value || '' });
	},

	saveApiBaseUrl() {
		const raw = (this.data.apiBaseUrl || '').trim();
		if (!/^https?:\/\//i.test(raw)) {
			wx.showToast({ title: '请填写 http(s) 地址', icon: 'none' });
			return;
		}
		app.setApiBaseUrl(raw);
		this.setData({ testResult: '已保存，请重新进入页面测试', testStatus: 'ok' });
		wx.showToast({ title: '保存成功', icon: 'success' });
	},

	useLocalhost() {
		this.setData({ apiBaseUrl: 'http://127.0.0.1:8080' });
	},

	async testConnection() {
		const url = (this.data.apiBaseUrl || '').trim().replace(/\/$/, '');
		if (!url) {
			wx.showToast({ title: '请先填写后端地址', icon: 'none' });
			return;
		}

		this.setData({ testing: true, testResult: '', testStatus: '' });

		wx.request({
			url: `${url}/v3/api-docs`,
			method: 'GET',
			timeout: 8000,
			success: (res) => {
				if (res.statusCode >= 200 && res.statusCode < 300) {
					this.setData({
						testing: false,
						testStatus: 'ok',
						testResult: `连接成功 (${res.statusCode})，后端可访问`
					});
					app.setApiBaseUrl(url);
					return;
				}
				this.setData({
					testing: false,
					testStatus: 'fail',
					testResult: `连接失败 (HTTP ${res.statusCode})`
				});
			},
			fail: (err) => {
				const msg = (err && err.errMsg) ? err.errMsg : '网络错误';
				this.setData({
					testing: false,
					testStatus: 'fail',
					testResult: `连接失败：${msg}`
				});
			}
		});
	},

	logout() {
		app.clearAuth();
		wx.showToast({ title: '已退出登录', icon: 'none' });
		setTimeout(() => {
			wx.navigateBack();
		}, 600);
	}
});