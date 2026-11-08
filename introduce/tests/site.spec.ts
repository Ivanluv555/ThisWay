import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

async function revealPage(page: Page) {
  // Exercise viewport entry, then verify the content actually becomes visible.
  for (const item of await page.locator('[data-reveal]').all()) {
    await item.scrollIntoViewIfNeeded();
    await expect(item).toHaveCSS('opacity', '1');
    await expect(item).toHaveCSS('transform', 'none');
  }
}

test('完整内容、资源加载与页内导航', async ({ page }) => {
  const errors: string[] = [];
  const failed: string[] = [];
  const external: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => { if (response.status() >= 400) failed.push(response.url()); });
  page.on('requestfailed', request => failed.push(request.url()));
  page.on('request', request => {
    if (request.url().startsWith('http') && new URL(request.url()).hostname !== '127.0.0.1') external.push(request.url());
  });
  await page.goto('/');
  await expect(page).toHaveTitle('此地 ThisWay · 风景值得，人潮不必');
  await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN');
  await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('风景值得');
  await revealPage(page);
  for (const link of await page.locator('a[href^="#"]').all()) {
    const target = await link.getAttribute('href');
    expect(target).not.toBe('#');
    await expect(page.locator(target!)).toHaveCount(1);
  }
  await page.getByRole('link', { name: '探索此地', exact: true }).click();
  await expect(page).toHaveURL(/#experience$/);
  await expect(page.getByRole('tablist')).toBeInViewport();
  await expect(page.getByText('本页不连接业务服务。', { exact: false })).toBeVisible();
  expect(errors).toEqual([]);
  expect(failed).toEqual([]);
  expect(external).toEqual([]);
});

test('收藏、键盘切换与自主选择', async ({ page }) => {
  await page.goto('/#experience');
  const firstTab = page.getByRole('tab', { name: '发现想去的地方' });
  await expect(firstTab).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('button', { name: '收藏洪崖洞', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('本页已收藏 1 个景点');
  await expect(page.getByRole('button', { name: '取消收藏洪崖洞' })).toHaveAttribute('aria-pressed', 'true');
  await firstTab.focus();
  await firstTab.press('ArrowRight');
  const middleTab = page.getByRole('tab', { name: '多了解一点人潮' });
  await expect(middleTab).toBeFocused();
  await expect(middleTab).toHaveAttribute('aria-selected', 'true');
  await expect(page.locator('.crowd-row')).toHaveCount(3);
  await middleTab.press('End');
  await expect(page.getByRole('tab', { name: '自主选择下一站' })).toBeFocused();
  await page.getByRole('button', { name: /保留原计划/ }).click();
  await expect(page.getByRole('status')).toContainText('此操作不会提交任何行程');
  await page.getByRole('button', { name: /考虑其他目的地/ }).click();
  await expect(page.getByRole('status')).toContainText('此处不生成实际推荐');
  await expect(page.getByRole('button', { name: /保留原计划/ })).toHaveAttribute('aria-pressed', 'false');
  await firstTab.click();
  await expect(page.getByRole('button', { name: '取消收藏洪崖洞' })).toBeVisible();
  await page.getByRole('button', { name: '取消收藏洪崖洞' }).click();
  await expect(page.getByRole('status')).toContainText('点击书签体验收藏');
  // Verify that state is intentionally ephemeral, not stored remotely or locally.
  await page.getByRole('button', { name: '收藏洪崖洞', exact: true }).click();
  await page.reload();
  await expect(page.getByRole('button', { name: '收藏洪崖洞', exact: true })).toHaveAttribute('aria-pressed', 'false');
});

test('快速切换不会丢失内容', async ({ page }) => {
  await page.goto('/#experience');
  const tabs = page.getByRole('tab');
  for (const index of [1, 2, 0, 2, 1, 0]) await tabs.nth(index).click();
  await expect(page.locator('.journey-copy h3')).toContainText('先被风景打动');
  await expect(page.locator('.journey-copy')).toHaveCSS('opacity', '1');
  await expect(page.locator('.destination-card')).toHaveCount(3);
  await expect(page.getByRole('tabpanel')).toHaveAttribute('aria-labelledby', 'tab-discover');
});

test('移动端菜单、关闭行为与焦点', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'mobile', '仅移动布局显示菜单');
  await page.goto('/');
  const open = page.getByRole('button', { name: '打开导航菜单' });
  await open.click();
  await expect(page.getByRole('navigation', { name: '移动端导航' })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(open).toBeFocused();
  await expect(page.getByRole('navigation', { name: '移动端导航' })).toBeHidden();
  await open.click();
  await page.getByRole('navigation', { name: '移动端导航' }).getByRole('link', { name: '常见问题' }).click();
  await expect(page).toHaveURL(/#questions$/);
  await expect(open).toHaveAttribute('aria-expanded', 'false');
  await expect(page.getByRole('heading', { name: '出发之前，你可能想知道。' })).toBeInViewport();
});

test('常见问题可通过键盘展开，边界说明完整', async ({ page }) => {
  await page.goto('/#questions');
  const details = page.locator('details');
  await expect(details).toHaveCount(5);
  for (const detail of await details.all()) {
    const summary = detail.locator('summary');
    await summary.focus();
    await summary.press('Enter');
    await expect(detail).toHaveAttribute('open', '');
    await expect(detail.locator('p')).toBeVisible();
    await summary.press('Enter');
    await expect(detail).not.toHaveAttribute('open', '');
  }
  await page.locator('summary').filter({ hasText: '客流预测能预测多长时间' }).click();
  await expect(page.locator('details[open]')).toContainText('下一小时');
  await expect(page.locator('details[open]')).toContainText('尚无正式准确率评估报告');
});

test('响应式无横向溢出，输出截图', async ({ page }, testInfo) => {
  await page.goto('/');
  for (const width of [320, 375, 390, 580, 768, 800, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 950 });
    await revealPage(page);
    const dimensions = await page.evaluate(() => ({ page: document.documentElement.scrollWidth, viewport: innerWidth }));
    expect(dimensions.page, `视口 ${width} 不应横向溢出`).toBeLessThanOrEqual(dimensions.viewport);
  }
  const width = testInfo.project.name === 'mobile' ? 390 : 1440;
  await page.setViewportSize({ width, height: 1000 });
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
  await page.screenshot({ path: testInfo.outputPath('full-page.png'), fullPage: true });
  await page.locator('.hero').screenshot({ path: testInfo.outputPath('hero.png') });
});

test('语义结构与自动无障碍检查', async ({ page }) => {
  await page.goto('/');
  await revealPage(page);
  for (const tab of await page.getByRole('tab').all()) {
    await tab.click();
    await expect(page.locator('.journey-copy')).toHaveCSS('opacity', '1');
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    expect(result.violations).toEqual([]);
  }
});

test('减少动态效果模式仍保留完整交互', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'reduced-motion', '仅减少动态效果项目运行');
  await page.goto('/');
  expect(await page.evaluate(() => matchMedia('(prefers-reduced-motion: reduce)').matches)).toBe(true);
  await expect(page.locator('html')).toHaveCSS('scroll-behavior', 'auto');
  await revealPage(page);
  await page.getByRole('tab', { name: '自主选择下一站' }).click();
  await expect(page.locator('.journey-copy')).toHaveCSS('opacity', '1');
  await page.getByRole('button', { name: /保留原计划/ }).click();
  await expect(page.getByRole('status')).toContainText('已选择保留原计划');
});

test('禁用脚本时保留产品说明', async ({ browser }) => {
  const context = await browser.newContext({ javaScriptEnabled: false });
  const page = await context.newPage();
  await page.goto('http://127.0.0.1:4173');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('此地 ThisWay');
  await expect(page.getByText('请启用 JavaScript', { exact: false })).toBeVisible();
  await context.close();
});


test('顶栏与结尾展示安全的 GitHub 仓库入口', async ({ page }) => {
  await page.goto('/');
  const links = page.locator('a[href="https://github.com/Ivanluv555/ThisWay"]');
  await expect(links).toHaveCount(2);
  for (const link of await links.all()) {
    await link.scrollIntoViewIfNeeded();
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute('target', '_blank');
    await expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    await expect(link).toHaveAccessibleName(/GitHub.*新标签页打开/);
  }
  await expect(page.locator('.site-header .header-github')).toBeVisible();
  await expect(page.locator('.closing .closing-github')).toBeVisible();
  await expect(page.locator('.github-invitation')).toContainText('欢迎 Star 关注或参与贡献');
});
