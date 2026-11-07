# 此地 CIDI · Flutter 移动端

面向移动端开发者。以 `../cidi-wx` 为业务参考，使用 Flutter / Dart 和 Material 3 实现，非 WebView 套壳。包含 Android、iOS 工程，以及用于跨平台编译验证的 Flutter Web 目标。

## 运行

```sh
flutter pub get
flutter run
```

需安装对应平台 SDK。Android 需要 Android SDK；iOS 需要 macOS、Xcode 及签名配置。

```sh
flutter analyze
flutter test
flutter build web
flutter build apk --release
```

## 功能与结构

- `lib/main.dart`：启动与暖白、森林绿主题。
- `lib/screens.dart`：探索、动态、地图附近、分流预测、个人中心、文章及景点详情、搜索、收藏、发布、登录、设置、反馈草稿。
- `lib/data/cidi_repository.dart`：接口适配、本地示例数据及持久化。
- 支持相册选择、评论、点赞、收藏、本地发布、搜索历史、下拉刷新、加载失败重试、系统定位及外部地图。

默认本地体验模式。示例预测始终标注为非实时数据，内容与互动仅保存在本机。设置中可以切换真实接口；失败不会静默降级为示例数据。真实读取使用小程序 REST 接口。

### 接入限制

- 原小程序只有微信小程序 code 登录，不能直接复用于 Flutter。当前仅提供本地体验身份，真实发布、评论及收藏写入明确拒绝，直到后端提供移动端认证流程。不要用本地用户 ID 冒充服务器身份。
- 小程序反馈页没有业务接口，当前只保存本机反馈草稿，不宣称已提交。
- 生产环境请配置 HTTPS API。Android/iOS 默认不放开全局明文流量；原小程序 HTTP 地址不能直接作为正式移动端服务。
- 地图使用 OpenStreetMap，需网络访问，定位采用系统坐标。接入国内 GCJ-02 景点数据前需要统一坐标体系。瓦片服务遵守 OpenStreetMap 使用政策，正式规模化部署应选择有授权的瓦片服务。
- Web 目标访问真实接口需要后端正确配置 CORS；移动端不受浏览器 CORS 限制。
- 当前图标为 Flutter 工程默认图标，正式发布前需替换并配置应用签名。

原小程序源码含合并冲突，本工程未修改小程序，也未将冲突直接复制到 Dart。
