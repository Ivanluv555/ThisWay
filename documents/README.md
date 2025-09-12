<h1 style="text-align: center;">documents文件说明



- 先设计文档，再根据设计文档实际开发。整理一个开发过程中的文档说明。

- 最好是加个后缀控制版本。（_v1）
- 自行添加、修改、说明。



# shell一键创建文件



```shell
:: 创建设计文档（带_v1版本）
type nul > des_api_v1.md
type nul > des_dbs_v1.md
type nul > des_sys_v1.md
type nul > des_UI_v1.md

:: 创建开发文档（带_v1版本）
type nul > dev_web_v1.md
type nul > dev_server_v1.md
type nul > dev_config_v1.md
type nul > dev_op_v1.md

:: 创建建模文档（带_v1版本）
type nul > mod_visitor_v1.md

:: 查看已创建的文件（可选）
dir *.md
```





# 设计文档（design）



| 文件名     | 内容说明         |
| ---------- | ---------------- |
| des_api.md | 接口设计文档     |
| des_dbs.md | 数据库设计文档   |
| des_sys.md | 系统架构设计文档 |
| des_UI.md  | UI/UX 设计文档   |
| ...        | ...              |



# 开发文档（develop）



| 文件名        | 内容说明     |
| ------------- | :----------- |
| dev_web.md    | 前端开发文档 |
| dev_server.md | 后端开发文档 |
| dev_config.md | 开发环境配置 |
| dev_op.md     | 部署运维文档 |
| ...           | ...          |



# 建模文档（module）



| 文件名         | 内容说明 |
| -------------- | -------- |
| mod_visitor.md | 游客模型 |
| ...            | ...      |



