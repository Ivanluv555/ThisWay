import { Compass, MapPinned, Users, Route, ScanEye, Heart } from 'lucide-react';

export const navigation = [
  { href: '#discover', label: '认识此地' },
  { href: '#experience', label: '体验旅程' },
  { href: '#how-it-works', label: '背后原理' },
  { href: '#questions', label: '常见问题' },
];

export const features = [
  { icon: Compass, number: '01', title: '不止热门，更有合适', description: '从景点列表、搜索和详情出发，发现想去的地方。把喜欢的目的地收藏起来，留给下一次出发。', tag: '探索 · 搜索 · 收藏' },
  { icon: MapPinned, number: '02', title: '看见风景，也看见人潮', description: '结合地图、下一小时客流预测与拥挤参考，多了解一点目的地，再决定什么时候去、去哪里。', tag: '地图 · 客流预测 · 拥挤参考' },
  { icon: Users, number: '03', title: '让旅途，彼此有回响', description: '浏览旅行动态和文章，通过评论与点赞交流灵感。好风景之外，也留下属于自己的旅行故事。', tag: '动态 · 文章 · 互动' },
];

export const journeys = [
  { id: 'discover', icon: Compass, label: '发现想去的地方', eyebrow: '01 / DISCOVER', title: '先被风景打动，\n再慢慢做决定。', description: '搜索心里的目的地，也从景点列表里寻找新灵感。景点详情和旅行内容，让下一站不只停留在一个名字。', details: ['浏览景点信息，认识目的地', '收藏旅行灵感，保留自己的清单'], previewTitle: '每一种向往，都有此地', previewSubtitle: '景点探索 · 静态示例', mode: 'discover' },
  { id: 'understand', icon: ScanEye, label: '多了解一点人潮', eyebrow: '02 / UNDERSTAND', title: '出发前，\n多一份从容的参考。', description: '系统利用历史客流预测下一小时人数，并提供拥挤参考。预测不是现场保证，更不能替代景区的开放公告与安全提示。', details: ['了解下一小时客流预测', '区分模型参考与现场实际情况'], previewTitle: '热闹，还是从容？', previewSubtitle: '拥挤状态 · 仅为演示设定', mode: 'understand' },
  { id: 'choose', icon: Route, label: '自主选择下一站', eyebrow: '03 / CHOOSE', title: '换一种走法，\n不减少一分期待。', description: '综合距离、容量余量等因素，系统生成可供考虑的替代目的地。继续原计划，或换一个方向，决定权始终在你。', details: ['参考替代目的地建议', '保留原计划，不强制分配行程'], previewTitle: '下一站，由你选择', previewSubtitle: '交互说明 · 不生成真实推荐', mode: 'choose' },
] as const;

export const destinations = [
  { name: '洪崖洞', caption: '层叠灯火，山城印象', variant: 'city' as const, kind: '城市漫游', status: '较拥挤', statusClass: 'busy' },
  { name: '磁器口古镇', caption: '街巷之间，慢慢行走', variant: 'town' as const, kind: '街巷慢游', status: '较舒适', statusClass: 'calm' },
  { name: '武隆天生三桥', caption: '山谷深处，自然回响', variant: 'mountain' as const, kind: '自然探索', status: '较舒适', statusClass: 'calm' },
];

export const principles = [
  { icon: ScanEye, title: '参考，而非承诺', description: '模型帮助理解可能的客流变化，不承诺准确率，也不替代实地判断。' },
  { icon: Route, title: '建议，而非安排', description: '推荐提供更多选择。是否前往、如何安排行程，始终由游客自主决定。' },
  { icon: Heart, title: '旅行，不止效率', description: '保留探索与分享的乐趣，让行程有方向，也给偶遇留一点空间。' },
];

export const faqs = [
  { question: '此地 ThisWay 是什么？', answer: '此地 ThisWay 是一个多景区游客协调分流系统，将景点信息、旅行社区、客流预测和替代目的地建议结合起来。它帮助游客自主选择行程，而不是强制分配游客。此页面为独立的静态产品介绍页。' },
  { question: '页面上的景点状态是真实、实时的吗？', answer: '不是。本页的景点状态、收藏交互和场景插画仅用于演示，不连接后端、定位服务或真实地图，不生成预测，也不代表景点实时拥挤程度。请以景区官方公告和现场情况为准。' },
  { question: '客流预测能预测多长时间？', answer: '当前模型实现基于历史客流的下一小时人数预测。已有接口中的多小时目的地展示重复使用最新一轮推荐，不能理解为连续多小时独立预测。历史不足时可能使用模拟数据，模型尚无正式准确率评估报告。' },
  { question: '现在可以下载应用或直接预约吗？', answer: '仓库已有微信小程序和 Flutter 移动端代码，但本页不提供已上线应用、下载包或小程序码。Flutter 默认本地体验，真实写入仍需移动端认证接入；小程序存在待处理的合并冲突。当前不提供完整购票、预约、实际发券或核销流程。' },
  { question: '这是完整上线的系统吗？', answer: '不是。项目已有业务与模型代码，仍需处理接口联调、真实数据接入、认证和端到端验证。景区管理看板、告警与票务联动属于后续规划，不能作为当前已交付能力。介绍页本身不采集个人信息，不请求定位，也不向业务服务提交数据。' },
];
