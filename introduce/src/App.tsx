import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { ArrowDown, ArrowRight, ArrowUpRight, Bookmark, Check, ChevronDown, Compass, Info, Leaf, MapPin, Menu, Mountain, MoveUpRight, Route, Smartphone, Sparkles, X } from 'lucide-react';
import { Landscape } from './components/Landscape';
import { destinations, faqs, features, journeys, navigation, principles } from './content';

const githubUrl = 'https://github.com/Ivanluv555/ThisWay';

function Reveal({ children, className = '', delay = 0 }: { children: ReactNode; className?: string; delay?: number }) {
  const reduced = useReducedMotion();
  return <motion.div className={className} data-reveal initial={{ opacity: reduced ? 1 : 0, y: reduced ? 0 : 22 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true, amount: 0.12 }} transition={{ duration: reduced ? 0 : 0.65, delay: reduced ? 0 : delay }}>{children}</motion.div>;
}

function Brand({ inverse = false }: { inverse?: boolean }) {
  return <a href="#top" className={`brand ${inverse ? 'brand-inverse' : ''}`} aria-label="此地 ThisWay，返回顶部"><span className="brand-symbol"><Mountain size={25} strokeWidth={1.6} /></span><span className="brand-word">此地<span>ThisWay</span></span></a>;
}

function Header() {
  const [open, setOpen] = useState(false);
  const toggle = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const closeOnWide = () => { if (window.innerWidth > 800) setOpen(false); };
    const escape = (event: globalThis.KeyboardEvent) => {
      if (event.key === 'Escape' && open) { setOpen(false); toggle.current?.focus(); }
    };
    window.addEventListener('resize', closeOnWide);
    window.addEventListener('keydown', escape);
    return () => { window.removeEventListener('resize', closeOnWide); window.removeEventListener('keydown', escape); };
  }, [open]);
  return <header className="site-header"><div className="container header-inner">
    <Brand />
    <nav className="desktop-nav" aria-label="主导航">{navigation.map(link => <a key={link.href} href={link.href}>{link.label}</a>)}</nav>
    <div className="header-actions">
      <a className="header-cta" href="#experience">开启此地之旅 <ArrowUpRight size={17} /></a>
      <a className="github-link header-github" href={githubUrl} target="_blank" rel="noopener noreferrer" aria-label="在 GitHub 查看 ThisWay 项目（新标签页打开）" title="在 GitHub 查看 ThisWay 项目（新标签页打开）"><span>GitHub</span><ArrowUpRight size={18} aria-hidden="true" /></a>
    </div>
    <button ref={toggle} className="menu-toggle icon-button" type="button" aria-label={open ? '关闭导航菜单' : '打开导航菜单'} aria-expanded={open} aria-controls="mobile-navigation" onClick={() => setOpen(!open)}>{open ? <X /> : <Menu />}</button>
  </div>
  <nav id="mobile-navigation" className="mobile-nav" aria-label="移动端导航" hidden={!open}>{navigation.map(link => <a key={link.href} href={link.href} onClick={() => setOpen(false)}>{link.label}<ArrowUpRight size={16} /></a>)}</nav>
  </header>;
}

function Hero() {
  const reduced = useReducedMotion();
  return <section className="hero container" aria-labelledby="hero-title">
    <div className="hero-copy">
      <Reveal><div className="eyebrow"><span className="status-dot" /> 此地 ThisWay · 让旅行多一种可能</div>
      <h1 id="hero-title">风景值得，<br /><span className="hero-green">人潮不必。</span><span className="title-spark"><Sparkles strokeWidth={1.2} /></span></h1>
      <p className="hero-description">把时间留给风景，把选择留给自己。<br />从发现目的地到了解拥挤参考，<br className="desktop-break" />此地，陪你探索更从容的旅行方式。</p>
      <div className="hero-actions"><a className="button button-primary" href="#experience">探索此地 <ArrowUpRight size={19} /></a><a className="text-link" href="#how-it-works">了解如何运作 <ArrowRight size={17} /></a></div>
      <div className="hero-note"><Leaf size={16} /><span>自主选择 · 非强制分流 · 保留旅行的自由</span></div></Reveal>
    </div>
    <Reveal className="hero-art" delay={0.12}>
      <div className="landscape-frame"><Landscape /><div className="scene-label"><MapPin size={14} /> 山城意象 <span>原创场景插画</span></div></div>
      <div className="art-caption">A LITTLE LESS CROWD. A LOT MORE JOURNEY.</div>
      <motion.div className="floating-note note-top" initial={{ opacity: reduced ? 1 : 0, y: reduced ? 0 : 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: reduced ? 0 : 0.45, duration: reduced ? 0 : 0.7 }}><span className="note-icon"><Leaf size={20} /></span><div><strong>发现另一种从容</strong><span>不止热门，也可以是心之所向</span></div></motion.div>
      <motion.div className="floating-note note-bottom" initial={{ opacity: reduced ? 1 : 0, y: reduced ? 0 : 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: reduced ? 0 : 0.6, duration: reduced ? 0 : 0.7 }}><span className="note-icon"><Route size={22} /></span><div><strong>下一站，由你选择</strong><span>分流建议，不替你做决定</span></div><ArrowUpRight size={19} /></motion.div>
      <span className="art-stamp">走 向<br />此 地</span>
    </Reveal>
    <div className="hero-bottom"><span>为每一次出发，留一点余地。</span><a href="#discover" aria-label="向下了解此地"><ArrowDown size={17} /> 向下探索</a></div>
  </section>;
}

function Features() {
  return <section id="discover" className="section container" aria-labelledby="discover-title">
    <Reveal className="section-heading"><div><span className="eyebrow">MORE THAN A DESTINATION / 认识此地</span><h2 id="discover-title">好旅行，不只有一个答案。</h2></div><p>一边发现，一边了解，一边自由选择。<br />把旅行的主动权，放回你的手里。</p></Reveal>
    <div className="feature-grid">{features.map(({ icon: Icon, ...feature }, index) => <Reveal key={feature.number} delay={index * 0.07}><article className="feature-card"><div className="feature-top"><span className="feature-icon"><Icon size={25} strokeWidth={1.5} /></span><span className="feature-number">/{feature.number}</span></div><h3>{feature.title}</h3><p>{feature.description}</p><div className="feature-tag">{feature.tag}</div></article></Reveal>)}</div>
  </section>;
}

function Experience() {
  const [active, setActive] = useState(0);
  const [saved, setSaved] = useState<string[]>([]);
  const [choice, setChoice] = useState('');
  const reduced = useReducedMotion();
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  const journey = journeys[active];
  function onKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    let next: number;
    if (event.key === 'ArrowRight') next = (index + 1) % journeys.length;
    else if (event.key === 'ArrowLeft') next = (index + journeys.length - 1) % journeys.length;
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = journeys.length - 1;
    else return;
    event.preventDefault(); setActive(next); tabs.current[next]?.focus();
  }
  return <section id="experience" className="experience-section" aria-labelledby="experience-title"><div className="container">
    <Reveal className="center-heading"><span className="eyebrow">YOUR JOURNEY, YOUR CHOICE / 体验旅程</span><h2 id="experience-title">从“想去”，到“从容地去”。</h2><p>轻轻切换，看看此地如何陪伴一次出发。</p></Reveal>
    <div role="tablist" aria-label="旅行体验步骤" className="journey-tabs">{journeys.map((step, index) => <button type="button" key={step.id} ref={element => { tabs.current[index] = element; }} role="tab" id={`tab-${step.id}`} aria-selected={active === index} aria-controls="journey-panel" tabIndex={active === index ? 0 : -1} onClick={() => setActive(index)} onKeyDown={event => onKeyDown(event, index)}><step.icon size={18} /><span>{step.label}</span>{active === index && <motion.span className="tab-underline" layoutId="active-tab" transition={{ duration: reduced ? 0 : 0.3 }} />}</button>)}</div>
    <div id="journey-panel" role="tabpanel" aria-labelledby={`tab-${journey.id}`} tabIndex={0} className="experience-panel">
      <AnimatePresence mode="wait" initial={false}><motion.div key={journey.id} className="journey-copy" initial={{ opacity: reduced ? 1 : 0, y: reduced ? 0 : 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: reduced ? 0 : 0.18 }}>
        <span className="eyebrow">{journey.eyebrow}</span><h3>{journey.title}</h3><p>{journey.description}</p><ul className="check-list">{journey.details.map(detail => <li key={detail}><Check size={16} />{detail}</li>)}</ul><span className="demo-pill"><Info size={13} /> 产品流程演示，非应用实屏</span>
      </motion.div></AnimatePresence>
      <div className="preview-shell"><div className="preview-bar"><span><Compass size={17} /> 此地探索</span><span className="demo-label">DEMO / 静态示例</span></div><div className="preview-content"><div className="preview-heading"><div><h4>{journey.previewTitle}</h4><p>{journey.previewSubtitle}</p></div><span className="preview-location"><MapPin size={13} /> 重庆</span></div>
        {active === 0 && <div className="destination-grid">{destinations.map(destination => <article className="destination-card" key={destination.name}><div className="destination-image"><Landscape variant={destination.variant} /><span>{destination.kind}</span><button className={`save-button ${saved.includes(destination.name) ? 'is-saved' : ''}`} type="button" aria-label={`${saved.includes(destination.name) ? '取消收藏' : '收藏'}${destination.name}`} aria-pressed={saved.includes(destination.name)} onClick={() => setSaved(current => current.includes(destination.name) ? current.filter(name => name !== destination.name) : [...current, destination.name])}><Bookmark size={16} fill={saved.includes(destination.name) ? 'currentColor' : 'none'} /></button></div><h5>{destination.name}</h5><p>{destination.caption}</p></article>)}</div>}
        {active === 1 && <div className="crowd-list">{destinations.map(destination => <div className="crowd-row" key={destination.name}><div className="crowd-thumbnail"><Landscape variant={destination.variant} /></div><div><h5>{destination.name}</h5><p>示例拥挤状态 · 非实时</p></div><span className={`crowd-badge ${destination.statusClass}`}>{destination.status}</span></div>)}</div>}
        {active === 2 && <div className="choice-demo"><div className="choice-route"><span className="route-icon"><MapPin size={24} /></span><div><h5>行程有参考，选择不设限</h5><p>系统提供候选目的地，你决定是否采纳。</p></div></div><button className={`choice-option ${choice === 'keep' ? 'chosen' : ''}`} aria-pressed={choice === 'keep'} type="button" onClick={() => setChoice('keep')}><div><strong>保留原计划</strong><span>按自己的节奏，继续期待的旅程</span></div>{choice === 'keep' ? <Check size={19} /> : <ArrowRight size={19} />}</button><button className={`choice-option ${choice === 'explore' ? 'chosen' : ''}`} aria-pressed={choice === 'explore'} type="button" onClick={() => setChoice('explore')}><div><strong>考虑其他目的地</strong><span>多看一个选择，再决定下一站</span></div>{choice === 'explore' ? <Check size={19} /> : <ArrowRight size={19} />}</button></div>}
        <div className="preview-footer" role="status">{active === 0 ? saved.length ? `本页已收藏 ${saved.length} 个景点，刷新后重置。` : '点击书签体验收藏，仅在本页临时保留。' : active === 1 ? '以上状态为演示设定，不作为出行依据。' : choice === 'keep' ? '已选择保留原计划。此操作不会提交任何行程。' : choice === 'explore' ? '已选择考虑其他目的地。此处不生成实际推荐。' : '试着选择一种方式，体验自主决策。'}</div>
      </div></div>
    </div>
    <p className="section-disclaimer"><Info size={15} /> 本页不连接业务服务。景点状态为示例，插画非实景，所有操作仅用于演示。</p>
  </div></section>;
}

function HowItWorks() {
  const steps = [
    { label: '01', title: '从历史客流出发', text: '汇集景点信息与历史客流，为预测提供输入。历史不足时，可能使用模拟数据。', meta: '景点信息 / 历史记录' },
    { label: '02', title: '多了解下一小时', text: '通过长短期记忆网络（LSTM）预测下一小时人数，并提供拥挤量化参考。', meta: '下一小时预测 / 拥挤参考' },
    { label: '03', title: '把更多选择交给你', text: '结合距离、容量余量等因素生成替代建议，由游客自主决定是否改变行程。', meta: '候选目的地 / 自主选择' },
  ];
  return <section id="how-it-works" className="section container" aria-labelledby="how-title"><Reveal className="section-heading"><div><span className="eyebrow">THOUGHTFUL BY DESIGN / 背后原理</span><h2 id="how-title">多一份参考，<br />少一点盲目出发。</h2></div><p>把复杂的计算留在背后，<br />把清晰的选择交给旅人。</p></Reveal>
    <div className="process-grid">{steps.map((step, index) => <Reveal key={step.label} delay={index * 0.08}><article className="process-step"><div className="process-line"><span>{step.label}</span>{index < 2 && <ArrowRight size={20} />}</div><h3>{step.title}</h3><p>{step.text}</p><span className="process-meta">{step.meta}</span></article></Reveal>)}</div>
    <div className="method-note"><Info size={18} /><p><strong>让能力与边界一样清楚。</strong> 当前预测未完成正式效果评估，部分拥挤输入使用模板。预测不是实时实测，不提供准确率或出行效果保证。</p></div>
  </section>;
}

function Philosophy() {
  return <section className="philosophy" aria-labelledby="philosophy-title"><div className="container philosophy-inner"><Reveal className="philosophy-heading"><span className="eyebrow">OUR PHILOSOPHY / 此地相信</span><h2 id="philosophy-title">好的建议，<br />让你更自由。</h2><p>不替你定义目的地，<br />只为每一次选择，多点可能。</p><span className="philosophy-flower"><Compass size={82} strokeWidth={0.8} /></span></Reveal><div className="principles">{principles.map(({ icon: Icon, title, description }, index) => <Reveal key={title} delay={index * 0.05}><article><Icon size={24} strokeWidth={1.3} /><div><h3>{title}</h3><p>{description}</p></div><span>0{index + 1}</span></article></Reveal>)}</div></div></section>;
}

function ProjectStatus() {
  return <section id="project" className="section container project-section" aria-labelledby="project-title"><Reveal className="section-heading"><div><span className="eyebrow">BUILT WITH CARE / 项目进展</span><h2 id="project-title">从一个想法，走向更多旅程。</h2></div><span className="progress-label"><span className="status-dot" /> 开发与验证中</span></Reveal><div className="status-grid"><article><Smartphone size={24} strokeWidth={1.5} /><h3>游客端</h3><p>已有微信小程序与 Flutter 移动端代码，覆盖探索、社区、地图与分流展示。</p><span>移动端默认本地体验，真实写入待认证接入。</span></article><article><Route size={24} strokeWidth={1.5} /><h3>数据与模型</h3><p>Spring Boot 承载业务接口，FastAPI 与 PyTorch 提供预测及分流计算，MySQL 存储数据。</p><span>已有代码实现，不代表端到端验收通过。</span></article><article><MoveUpRight size={24} strokeWidth={1.5} /><h3>接下来的方向</h3><p>完善真实数据接入、模型评估和接口联调，逐步探索景区管理与服务协同。</p><span>管理看板、告警、票务联动属于后续规划。</span></article></div></section>;
}

function Questions() {
  return <section id="questions" className="questions-section container" aria-labelledby="questions-title"><Reveal className="questions-heading"><span className="eyebrow">GOOD TO KNOW / 常见问题</span><h2 id="questions-title">出发之前，<br />你可能想知道。</h2><p>关于此地，也关于这份演示。</p></Reveal><div className="faq-list">{faqs.map(faq => <details key={faq.question}><summary>{faq.question}<ChevronDown size={19} /></summary><p>{faq.answer}</p></details>)}</div></section>;
}

function Footer() {
  return <><section className="closing container" aria-labelledby="closing-title"><Reveal><span className="eyebrow">SEE YOU OUT THERE</span><h2 id="closing-title">世界很大，<br className="mobile-break" />不必挤在一处。</h2><p>下一次出发，给风景多一点时间。</p><div className="closing-actions"><a href="#experience" className="button button-primary">再探索一种可能 <ArrowUpRight size={19} /></a><a href={githubUrl} className="button github-link closing-github" target="_blank" rel="noopener noreferrer" aria-label="前往 GitHub 查看源码与项目进展（新标签页打开）">前往 GitHub <ArrowUpRight size={19} aria-hidden="true" /></a></div><p className="github-invitation">查看源码与项目进展，欢迎 Star 关注或参与贡献。</p></Reveal><span className="closing-decoration" aria-hidden="true"><Mountain size={160} strokeWidth={0.6} /></span></section><footer className="footer"><div className="container"><div className="footer-main"><div><Brand /><p>多景区游客协调分流系统<br />发现不拥挤的旅行方式。</p></div><nav aria-label="页脚导航">{navigation.map(link => <a key={link.href} href={link.href}>{link.label}</a>)}</nav><a href="#top" className="back-top">回到顶部 <ArrowUpRight size={17} /></a></div><div className="footer-bottom"><span>此地 ThisWay · 产品介绍</span><span>静态演示 · 非实时旅游信息 · 无购票与预约服务</span><a href="#project">查看项目进展 <ArrowUpRight size={13} /></a></div></div></footer></>;
}

export default function App() {
  return <><a className="skip-link" href="#main">跳到主要内容</a><div id="top" /><Header /><main id="main"><Hero /><Features /><Experience /><HowItWorks /><Philosophy /><ProjectStatus /><Questions /></main><Footer /></>;
}
