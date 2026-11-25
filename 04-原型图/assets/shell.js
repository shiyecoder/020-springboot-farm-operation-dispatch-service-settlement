/* 设计稿外壳注入：侧边栏 + 顶栏，按 body[data-role] 渲染，保证 15 个页面导航一致且可点击 */
(function () {
  const ICONS = {
    dash: 'M3 3h7v7H3zM14 3h7v4h-7zM14 10h7v11h-7zM3 13h7v8H3z',
    list: 'M4 6h16M4 12h16M4 18h10',
    doc: 'M6 3h8l4 4v14H6zM14 3v4h4',
    truck: 'M3 7h11v8H3zM14 10h4l3 3v2h-7zM6 19a2 2 0 100-4 2 2 0 000 4M17 19a2 2 0 100-4 2 2 0 000 4',
    user: 'M12 12a4 4 0 100-8 4 4 0 000 8M4 21c0-4 3.6-6 8-6s8 2 8 6',
    money: 'M12 3v18M7 7h7a3 3 0 010 6H7h8a3 3 0 010 6H7',
    gear: 'M12 15a3 3 0 100-6 3 3 0 000 6M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2',
    chart: 'M4 20V10M10 20V4M16 20v-7M22 20H2'
  };
  const svg = (k) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="${ICONS[k] || ICONS.list}"/></svg>`;

  const MENUS = {
    dispatch: [
      { g: '工作台' },
      { t: '机具运营看板', h: '02-dashboard.html', i: 'dash', k: 'dashboard' },
      { t: '智能排程台', h: '06-dispatch.html', i: 'truck', k: 'dispatch', b: 4 },
      { g: '业务受理' },
      { t: '服务需求池', h: '03-request-pool.html', i: 'list', k: 'pool', b: 7 },
      { t: '报价确认', h: '05-quotation.html', i: 'money', k: 'quote' },
      { t: '验收结算单', h: '08-acceptance.html', i: 'doc', k: 'accept' },
      { g: '档案与配置' },
      { t: '机具档案', h: '09-machine.html', i: 'truck', k: 'machine' },
      { t: '农机手档案', h: '10-operator.html', i: 'user', k: 'operator' },
      { t: '服务与价目', h: '11-price.html', i: 'money', k: 'price' },
      { t: '派单规则配置', h: '13-rule-config.html', i: 'gear', k: 'rule' },
      { g: '结算' },
      { t: '结算管理', h: '12-settlement.html', i: 'chart', k: 'settle' }
    ],
    machine: [
      { g: '机务工作台' },
      { t: '机具运营看板', h: '02-dashboard.html', i: 'dash', k: 'dashboard' },
      { t: '机具档案', h: '09-machine.html', i: 'truck', k: 'machine', b: 2 },
      { t: '保养与维修', h: '09-machine.html', i: 'gear', k: 'machine' },
      { g: '人员与配置' },
      { t: '农机手档案', h: '10-operator.html', i: 'user', k: 'operator' },
      { t: '服务与价目', h: '11-price.html', i: 'money', k: 'price' }
    ],
    finance: [
      { g: '财务工作台' },
      { t: '结算管理', h: '12-settlement.html', i: 'money', k: 'settle', b: 5 },
      { t: '验收结算单', h: '08-acceptance.html', i: 'doc', k: 'accept' },
      { g: '台账' },
      { t: '机具运营看板', h: '02-dashboard.html', i: 'chart', k: 'dashboard' },
      { t: '服务与价目', h: '11-price.html', i: 'money', k: 'price' }
    ],
    farmer: [
      { g: '我的农机服务' },
      { t: '服务大厅', h: '15-portal.html', i: 'dash', k: 'portal' },
      { t: '我的需求', h: '15-portal.html', i: 'list', k: 'portal', b: 2 },
      { t: '待我验收', h: '08-acceptance.html', i: 'doc', k: 'accept', b: 1 },
      { g: '账户' },
      { t: '个人中心', h: '14-profile.html', i: 'user', k: 'profile' }
    ]
  };

  window.renderShell = function (opt) {
    const role = document.body.dataset.role || 'dispatch';
    const active = document.body.dataset.nav || '';
    const crumb = document.body.dataset.crumb || '';
    const who = opt || {};
    const menu = MENUS[role] || MENUS.dispatch;
    const side = document.getElementById('sidebar');
    if (side) {
      side.innerHTML = `<div class="brand"><div class="logo">农</div><b>农机作业调度平台</b></div>
        <nav class="nav">${menu.map(m => m.g ? `<div class="grp">${m.g}</div>`
          : `<a href="${m.h}" class="${m.k === active ? 'on' : ''}">${svg(m.i)}<span>${m.t}</span>${m.b ? `<em class="badge">${m.b}</em>` : ''}</a>`).join('')}</nav>`;
    }
    const top = document.getElementById('topbar');
    if (top) {
      top.innerHTML = `<div class="crumb">${crumb}</div><div class="spacer"></div>
        <div class="icon-btn">${svg('list')}${(who.msg !== false) ? '<i class="dot"></i>' : ''}</div>
        <div class="icon-btn">${svg('gear')}</div>
        <div class="avatar">${who.avatar || '陈'}</div>
        <div class="who">${who.name || '陈志远'}<span>${who.role || '调度员'}</span></div>`;
    }
  };
})();
