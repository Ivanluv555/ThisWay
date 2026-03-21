<h1 align='center' style='font-size: 40px'>
    <a href="https://gitee.com/daiso_1/daiso" style="text-decoration: none; color: black;">基于人工智能的多景区游客协调分流系统开发</a>
</h1>

<div align='center'>
    <img src="https://img.shields.io/badge/License-Apache2.0-green">
    <img src="https://img.shields.io/badge/Version-0.1-blue">
    <img src="https://img.shields.io/badge/创新训练项目-red">
</div>
<div align='center'>
    <img src="https://img.shields.io/badge/Leader-刘博洋-gold">
    <img src="https://img.shields.io/badge/Member-韩方俊-aqua">
    <img src="https://img.shields.io/badge/Member-代双遥-aqua">
</div> 

---

### 项目介绍

国内旅游出行领域受庞大人口基数与集中化公众假期影响，旅游需求短期集中释放，致使景区陷入高峰期超负荷、平峰期资源闲置的两极困境，加之现有疏导机制缺乏景区间协同与景区内精细化分流能力，出现热门景区超载、周边潜力景区资源闲置的问题，还引发游览体验下降、安全风险、设施损耗与生态破坏等连锁问题，且当前分流手段缺少数据与智能预测支撑，无法从根源解决资源错配难题；本项目依托多源数据追踪旅游高峰期人员动向，结合数学建模与人工智能预测量化景区拥挤程度，通过多景区协同调度算法实现游客合理分配，破解旅游资源利用不均与景区拥堵问题。

> 注意：我们的软件项目并不能强制限制游客的行为，只是为游客推荐旅行计划，真正能协调游客流量只能景区进行限流，所以要实现协调分流这一目的的话，只能是给景区的管理端发送游客流量警报，然后关闭售票通道，或者关闭预约通道，也不能将已经到达景区的人直接不让进了吧，到都到了才说人满了，可以根据历史游客数量进行预防，提前进行一些限制手段

---

### 系统架构图

> 分层架构层只写 “该系统有什么”
> 可以补充团队开发过程中用到的工具

![anon-bob-2_a_帮我生成图片：旅游软件分层架构_compressed](https://gitee.com/kualk/pic-go/raw/master/imgs/anon-bob-2_a_帮我生成图片：旅游软件分层架构_compressed.png)

> AI 读取项目的时候，图片内容读取可能会遇到一些问题，所以这里给出了文字版本的项目架构，可以随时修改文字版本进行画图调整。架构图最终还是回归到手画，AI 仅为设计参考

- 前端层
    1. 游客用户端
        - 景区信息查询
        - 智能路线规划
        - 游客个人中心
        - 旅游经验交流
    2. 景区管理端
        - 景区数据监控
        - 景区配置管理
        - 线上通道控制
        - 游客超载预警
        - 信息广播
- 后端层
    1. 应用服务端
        - 核心模块
            - Controller
            - Service
            - Mapper
            - Configuration
            - 任务调度模块
       - 核心技术
            - SpringBoot 
            - Spring Security
            - MyBatis
            - Redis
            - WebSocket
    2. 模型算法端
        - 核心模型
            - 景区拥挤度量化模型
            - 基于深度学习的游客流量预测
            - 多景区协同调度与路线优化算法
        - 核心模块
            - 多源数据清洗
            - 时间序列插值
            - 异常值检测
- 数据层
    1. MySQL
        - 景区信息表
        - 客流数据表
        - 用户信息表
        - 系统配置表
    2. Redis
        - 实时客流指标
        - 热门景区缓存
        - 用户 Session

---
