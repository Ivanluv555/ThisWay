// cloudfunctions/login/index.js
const cloud = require('wx-server-sdk')
cloud.init()

exports.main = async (event, context) => {
  // 核心魔法：直接获取当前用户的 OpenID (微信身份证)
  const wxContext = cloud.getWXContext()

  return {
    openid: wxContext.OPENID,
    appid: wxContext.APPID,
    unionid: wxContext.UNIONID,
    // 返回一个模拟的微信用户信息，方便前端展示
    userInfo: {
        username: '微信用户_' + wxContext.OPENID.substring(0, 4), // 模拟昵称
        // 使用 ui-avatars 生成一个随机头像
        avatarUrl: `https://ui-avatars.com/api/?name=Wx&background=07C160&color=fff`
    }
  }
}