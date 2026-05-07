const ci = require('miniprogram-ci');

(async () => {
    // 1. 定义项目配置
    const project = new ci.Project({
        appid: 'wxca8c34ccdeebcabc',
        type: 'miniProgram',
        projectPath: './', // 指向包含 project.config.json 的目录
        privateKeyPath: './private.wxca8c34ccdeebcabc.key', // 刚才下载的密钥文件路径
        ignores: ['node_modules/**/*', 'preview.js'], // 忽略不需要打包的文件
    });

    console.log('Compiling and making QR cods');

    // 2. 执行预览动作
    try {
        const previewResult = await ci.preview({
            project,
            desc: 'WebStorm CI 自动预览',
            setting: {
                es6: true,
                minify: true,
                minifyWXML: true,
                minifyWXSS: true,
                minifyJS: true
            },
            // 关键配置：直接在 WebStorm 终端打印二维码
            qrcodeFormat: 'image', // 将 'terminal' 改为 'image'
            qrcodeOutputDest: './P.jpg', // 指定生成的二维码图片名称和路径
            pagePath: 'pages/index/index', // 扫码后直接打开的页面
            searchQuery: '', // 启动参数
        });
        console.log('Successfully generated QR code for preview!');
    } catch (error) {
        console.error('Mistake:', error);
    }
})();