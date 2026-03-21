## 业务后端

自己描述清楚后端的运行机制，交互逻辑。一般是不用写的，因为一般情况下之后负责后端开发的人以及产品经理等负责人才会看后端代码，其他人不会管，但是我们只有 3 个人，不能完全一个模块的代码让一个人完全独立负责。

代码依旧是有主要的负责人，但是其他成员很有可能共同参与，所以得写一些必要的说明，快速同步、对接

## 微信小程序环境匹配

后端已支持按 `Spring Profiles` 区分 `dev/uat/prod` 三套配置。

- 默认环境: `dev`
- 切换方式: 设置环境变量 `SPRING_PROFILES_ACTIVE=uat` 或 `prod`
- 登录接口: `POST /api/auth/wechat/login`
- 手机号绑定接口: `POST /api/auth/wechat/phone/bind`

### 必要环境变量

- `WX_APPID_DEV`, `WX_SECRET_DEV`
- `WX_APPID_UAT`, `WX_SECRET_UAT`
- `WX_APPID_PROD`, `WX_SECRET_PROD`
- `JWT_SECRET_DEV`, `JWT_SECRET_UAT`, `JWT_SECRET_PROD`

也可使用通用回退变量 `WX_APPID`, `WX_SECRET`, `JWT_SECRET`。
