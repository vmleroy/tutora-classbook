/* @ds-bundle: {"format":4,"namespace":"Tutora","components":[{"name":"Icon"},{"name":"Button"},{"name":"IconButton"},{"name":"Fab"},{"name":"SegmentedControl"},{"name":"Chip"},{"name":"Switch"},{"name":"Checkbox"},{"name":"Stepper"},{"name":"TextField"},{"name":"SelectField"},{"name":"Badge"},{"name":"Avatar"},{"name":"TagDot"},{"name":"Money"},{"name":"ProgressBar"},{"name":"Card"},{"name":"EarningsSummary"},{"name":"SectionHeader"},{"name":"LessonRow"},{"name":"ListGroup"},{"name":"ListRow"},{"name":"AlertBanner"},{"name":"ScreenHeader"},{"name":"TabBar"},{"name":"WeekStrip"},{"name":"AgendaBlock"},{"name":"BottomSheet"},{"name":"EmptyState"},{"name":"TimerRing"}]} */
(function () {
  var React = window.React;
  var h = React.createElement;
  var useState = React.useState;

  function cx() {
    var out = [];
    for (var i = 0; i < arguments.length; i++) if (arguments[i]) out.push(arguments[i]);
    return out.join(' ');
  }

  var PATHS = {
    plus: 'M12 5v14M5 12h14',
    minus: 'M5 12h14',
    check: 'M5 12.5l4.5 4.5L19 7.5',
    close: 'M6 6l12 12M18 6L6 18',
    'chevron-left': 'M15 5l-7 7 7 7',
    'chevron-right': 'M9 5l7 7-7 7',
    home: 'M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z',
    calendar: 'M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 10h18M8 3v4M16 3v4',
    users: 'M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M2.5 20c.8-3.5 3.4-5.5 6.5-5.5s5.7 2 6.5 5.5M16 4.5a3.5 3.5 0 0 1 0 7M18 14.8c1.8.7 3 2.5 3.5 5.2',
    pin: 'M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21zM9.5 9.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0',
    wallet: 'M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 10h18M16 15h2',
    clock: 'M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0M12 7.5V12l3 2',
    alert: 'M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 7.5v5M12 16v.5',
    file: 'M6 3h8l4 4v14H6zM14 3v4h4M9 13h6M9 17h4',
    cloud: 'M7 18a4.5 4.5 0 0 1-.5-9A6 6 0 0 1 18 8.5a4 4 0 0 1-.5 9.5zM12 11v6M9.5 13.5L12 11l2.5 2.5',
    gear: 'M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z',
    'calendar-check': 'M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 10h18M8 3v4M16 3v4M9 15l2 2 4-4',
    'no-show': 'M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 7v5l3 2M5 5l14 14'
  };

  function Icon(p) {
    var size = p.size || 22;
    if (p.name === 'play') {
      return h('svg', { width: size, height: size, viewBox: '0 0 24 24', fill: 'currentColor', 'aria-hidden': p.label ? undefined : 'true', role: p.label ? 'img' : undefined, 'aria-label': p.label, className: p.className }, h('path', { d: 'M7 4.5v15l12-7.5z' }));
    }
    return h('svg', { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: p.strokeWidth || 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': p.label ? undefined : 'true', role: p.label ? 'img' : undefined, 'aria-label': p.label, className: p.className }, h('path', { d: PATHS[p.name] || PATHS.alert }));
  }

  function Button(p) {
    var variant = p.variant || 'primary';
    var size = p.size || 'md';
    return h('button', {
      type: 'button', disabled: p.disabled || p.loading, onClick: p.onPress, 'aria-busy': p.loading ? 'true' : undefined,
      className: cx('tu-btn', 'button', 'tu-btn--' + variant, 'tu-btn--' + size, p.fullWidth && 'tu-btn--full')
    }, p.loading ? h('span', { className: 'tu-spinner', 'aria-hidden': 'true' }) : (p.icon ? h(Icon, { name: p.icon, size: size === 'sm' ? 16 : 18, strokeWidth: 2.4 }) : null), p.children);
  }

  function IconButton(p) {
    return h('button', { type: 'button', onClick: p.onPress, 'aria-label': p.label, disabled: p.disabled, className: cx('tu-iconbtn', 'tu-iconbtn--' + (p.variant || 'sunken')) }, h(Icon, { name: p.icon, size: 20, strokeWidth: 2.2 }));
  }

  function Fab(p) {
    return h('button', { type: 'button', onClick: p.onPress, 'aria-label': p.label || 'Nova aula', className: 'tu-fab' }, h(Icon, { name: p.icon || 'plus', size: 26, strokeWidth: 2.5 }));
  }

  function SegmentedControl(p) {
    var st = useState(p.value != null ? p.value : p.options[0].value);
    var value = st[0], set = st[1];
    return h('div', { role: 'radiogroup', 'aria-label': p.label, className: cx('tu-seg', p.size === 'sm' && 'tu-seg--sm'), style: { gridTemplateColumns: 'repeat(' + p.options.length + ', minmax(0, 1fr))' } },
      p.options.map(function (o) {
        var on = o.value === value;
        return h('button', { key: o.value, type: 'button', role: 'radio', 'aria-checked': on ? 'true' : 'false', className: cx('tu-seg__opt', 'button', on && 'is-on'), onClick: function () { set(o.value); if (p.onChange) p.onChange(o.value); } }, o.label);
      }));
  }

  function Chip(p) {
    var st = useState(!!p.selected);
    var on = st[0];
    return h('button', { type: 'button', 'aria-pressed': on ? 'true' : 'false', className: cx('tu-chip', 'button', on && 'is-on'), onClick: function () { st[1](!on); if (p.onPress) p.onPress(!on); } }, p.label);
  }

  function Switch(p) {
    var st = useState(!!p.value);
    var on = st[0];
    return h('button', { type: 'button', role: 'switch', 'aria-checked': on ? 'true' : 'false', 'aria-label': p.label, disabled: p.disabled, className: cx('tu-switch', on && 'is-on'), onClick: function () { st[1](!on); if (p.onChange) p.onChange(!on); } }, h('span', { className: 'tu-switch__knob' }));
  }

  function Checkbox(p) {
    var st = useState(!!p.checked);
    var on = st[0];
    return h('button', { type: 'button', role: 'checkbox', 'aria-checked': on ? 'true' : 'false', className: 'tu-check body-strong', onClick: function () { st[1](!on); if (p.onChange) p.onChange(!on); } },
      h('span', { className: cx('tu-check__box', on && 'is-on') }, on ? h(Icon, { name: 'check', size: 14, strokeWidth: 3 }) : null), p.label);
  }

  function Stepper(p) {
    var step = p.step || 15, min = p.min != null ? p.min : 15, max = p.max != null ? p.max : 300;
    var st = useState(p.value != null ? p.value : 60);
    var v = st[0];
    var fmt = p.format || function (n) { var hh = Math.floor(n / 60), mm = n % 60; return n + ' min' + (hh ? ' · ' + hh + 'h' + (mm ? String(mm).padStart(2, '0') : '') : ''); };
    return h('div', { className: cx('tu-stepper', p.size === 'sm' && 'tu-stepper--sm') },
      h('button', { type: 'button', className: 'tu-stepper__btn', 'aria-label': p.decrementLabel || 'Menos ' + step, disabled: v <= min, onClick: function () { st[1](Math.max(min, v - step)); } }, h(Icon, { name: 'minus', size: 18, strokeWidth: 2.6 })),
      h('span', { className: 'tu-stepper__value money-md', 'aria-live': 'polite' }, fmt(v)),
      h('button', { type: 'button', className: 'tu-stepper__btn', 'aria-label': p.incrementLabel || 'Mais ' + step, disabled: v >= max, onClick: function () { st[1](Math.min(max, v + step)); } }, h(Icon, { name: 'plus', size: 18, strokeWidth: 2.6 })));
  }

  var fieldId = 0;
  function TextField(p) {
    var id = React.useMemo(function () { fieldId += 1; return 'tu-field-' + fieldId; }, []);
    return h('div', { className: cx('tu-field', p.tone === 'onPrimary' && 'tu-field--on-primary', p.error && 'is-error') },
      h('label', { htmlFor: id, className: 'field-label tu-field__label' }, p.label),
      h('input', { id: id, type: 'text', className: 'body tu-field__input', placeholder: p.placeholder, defaultValue: p.value, inputMode: p.inputMode, 'aria-invalid': p.error ? 'true' : undefined, 'aria-describedby': (p.error || p.helper) ? id + '-help' : undefined }),
      (p.error || p.helper) ? h('span', { id: id + '-help', className: 'meta tu-field__help' }, p.error || p.helper) : null);
  }

  function SelectField(p) {
    return h('div', { className: 'tu-field' },
      p.label ? h('span', { className: 'field-label tu-field__label' }, p.label) : null,
      h('button', { type: 'button', className: 'tu-select', onClick: p.onPress, 'aria-label': (p.label ? p.label + ': ' : '') + p.value },
        p.avatar ? h(Avatar, { initials: p.avatar.initials, tag: p.avatar.tag, size: 'sm' }) : (p.icon ? h('span', { className: 'tu-select__icon' }, h(Icon, { name: p.icon, size: 18 })) : null),
        h('span', { className: 'tu-select__text' }, h('span', { className: 'body-strong' }, p.value), p.sub ? h('span', { className: 'meta tu-muted' }, p.sub) : null),
        p.actionLabel ? h('span', { className: 'badge tu-select__action' }, p.actionLabel) : null));
  }

  function Badge(p) {
    return h('span', { className: cx('tu-badge', 'badge', 'tu-badge--' + (p.tone || 'neutral')) }, p.icon ? h(Icon, { name: p.icon, size: 12, strokeWidth: 3 }) : null, p.children);
  }

  function Avatar(p) {
    return h('span', { className: cx('tu-avatar', 'tu-avatar--' + (p.size || 'md'), 'tu-tag-' + (p.tag || 'amber')), 'aria-hidden': 'true' }, p.initials);
  }

  function TagDot(p) {
    return h('span', { className: cx('tu-dot', 'tu-dot-' + (p.tag || 'amber'), p.size === 'sm' && 'tu-dot--sm'), 'aria-hidden': 'true' });
  }

  function formatMoney(cents, currency, locale, compact) {
    var cur = currency || 'BRL';
    var loc = locale || (cur === 'USD' ? 'en-US' : 'pt-BR');
    var opts = { style: 'currency', currency: cur };
    if (compact) { opts.minimumFractionDigits = 0; opts.maximumFractionDigits = 0; }
    return new Intl.NumberFormat(loc, opts).format(cents / 100).replace(/ /g, ' ');
  }

  function Money(p) {
    var size = p.size || 'md';
    var cls = { display: 'money-display', xl: 'money-xl', lg: 'money-lg', md: 'money-md', sm: 'row-title' }[size];
    var text = formatMoney(p.cents, p.currency, p.locale, p.compact);
    if (p.showSign && p.cents > 0) text = '+ ' + text;
    return h('span', { className: cx('tu-money', cls, 'tu-money--' + (p.tone || 'default')) }, text);
  }

  function ProgressBar(p) {
    var pct = Math.max(0, Math.min(1, p.value || 0)) * 100;
    return h('div', { className: cx('tu-progress', 'tu-progress--' + (p.track || 'accent'), p.thin && 'tu-progress--thin'), role: 'progressbar', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': Math.round(pct), 'aria-label': p.label },
      h('span', { className: cx('tu-progress__fill', 'tu-fill-' + (p.tone || 'accent')), style: { width: pct + '%' } }));
  }

  function Card(p) {
    return h(p.as || 'section', { 'aria-label': p.label, className: cx('tu-card', 'tu-card--' + (p.elevation || 'flat'), p.className) }, p.children);
  }

  function EarningsSummary(p) {
    return h(Card, { elevation: 'raised', label: p.title },
      h('div', { className: 'tu-row tu-row--between tu-row--baseline' }, h('span', { className: 'field-label tu-muted' }, p.title), p.expectedLabel ? h('span', { className: 'caption tu-muted' }, p.expectedLabel) : null),
      h(Money, { cents: p.earnedCents, size: 'xl', tone: 'earnings' }),
      p.expectedCents ? h(ProgressBar, { value: p.earnedCents / p.expectedCents, label: 'Ganho em relação ao previsto' }) : null,
      p.stats ? h('div', { className: 'tu-stats' }, p.stats.map(function (s) {
        return h('div', { key: s.label, className: 'tu-stat' }, h('span', { className: 'meta tu-muted' }, s.label), h(Money, { cents: s.cents, size: 'md', compact: true, tone: s.tone || 'default' }));
      })) : null);
  }

  function SectionHeader(p) {
    return h('div', { className: 'tu-row tu-row--between tu-row--baseline tu-section-header' }, h('h2', { className: 'section' }, p.title), p.aside ? h('span', { className: cx('caption', p.asideTone === 'primary' ? 'tu-text-primary tu-bold' : 'tu-muted') }, p.aside) : null);
  }

  function LessonRow(p) {
    return h('button', { type: 'button', className: 'tu-lesson', onClick: p.onPress },
      h('span', { className: 'row-title tu-lesson__time tu-muted tu-tabular' }, p.time),
      h(TagDot, { tag: p.tag }),
      h('span', { className: 'tu-lesson__text' }, h('span', { className: 'row-title' }, p.title), p.subtitle ? h('span', { className: 'meta tu-muted' }, p.subtitle) : null),
      p.status ? h(Badge, { tone: p.status.tone }, p.status.label) : (p.trailing ? h('span', { className: 'caption tu-muted tu-bold' }, p.trailing) : null));
  }

  function ListGroup(p) {
    return h('section', { 'aria-label': p.title, className: 'tu-group' }, p.title ? h('h2', { className: 'badge tu-group__title tu-muted' }, p.title) : null, h('div', { className: 'tu-group__body' }, p.children));
  }

  function ListRow(p) {
    var trailing = null;
    if (p.trailing === 'switch') trailing = h(Switch, { value: p.switchValue, label: p.title });
    else if (p.trailing === 'chevron' || p.trailing == null) trailing = h('span', { className: 'tu-muted', style: { display: 'flex' } }, h(Icon, { name: 'chevron-right', size: 18, strokeWidth: 2.4 }));
    var lead = p.icon ? h('span', { className: cx('tu-row-icon', 'tu-row-icon--' + (p.iconTone || 'secondary')) }, h(Icon, { name: p.icon, size: 18 })) : null;
    var text = h('span', { className: 'tu-listrow__text' }, h('span', { className: 'body-strong' }, p.title), p.subtitle ? h('span', { className: 'meta tu-muted' }, p.subtitle) : null);
    var badge = p.badge ? h(Badge, { tone: p.badge.tone }, p.badge.label) : null;
    return p.trailing === 'switch'
      ? h('div', { className: 'tu-listrow' }, lead, text, badge, trailing)
      : h('button', { type: 'button', className: 'tu-listrow', onClick: p.onPress }, lead, text, badge, trailing);
  }

  function AlertBanner(p) {
    return h('button', { type: 'button', className: cx('tu-banner', 'tu-banner--' + (p.tone || 'danger')), onClick: p.onPress },
      h(Icon, { name: p.icon || 'alert', size: 20 }), h('span', { className: 'row-title tu-banner__msg' }, p.message), p.value ? h('span', { className: 'row-title tu-bold tu-tabular' }, p.value) : null);
  }

  function ScreenHeader(p) {
    var variant = p.variant || 'plain';
    var actions = (p.actions || []).map(function (a) { return h(IconButton, { key: a.icon, icon: a.icon, label: a.label, variant: a.variant || (variant === 'hero' ? 'onPrimary' : 'sunken'), onPress: a.onPress }); });
    if (variant === 'hero') {
      return h('header', { className: 'tu-header tu-header--hero' },
        h('div', { className: 'tu-header__titles' }, p.eyebrow ? h('span', { className: 'caption tu-bold tu-on-header-muted' }, p.eyebrow) : null, h('h1', { className: 'title-xl' }, p.title)),
        h('div', { className: 'tu-row' }, actions));
    }
    return h('header', { className: 'tu-header tu-header--' + variant },
      p.onBack ? h(IconButton, { icon: variant === 'modal' ? 'close' : 'chevron-left', label: variant === 'modal' ? 'Fechar' : 'Voltar', onPress: p.onBack }) : null,
      h('div', { className: 'tu-header__titles' }, h('h1', { className: variant === 'tab' ? 'title-xl' : 'title-lg' }, p.title), p.eyebrow ? h('span', { className: 'caption tu-muted tu-bold' }, p.eyebrow) : null),
      p.action ? h('div', { style: { marginLeft: 'auto' } }, p.action) : null);
  }

  var TABS = [['today', 'Hoje', 'home'], ['schedule', 'Agenda', 'calendar'], ['students', 'Alunos', 'users'], ['places', 'Locais', 'pin'], ['earnings', 'Ganhos', 'wallet']];
  function TabBar(p) {
    var st = useState(p.active || 'today');
    var active = st[0];
    return h('nav', { 'aria-label': 'Navegação principal', className: 'tu-tabbar' }, (p.items || TABS).map(function (t) {
      var on = t[0] === active;
      return h('button', { key: t[0], type: 'button', 'aria-current': on ? 'page' : undefined, className: cx('tu-tab', 'tab', on && 'is-on'), onClick: function () { st[1](t[0]); } },
        h('span', { className: 'tu-tab__pill' }, h(Icon, { name: t[2], size: 22 })), t[1]);
    }));
  }

  function WeekStrip(p) {
    var st = useState(p.selected != null ? p.selected : 0);
    var sel = st[0];
    return h('div', { className: 'tu-week', role: 'tablist', 'aria-label': p.label || 'Dias da semana' }, p.days.map(function (d, i) {
      var on = i === sel;
      return h('button', { key: i, type: 'button', role: 'tab', 'aria-selected': on ? 'true' : 'false', 'aria-label': d.a11y || (d.weekday + ' ' + d.day), className: cx('tu-week__day', on && 'is-on'), onClick: function () { st[1](i); } },
        h('span', { className: 'badge tu-week__wd' }, d.weekday), h('span', { className: 'section' }, d.day), h('span', { className: cx('tu-week__dot', d.dot && ('tu-dot-' + d.dot), on && d.dot && 'tu-week__dot--on') }));
    }));
  }

  function AgendaBlock(p) {
    var state = p.state || 'default';
    return h('button', { type: 'button', className: cx('tu-block', 'tu-tag-' + (p.tag || 'blue'), 'tu-block--' + state), style: p.height ? { minHeight: p.height } : undefined, onClick: p.onPress },
      h(TagDot, { tag: p.tag, size: 'sm' }), h('span', { className: 'row-title' }, p.title), p.subtitle ? h('span', { className: 'meta tu-block__sub' }, p.subtitle) : null,
      state === 'done' ? h(Icon, { name: 'check', size: 16, strokeWidth: 2.6, label: 'Concluída' }) : null,
      state === 'next' ? h('span', { className: 'badge' }, 'Próxima') : null);
  }

  function BottomSheet(p) {
    return h('div', { className: 'tu-sheet-scrim' }, h('div', { role: 'dialog', 'aria-modal': 'true', 'aria-label': p.title, className: 'tu-sheet' },
      h('span', { className: 'tu-sheet__grabber', 'aria-hidden': 'true' }),
      h('div', { className: 'tu-sheet__head' }, h('h2', { className: 'title-lg' }, p.title), p.subtitle ? h('span', { className: 'caption tu-muted' }, p.subtitle) : null),
      p.children,
      p.actions ? h('div', { className: 'tu-sheet__actions' }, p.actions) : null));
  }

  function EmptyState(p) {
    return h('div', { className: 'tu-empty' },
      h('span', { className: 'tu-empty__icon' }, h(Icon, { name: p.icon || 'calendar', size: 28 })),
      h('h2', { className: 'section' }, p.title), p.body ? h('p', { className: 'caption tu-muted' }, p.body) : null, p.action || null);
  }

  function TimerRing(p) {
    var r = 120, c = 2 * Math.PI * r, prog = Math.max(0, Math.min(1, p.progress || 0));
    return h('div', { className: 'tu-ring', role: 'timer', 'aria-label': (p.label || 'Tempo de aula') + ': ' + p.elapsed },
      h('svg', { width: 260, height: 260, viewBox: '0 0 260 260', 'aria-hidden': 'true' },
        h('circle', { cx: 130, cy: 130, r: r, fill: 'none', className: 'tu-ring__track', strokeWidth: 14 }),
        h('circle', { cx: 130, cy: 130, r: r, fill: 'none', className: 'tu-ring__fill', strokeWidth: 14, strokeLinecap: 'round', strokeDasharray: c.toFixed(1), strokeDashoffset: (c * (1 - prog)).toFixed(1), transform: 'rotate(-90 130 130)' })),
      h('div', { className: 'tu-ring__center' }, h('span', { className: 'field-label tu-on-header-muted' }, p.label || 'Tempo de aula'), h('span', { className: 'money-display tu-tabular' }, p.elapsed), p.caption ? h('span', { className: 'caption tu-on-header-muted' }, p.caption) : null));
  }

  window.Tutora = Object.assign(window.Tutora || {}, {
    Icon: Icon, Button: Button, IconButton: IconButton, Fab: Fab, SegmentedControl: SegmentedControl, Chip: Chip, Switch: Switch, Checkbox: Checkbox, Stepper: Stepper,
    TextField: TextField, SelectField: SelectField, Badge: Badge, Avatar: Avatar, TagDot: TagDot, Money: Money, ProgressBar: ProgressBar, Card: Card,
    EarningsSummary: EarningsSummary, SectionHeader: SectionHeader, LessonRow: LessonRow, ListGroup: ListGroup, ListRow: ListRow, AlertBanner: AlertBanner,
    ScreenHeader: ScreenHeader, TabBar: TabBar, WeekStrip: WeekStrip, AgendaBlock: AgendaBlock, BottomSheet: BottomSheet, EmptyState: EmptyState, TimerRing: TimerRing,
    formatMoney: formatMoney
  });
})();
