/* «Питомец Финни» — интерактивный прототип игрового цикла.
   Локальный профиль (localStorage), без сервера и регистрации.
   Воспроизводит обязательный сценарий ТЗ: онбординг → создание питомца →
   доход → план бюджета → задания → покупки → цели/накопления →
   итоги периода → раздел взрослого → демонстрационный режим. */

'use strict';

const LS_KEY = 'finni_prototype_v1';
const START_BALANCE = 40;
const DAILY_INCOME = 20;
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

const BODIES = [
  { id: 'dog', emoji: '🐶', name: 'Собачка' },
  { id: 'cat', emoji: '🐱', name: 'Котёнок' },
  { id: 'rabbit', emoji: '🐰', name: 'Зайчонок' },
];
const COLORS = [
  { id: 'natural', css: '', name: 'Природный' },
  { id: 'sunny', css: 'hue-rotate(35deg) saturate(1.3)', name: 'Солнечный' },
  { id: 'berry', css: 'hue-rotate(-45deg) saturate(1.4)', name: 'Ягодный' },
];
const ACCESSORIES = [
  { id: 'none', emoji: '', name: 'Без аксессуара' },
  { id: 'bow', emoji: '🎀', name: 'Бантик' },
  { id: 'crown', emoji: '👑', name: 'Корона' },
];
const STAGES = [
  { id: 0, name: 'Малыш', need: '' },
  { id: 1, name: 'Друг', need: 'Средний балл решений 0.6+' },
  { id: 2, name: 'Звезда', need: 'Балл 0.8+ за 5 периодов' },
];
const TOPICS = {
  BUDGET_PLANNING: 'Планирование бюджета',
  SAVINGS: 'Накопления',
  PURCHASES: 'Покупки и платежи',
};

let CONTENT = { tasks: [], shop: [], goals: [], glossary: [] };
let state = null;
let screen = 'main';
let taskRuntime = null; // {taskId, step, picked}

/* ---------- загрузка ---------- */
async function load() {
  const [tasks, shop, goals, glossary] = await Promise.all(
    ['tasks', 'shop', 'goals', 'glossary'].map((n) =>
      fetch('content/' + n + '.json').then((r) => r.json())
    )
  );
  CONTENT = { tasks, shop, goals, glossary };
  state = loadState();
  render();
}

function defaultState() {
  return {
    profile: null,
    onboarded: false,
    periodIndex: 1,
    plan: null, // {m, o, s} — подтверждённый план текущего периода
    draftPlan: { m: 0, o: 0, s: 0 },
    fact: { m: 0, o: 0, s: 0 },
    mandatoryBought: false,
    periodPurchases: [],
    closedPeriods: [], // [{m,o,s,fm,fo,fs,mandatoryCovered,savingsMet,adherence,score}]
    tasksProgress: {}, // id -> {completed, attempts, lastCorrect}
    tx: [], // {type, amount, title, at}
    lastDaily: null,
    demo: false,
  };
}
function loadState() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) return Object.assign(defaultState(), JSON.parse(raw));
  } catch (e) { /* повреждено — начинаем заново */ }
  return defaultState();
}
function save() {
  localStorage.setItem(LS_KEY, JSON.stringify(state));
}

/* ---------- экономика ---------- */
function addTx(type, amount, title) {
  state.tx.unshift({ type, amount, title, at: Date.now() });
  state.tx = state.tx.slice(0, 60);
}

function canClosePlan(draft) {
  const p = state.profile;
  return draft.m + draft.o + draft.s <= p.balance;
}

function buyItem(item) {
  const p = state.profile;
  if (item.price > p.balance) {
    const lack = item.price - p.balance;
    toast(
      `Не хватает ${lack} монет. Варианты: выполнить задание и заработать, ` +
      `выбрать подешевле или отложить покупку.`
    );
    return false;
  }
  p.balance -= item.price;
  const cat = item.category === 'MANDATORY' ? 'm' : 'o';
  state.fact[cat] += item.price;
  if (item.category === 'MANDATORY') state.mandatoryBought = true;
  p.mood = clamp(p.mood + (item.effect.mood || 0), 0, 100);
  p.satiety = clamp(p.satiety + (item.effect.satiety || 0), 0, 100);
  state.periodPurchases.push({ name: item.name, price: item.price, cat });
  addTx('PURCHASE_' + item.category, item.price, item.name);
  save();
  return true;
}

function toSavings(amount) {
  const p = state.profile;
  amount = Math.min(amount, p.balance);
  if (amount <= 0) return;
  p.balance -= amount;
  p.savings += amount;
  state.fact.s += amount;
  addTx('SAVINGS_IN', amount, 'В накопления');
  save();
}

function fromSavings(amount) {
  const p = state.profile;
  amount = Math.min(amount, p.savings);
  if (amount <= 0) return;
  const goal = CONTENT.goals.find((g) => g.id === p.goalId);
  p.savings -= amount;
  p.balance += amount;
  state.fact.s = Math.max(0, state.fact.s - amount);
  addTx('SAVINGS_OUT', amount, 'Снято с накоплений');
  save();
  if (goal) {
    const rest = Math.max(0, goal.price - p.savings);
    toast(`Снято ${amount} монет. До цели «${goal.name}» теперь ${rest} монет.`);
  }
}

function claimDaily() {
  const today = new Date().toDateString();
  if (!state.demo && state.lastDaily === today) {
    toast('Ежедневный доход уже получен. Загляни завтра!');
    return;
  }
  state.profile.balance += DAILY_INCOME;
  state.lastDaily = today;
  addTx('INCOME_DAILY', DAILY_INCOME, 'Ежедневный доход');
  save();
  toast(`+${DAILY_INCOME} монет — ежедневный доход!`);
}

function periodScore(c) {
  return 0.4 * (c.mandatoryCovered ? 1 : 0) +
         0.3 * c.adherence +
         0.3 * (c.savingsMet ? 1 : 0);
}

function closePeriod() {
  const p = state.profile;
  const plan = state.plan || { m: 0, o: 0, s: 0 };
  const dirs = ['m', 'o', 's'];
  let okDirs = 0;
  dirs.forEach((d) => {
    const dev = Math.abs(state.fact[d] - plan[d]);
    const limit = Math.max(plan[d] * 0.2, plan[d] === 0 && state.fact[d] === 0 ? 0 : 2);
    if (dev <= limit) okDirs++;
  });
  const rec = {
    index: state.periodIndex,
    m: plan.m, o: plan.o, s: plan.s,
    fm: state.fact.m, fo: state.fact.o, fs: state.fact.s,
    mandatoryCovered: state.mandatoryBought,
    savingsMet: state.fact.s >= plan.s,
    adherence: okDirs / 3,
  };
  rec.score = periodScore(rec);
  state.closedPeriods.push(rec);

  // состояние питомца: голод/грусть обратимы, без «смерти»
  let msg;
  if (!rec.mandatoryCovered) {
    p.mood = clamp(p.mood - 15, 0, 100);
    p.satiety = clamp(p.satiety - 20, 0, 100);
    msg = 'Обязательные покупки не закрыты — Финни голодный и грустный. В новом периоде сначала купи корм!';
  } else if (rec.adherence >= 0.99) {
    p.mood = clamp(p.mood + 10, 0, 100);
    msg = 'План выполнен точно! Финни гордится тобой.';
  } else {
    msg = 'Период завершён. Сравни план и факт — и улучши следующий период!';
  }

  // стадия развития (не понижается)
  const avg = state.closedPeriods.reduce((a, c) => a + c.score, 0) / state.closedPeriods.length;
  let newStage = p.stage;
  if (avg >= 0.8 && state.closedPeriods.length >= 5) newStage = 2;
  else if (avg >= 0.6) newStage = Math.max(newStage, 1);
  if (newStage > p.stage) {
    msg += ` 🎉 Финни вырос: новая стадия «${STAGES[newStage].name}»!`;
    p.stage = newStage;
  }

  // новый период
  state.periodIndex += 1;
  state.plan = null;
  state.draftPlan = { m: 0, o: 0, s: 0 };
  state.fact = { m: 0, o: 0, s: 0 };
  state.mandatoryBought = false;
  state.periodPurchases = [];
  save();
  return msg;
}

/* ---------- рендер ---------- */
const app = () => document.getElementById('app');
function esc(s) {
  return String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}
function toast(text) {
  const t = document.createElement('div');
  t.className = 'toast';
  t.textContent = text;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 4200);
}

function petEmojiHtml(big) {
  const p = state.profile;
  const body = BODIES.find((b) => b.id === p.body) || BODIES[0];
  const color = COLORS.find((c) => c.id === p.color) || COLORS[0];
  const acc = ACCESSORIES.find((a) => a.id === p.accessory) || ACCESSORIES[0];
  const moodClass = p.mood >= 60 ? 'happy' : p.mood <= 30 ? 'sad' : '';
  return `<span class="pet-emoji ${moodClass}" style="filter:${color.css || 'none'}">${body.emoji}</span>` +
    (acc.emoji ? `<span class="pet-accessory">${acc.emoji}</span>` : '');
}
function petMessage() {
  const p = state.profile;
  if (p.satiety < 35) return 'Финни голодный… Сытость падает, нужен корм!';
  if (p.mood < 35) return 'Финни скучает. Игрушка или выполненный план его взбодрят!';
  if (p.mood > 75 && p.satiety > 75) return 'Финни счастлив, сыт и готов играть!';
  return 'Финни ждёт твоих решений: потратить на обязательное, на желаемое или отложить.';
}

function topbar(title, backTo) {
  return `<div class="topbar">
    ${backTo ? `<button class="back" data-go="${backTo}" aria-label="Назад">←</button>` : '<span></span>'}
    <h1 style="flex:1;text-align:center">${title}</h1>
    <span></span>
  </div>`;
}

function render() {
  const r = app();
  let html = '';
  if (state.demo) html += '<div class="demo-flag">ДЕМО-РЕЖИМ</div>';

  if (!state.onboarded) html = viewOnboarding();
  else if (!state.profile) html = viewCreateProfile();
  else {
    switch (screen) {
      case 'main': html = viewMain(); break;
      case 'plan': html = viewPlan(); break;
      case 'shop': html = viewShop(); break;
      case 'goals': html = viewGoals(); break;
      case 'tasks': html = viewTasks(); break;
      case 'task': html = viewTask(); break;
      case 'periodEnd': html = viewPeriodEnd(); break;
      case 'history': html = viewHistory(); break;
      case 'adult': html = viewAdultGate(); break;
      case 'adultHome': html = viewAdultHome(); break;
      case 'help': html = viewHelp(); break;
      default: html = viewMain();
    }
  }
  r.innerHTML = html;
  bind();
}

/* --- экраны --- */
function viewOnboarding() {
  return `<div class="card" style="text-align:center">
    <div class="pet-emoji" style="font-size:5rem">🐶</div>
    <h1>Питомец Финни</h1>
    <p>Научись управлять деньгами, заботясь о питомце. Всего три типа решений:</p>
  </div>
  <div class="card">
    <p>🍖 <b>Потратить на обязательное</b> — еда и уход. Без них Финни грустит.</p>
    <p>🎾 <b>Потратить на желаемое</b> — игрушки и украшения. Приятно, но можно и подождать.</p>
    <p>🏦 <b>Отложить</b> — копи на большую цель понемногу каждый период.</p>
  </div>
  <button data-action="finishOnboarding" style="width:100%">Понятно, начнём!</button>
  <p class="bottom-note">Гостевой режим: без имени, телефона и e-mail. Прогресс хранится только на этом устройстве.</p>`;
}

function viewCreateProfile() {
  const draft = state.profileDraft || {
    childName: '', petName: '', body: 'dog', color: 'natural', accessory: 'none',
  };
  state.profileDraft = draft;
  const pickRow = (items, key, renderItem) =>
    `<div class="pick-row">${items.map((it) =>
      `<button class="pick ${draft[key] === it.id ? 'selected' : ''}" data-pick="${key}:${it.id}">${renderItem(it)}</button>`
    ).join('')}</div>`;

  return `${topbar('Создай питомца')}
  <div class="card" style="text-align:center">${petPreviewHtml(draft)}</div>
  <div class="card">
    <label><b>Твоё игровое имя</b></label>
    <input type="text" id="childName" maxlength="20" placeholder="Например, Соня" value="${esc(draft.childName)}" />
    <label><b>Имя питомца</b></label>
    <input type="text" id="petName" maxlength="20" placeholder="Например, Финни" value="${esc(draft.petName)}" />
    <b>Кто твой питомец?</b>
    ${pickRow(BODIES, 'body', (b) => b.emoji)}
    <b>Окрас</b>
    ${pickRow(COLORS, 'color', (c) => `<span style="font-size:1rem">⬤</span>`)}
    <b>Аксессуар</b>
    ${pickRow(ACCESSORIES, 'accessory', (a) => a.emoji || '∅')}
    <button data-action="createProfile" style="width:100%">Готово, знакомиться!</button>
  </div>`;
}
function petPreviewHtml(d) {
  const body = BODIES.find((b) => b.id === d.body) || BODIES[0];
  const color = COLORS.find((c) => c.id === d.color) || COLORS[0];
  const acc = ACCESSORIES.find((a) => a.id === d.accessory) || ACCESSORIES[0];
  return `<span class="pet-emoji" style="filter:${color.css || 'none'}">${body.emoji}</span>` +
    (acc.emoji ? `<span class="pet-accessory">${acc.emoji}</span>` : '') +
    `<div class="pet-name">${esc(d.petName || 'Твой питомец')}</div>`;
}

function viewMain() {
  const p = state.profile;
  const goal = CONTENT.goals.find((g) => g.id === p.goalId);
  const nextTask = CONTENT.tasks.find((t) => !state.tasksProgress[t.id]?.completed);
  const stage = STAGES[p.stage];
  return `
  <div class="topbar">
    <button class="back" data-go="help" aria-label="Подсказка">?</button>
    <h1 style="flex:1;text-align:center">Период ${state.periodIndex}</h1>
    <button class="back" data-go="adult" aria-label="Взрослый">🔒</button>
  </div>
  <div class="card pet-stage">
    ${petEmojiHtml()}
    <div class="pet-name">${esc(p.petName)} · ${stage.name}</div>
    <div class="pet-msg">${petMessage()}</div>
    <div style="max-width:300px;margin:10px auto 0">
      <div class="bar-wrap"><div class="bar-label"><span>Настроение</span><span>${p.mood}/100</span></div><div class="bar"><div class="mood-fill" style="width:${p.mood}%"></div></div></div>
      <div class="bar-wrap"><div class="bar-label"><span>Сытость</span><span>${p.satiety}/100</span></div><div class="bar"><div class="sat-fill" style="width:${p.satiety}%"></div></div></div>
    </div>
  </div>
  <div class="card" style="display:flex;gap:10px;justify-content:space-between;align-items:center">
    <div><div style="font-size:0.85rem;color:var(--muted)">Баланс</div><div class="chip">🪙 ${p.balance}</div></div>
    <div><div style="font-size:0.85rem;color:var(--muted)">Накопления</div><div class="chip savings">🏦 ${p.savings}</div></div>
    <div><div style="font-size:0.85rem;color:var(--muted)">Цель</div><div class="chip">${goal ? esc(goal.name) + ' · ' + goal.price : 'не выбрана'}</div></div>
  </div>
  <div class="card">
    <b>Активное задание:</b>
    ${nextTask ? `<p>${esc(nextTask.title)} — «${TOPICS[nextTask.topic]}»</p><button data-go="tasks" style="width:100%">К заданиям</button>`
               : '<p>Все задания выполнены! Отличная работа. 🎉</p>'}
  </div>
  <div class="nav-grid">
    <button data-go="plan"><span>📋</span>План бюджета<small>${state.plan ? 'план подтверждён' : 'распределить монеты'}</small></button>
    <button data-go="shop"><span>🛒</span>Покупки<small>обязательные и желаемые</small></button>
    <button data-go="goals"><span>🏦</span>Накопления<small>цель и пополнение</small></button>
    <button data-go="history"><span>📈</span>Прогресс<small>итоги и история</small></button>
  </div>
  <div class="btn-row">
    <button class="secondary" data-action="claimDaily">🪙 +${DAILY_INCOME} ежедневный доход</button>
  </div>
  <button data-action="closePeriod" style="width:100%;margin-top:12px" class="outline">Завершить период и посмотреть итоги</button>`;
}

function viewPlan() {
  const p = state.profile;
  if (state.plan) {
    const f = state.fact;
    return `${topbar('План бюджета', 'main')}
    <div class="card">
      <p>План на период ${state.periodIndex} подтверждён. Вот сравнение с фактом:</p>
      ${pvfTable(state.plan, f)}
      <p style="color:var(--muted);font-size:0.9rem">План больше изменить нельзя — но следующий период ты составишь ещё лучше!</p>
      <div class="btn-row">
        <button class="secondary" data-go="main">На главный</button>
        <button data-go="shop">К покупкам</button>
      </div>
    </div>`;
  }
  const d = state.draftPlan;
  const sum = d.m + d.o + d.s;
  const over = sum > p.balance;
  return `${topbar('План бюджета', 'main')}
  <div class="card">
    <p>Распредели <b>${p.balance} монет</b> по трём направлениям. План нельзя подтвердить, если сумма больше баланса.</p>
    ${planSlider('m', '🍖 Обязательные расходы', 'Еда и уход для Финни', d.m, p.balance)}
    ${planSlider('o', '🎾 Необязательные расходы', 'Игрушки и украшения', d.o, p.balance)}
    ${planSlider('s', '🏦 Накопления', 'На финансовую цель', d.s, p.balance)}
    <div class="remainder ${over ? 'over' : ''}">
      Итого плана: ${sum} из ${p.balance} · Остаток: ${p.balance - sum}
      ${over ? '<br>⚠️ Сумма превышает бюджет — уменьши одно из направлений.' : ''}
    </div>
    <button data-action="confirmPlan" style="width:100%;margin-top:12px" ${over ? 'disabled' : ''}>Подтвердить план</button>
  </div>`;
}
function planSlider(key, title, hint, val, max) {
  return `<div class="plan-row">
    <div class="plan-head"><span>${title}</span><span class="val" id="val-${key}">${val} 🪙</span></div>
    <div style="color:var(--muted);font-size:0.85rem">${hint}</div>
    <input type="range" min="0" max="${max}" step="1" value="${val}" data-slider="${key}" />
  </div>`;
}
function pvfTable(plan, fact) {
  const row = (name, pl, fa) => {
    const dev = Math.abs(fa - pl);
    const limit = Math.max(pl * 0.2, pl === 0 && fa === 0 ? 0 : 2);
    const okMark = dev <= limit ? '<span class="ok">✓</span>' : '<span class="bad">±' + dev + '</span>';
    return `<tr><td>${name}</td><td class="num">${pl}</td><td class="num">${fa}</td><td class="num">${okMark}</td></tr>`;
  };
  return `<table class="pvf"><tr><th>Направление</th><th class="num">План</th><th class="num">Факт</th><th class="num">Откл.</th></tr>
    ${row('🍖 Обязательные', plan.m, fact.m)}
    ${row('🎾 Необязательные', plan.o, fact.o)}
    ${row('🏦 Накопления', plan.s, fact.s)}
  </table>`;
}

function viewShop() {
  const p = state.profile;
  const item = (it) => `<div class="shop-item">
      <div class="info">
        <div class="name">${esc(it.name)}</div>
        <div><span class="tag ${it.category === 'MANDATORY' ? 'mandatory' : 'optional'}">${it.category === 'MANDATORY' ? 'обязательное' : 'желаемое'}</span><b>${it.price} 🪙</b></div>
        <div class="hint">${esc(it.influenceHint)}</div>
      </div>
      <button data-buy="${it.id}" ${it.price > p.balance ? 'class="secondary"' : ''}>Купить</button>
    </div>`;
  return `${topbar('Магазин', 'main')}
  <div class="card">
    <p>Баланс: <b>${p.balance} 🪙</b>. Перед покупкой смотри цену, категорию и влияние на Финни.</p>
    ${CONTENT.shop.map(item).join('')}
  </div>
  <div class="card">
    <b>Куплено в этом периоде:</b>
    ${state.periodPurchases.length
      ? '<ul class="footnote-list">' + state.periodPurchases.map((x) => `<li>${esc(x.name)} — ${x.price} 🪙</li>`).join('') + '</ul>'
      : '<p style="color:var(--muted)">Пока пусто.</p>'}
  </div>`;
}

function viewGoals() {
  const p = state.profile;
  const goal = CONTENT.goals.find((g) => g.id === p.goalId);
  return `${topbar('Цели и накопления', 'main')}
  <div class="card">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>Накоплено: <b>${p.savings} 🪙</b></div>
      <div>${goal ? 'Цель: <b>' + esc(goal.name) + '</b>' : 'Цель не выбрана'}</div>
    </div>
    ${goal ? `<div class="progress"><div style="width:${clamp((p.savings / goal.price) * 100, 0, 100)}%"></div></div>
    <p style="font-size:0.9rem;color:var(--muted)">Цена: ${goal.price} · осталось собрать: ${Math.max(0, goal.price - p.savings)}</p>
    <div class="btn-row">
      <button data-action="toSavings" ${p.balance <= 0 ? 'disabled' : ''}>Положить 10 🪙</button>
      <button class="danger" data-action="fromSavings" ${p.savings <= 0 ? 'disabled' : ''}>Снять 10 🪙</button>
    </div>
    <p style="font-size:0.85rem;color:var(--muted)">Снятие — только с подтверждением: ты увидишь, как уменьшится сумма и отдалится цель.</p>`
    : '<p>Выбери цель ниже, чтобы начать копить.</p>'}
  </div>
  <div class="card">
    <b>Выбрать цель:</b>
    ${CONTENT.goals.map((g) => `<div class="goal-card ${p.goalId === g.id ? 'selected' : ''}">
      <div style="display:flex;justify-content:space-between"><b>${esc(g.name)}</b><b>${g.price} 🪙</b></div>
      <p style="font-size:0.9rem;color:var(--muted)">${esc(g.description)}</p>
      ${p.goalId === g.id ? '<span class="ok">✓ Текущая цель</span>' : `<button data-goal="${g.id}" style="width:100%">Выбрать эту цель</button>`}
    </div>`).join('')}
  </div>`;
}

function viewTasks() {
  return `${topbar('Задания', 'main')}
  <div class="card">
    ${CONTENT.tasks.map((t) => {
      const pr = state.tasksProgress[t.id];
      return `<div class="shop-item ${pr?.completed ? 'task-done' : ''}">
        <div class="info">
          <div class="name">${pr?.completed ? '✅ ' : ''}${esc(t.title)}</div>
          <div class="hint">${TOPICS[t.topic]} · награда ${t.reward} 🪙</div>
        </div>
        ${pr?.completed ? '<span class="ok">готово</span>' : `<button data-task="${t.id}">Играть</button>`}
      </div>`;
    }).join('')}
  </div>
  <p class="bottom-note">В демонстрационном режиме все задания доступны сразу, без привязки ко времени.</p>`;
}

function viewTask() {
  const rt = taskRuntime;
  const t = CONTENT.tasks.find((x) => x.id === rt.taskId);
  const step = t.scenario[rt.step];
  const answered = rt.picked !== null;
  const pickedOpt = answered ? step.options.find((o) => o.id === rt.picked) : null;
  const last = rt.step === t.scenario.length - 1;
  return `${topbar(t.title, 'tasks')}
  <div class="card">
    <p class="task-story">${esc(t.story)}</p>
    <h2>Шаг ${rt.step + 1} из ${t.scenario.length}</h2>
    <p><b>${esc(step.text)}</b></p>
    ${step.options.map((o) => {
      let cls = 'option';
      if (answered) {
        if (o.id === rt.picked) cls += o.correct ? ' correct' : ' wrong';
        else if (o.correct) cls += ' correct';
      }
      return `<button class="${cls}" data-option="${o.id}" ${answered ? 'disabled' : ''}>${esc(o.text)}</button>`;
    }).join('')}
    ${answered ? `<div class="explain ${pickedOpt.correct ? '' : 'soft'}">${esc(pickedOpt.explanation)}</div>` : ''}
    ${answered ? `<button data-action="taskNext" style="width:100%">${last ? 'Забрать награду +' + t.reward + ' 🪙' : 'Дальше'}</button>` : ''}
  </div>`;
}

function viewPeriodEnd() {
  const c = state.closedPeriods[state.closedPeriods.length - 1];
  return `${topbar('Итоги периода', 'main')}
  <div class="card" style="text-align:center">${petEmojiHtml()}
    <div class="pet-name">${esc(state.profile.petName)}</div>
    <div class="pet-msg">${esc(window.__periodMsg || '')}</div>
  </div>
  <div class="card">
    <b>Период ${c.index}: план vs факт</b>
    ${pvfTable({ m: c.m, o: c.o, s: c.s }, { m: c.fm, o: c.fo, s: c.fs })}
    <ul class="footnote-list">
      <li>${c.mandatoryCovered ? '<span class="ok">✓</span> Обязательные расходы закрыты' : '<span class="bad">✗</span> Обязательные расходы не закрыты — исправь в новом периоде'}</li>
      <li>${c.savingsMet ? '<span class="ok">✓</span> Накопления по плану или больше' : '<span class="bad">✗</span> Накоплено меньше плана'}</li>
      <li>Балл периода: <b>${c.score.toFixed(2)}</b> · Средний балл: <b>${(state.closedPeriods.reduce((a, x) => a + x.score, 0) / state.closedPeriods.length).toFixed(2)}</b></li>
    </ul>
    <button data-action="nextPeriod" style="width:100%">Начать период ${state.periodIndex}</button>
  </div>`;
}

function viewHistory() {
  const p = state.profile;
  const done = Object.values(state.tasksProgress).filter((x) => x.completed).length;
  const goal = CONTENT.goals.find((g) => g.id === p.goalId);
  return `${topbar('Прогресс', 'main')}
  <div class="card">
    <b>Ребёнок:</b> ${esc(p.childName)} · <b>Питомец:</b> ${esc(p.petName)} (${STAGES[p.stage].name})
    <ul class="footnote-list">
      <li>Периодов завершено: <b>${state.closedPeriods.length}</b></li>
      <li>Заданий выполнено: <b>${done} из ${CONTENT.tasks.length}</b></li>
      <li>Цель: ${goal ? esc(goal.name) + ' — накоплено ' + p.savings + ' из ' + goal.price : 'не выбрана'}</li>
    </ul>
  </div>
  <div class="card">
    <b>История начислений и трат:</b>
    ${state.tx.length ? '<ul class="footnote-list">' + state.tx.slice(0, 15).map((x) =>
      `<li>${x.amount > 0 && x.type.startsWith('INCOME') ? '+' : ''}${x.amount > 0 && x.type.startsWith('SAVINGS_IN') ? '' : ''}${x.amount} 🪙 — ${esc(x.title)}</li>`
    ).join('') + '</ul>' : '<p style="color:var(--muted)">Пока пусто.</p>'}
  </div>
  <div class="card">
    <b>Справочник: что значат слова</b>
    ${CONTENT.glossary.map((g) => `<p style="font-size:0.92rem"><b>${esc(g.term)}</b> — ${esc(g.explanation)}</p>`).join('')}
  </div>`;
}

function viewAdultGate() {
  return `${topbar('Раздел для взрослого', 'main')}
  <div class="card" style="text-align:center">
    <p style="font-size:3rem">🔒</p>
    <p>Этот раздел для родителя. <b>Удерживай кнопку 3 секунды</b>, чтобы войти.</p>
    <button class="hold-btn" id="holdBtn" style="width:100%"><span class="fill" id="holdFill"></span>УДЕРЖИВАЙ, ЧТОБЫ ВОЙТИ</button>
  </div>`;
}

function viewAdultHome() {
  const done = Object.entries(state.tasksProgress).filter(([, v]) => v.completed);
  const topics = [...new Set(CONTENT.tasks.filter((t) => state.tasksProgress[t.id]?.completed).map((t) => t.topic))];
  return `${topbar('Раздел для взрослого', 'main')}
  <div class="card">
    <b>Цель приложения:</b> сформировать у ребёнка 7–11 лет базовые навыки управления деньгами:
    планировать бюджет, различать обязательные и необязательные расходы, копить на цель.
    Приложение не использует реальные деньги, рекламу и не собирает персональные данные.
  </div>
  <div class="card">
    <b>Пройденные темы:</b> ${topics.length ? topics.map((t) => TOPICS[t]).join(', ') : 'пока нет'}
    <br><b>Заданий выполнено:</b> ${done.length} из ${CONTENT.tasks.length}
    <br><b>Периодов завершено:</b> ${state.closedPeriods.length}
    <p style="color:var(--muted);font-size:0.9rem">Оценки ребёнку не ставятся — здесь только общий прогресс.</p>
  </div>
  <div class="card">
    <b>Демонстрационный режим</b> (для экспертной проверки):
    ежедневный доход по кнопке, все задания открыты сразу.
    <div class="btn-row" style="margin-top:10px">
      <button class="${state.demo ? '' : 'secondary'}" data-action="toggleDemo">${state.demo ? 'Включён — отключить' : 'Включить'}</button>
    </div>
  </div>
  <div class="card">
    <b>Управление профилем</b>
    <div class="btn-row" style="margin-top:10px">
      <button class="danger" data-action="resetProfile">Сбросить тестовый профиль</button>
    </div>
    <p style="color:var(--muted);font-size:0.85rem">Сброс вернёт игру к исходному состоянию: начальный баланс, первая стадия, чистая история. Действие требует подтверждения.</p>
  </div>`;
}

function viewHelp() {
  return `${topbar('Как играть', 'main')}
  <div class="card">
    <p>🍖 <b>Обязательное</b> — еда и уход. Купи в первую очередь.</p>
    <p>🎾 <b>Желаемое</b> — радует питомца, но может подождать.</p>
    <p>🏦 <b>Отложить</b> — пополняй накопления, чтобы достичь цели.</p>
    <p>📋 Сначала составь <b>план бюджета</b>, потом трать. В конце периода сравни план с фактом.</p>
    <p>📈 Состояние и рост питомца зависят от твоих решений. Ошибку всегда можно исправить в следующем периоде!</p>
  </div>`;
}

/* ---------- события ---------- */
function bind() {
  document.querySelectorAll('[data-go]').forEach((b) =>
    b.addEventListener('click', () => { screen = b.dataset.go; render(); }));
  document.querySelectorAll('[data-action]').forEach((b) =>
    b.addEventListener('click', () => onAction(b.dataset.action)));
  document.querySelectorAll('[data-pick]').forEach((b) =>
    b.addEventListener('click', () => {
      const [key, val] = b.dataset.pick.split(':');
      state.profileDraft[key] = val;
      save(); render();
      const inp = document.getElementById('childName');
      if (inp) { inp.focus(); inp.setSelectionRange(inp.value.length, inp.value.length); }
    }));
  document.querySelectorAll('[data-slider]').forEach((s) =>
    s.addEventListener('input', () => {
      state.draftPlan[s.dataset.slider] = parseInt(s.value, 10);
      document.getElementById('val-' + s.dataset.slider).textContent = s.value + ' 🪙';
      const p = state.profile;
      const d = state.draftPlan;
      const sum = d.m + d.o + d.s;
      const rem = document.querySelector('.remainder');
      if (rem) {
        rem.className = 'remainder' + (sum > p.balance ? ' over' : '');
        rem.innerHTML = `Итого плана: ${sum} из ${p.balance} · Остаток: ${p.balance - sum}` +
          (sum > p.balance ? '<br>⚠️ Сумма превышает бюджет — уменьши одно из направлений.' : '');
        document.querySelector('[data-action="confirmPlan"]').disabled = sum > p.balance;
      }
    }));
  document.querySelectorAll('[data-buy]').forEach((b) =>
    b.addEventListener('click', () => {
      const item = CONTENT.shop.find((x) => x.id === b.dataset.buy);
      if (buyItem(item)) {
        toast(`Куплено: ${item.name} (−${item.price} 🪙)`);
        render();
      }
    }));
  document.querySelectorAll('[data-goal]').forEach((b) =>
    b.addEventListener('click', () => {
      state.profile.goalId = b.dataset.goal;
      save(); render();
      toast('Цель выбрана! Пополняй накопления понемногу.');
    }));
  document.querySelectorAll('[data-task]').forEach((b) =>
    b.addEventListener('click', () => {
      taskRuntime = { taskId: b.dataset.task, step: 0, picked: null, correctCount: 0 };
      screen = 'task'; render();
    }));
  document.querySelectorAll('[data-option]').forEach((b) =>
    b.addEventListener('click', () => {
      const rt = taskRuntime;
      const t = CONTENT.tasks.find((x) => x.id === rt.taskId);
      const step = t.scenario[rt.step];
      const opt = step.options.find((o) => o.id === b.dataset.option);
      rt.picked = opt.id;
      if (opt.correct) rt.correctCount++;
      state.profile.mood = clamp(state.profile.mood + (opt.effect?.mood || 0), 0, 100);
      state.profile.satiety = clamp(state.profile.satiety + (opt.effect?.satiety || 0), 0, 100);
      save(); render();
    }));

  // имя профиля: сохранять черновик при вводе
  ['childName', 'petName'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', () => {
      state.profileDraft[id] = el.value;
      save();
    });
  });

  // барьер для взрослого: удержание 3 секунды
  const hold = document.getElementById('holdBtn');
  if (hold) {
    let timer = null, raf = null, start = 0;
    const fill = document.getElementById('holdFill');
    const tick = () => {
      const pct = Math.min(100, ((Date.now() - start) / 3000) * 100);
      fill.style.width = pct + '%';
      if (pct >= 100) { cleanup(); screen = 'adultHome'; render(); return; }
      raf = requestAnimationFrame(tick);
    };
    const cleanup = () => { clearTimeout(timer); cancelAnimationFrame(raf); fill.style.width = '0%'; };
    hold.addEventListener('pointerdown', () => { start = Date.now(); timer = setTimeout(cleanup, 3100); raf = requestAnimationFrame(tick); });
    hold.addEventListener('pointerup', cleanup);
    hold.addEventListener('pointerleave', cleanup);
  }
}

function onAction(action) {
  switch (action) {
    case 'finishOnboarding':
      state.onboarded = true; save(); render(); break;
    case 'createProfile': {
      const d = state.profileDraft || {};
      if (!d.petName || !d.petName.trim()) { toast('Дай питомцу имя!'); return; }
      state.profile = {
        childName: (d.childName || 'Игрок').trim(),
        petName: d.petName.trim(),
        body: d.body, color: d.color, accessory: d.accessory,
        balance: START_BALANCE, savings: 0, goalId: null,
        mood: 70, satiety: 70, stage: 0,
      };
      addTx('INCOME_START', START_BALANCE, 'Стартовый бюджет');
      state.profileDraft = null;
      save(); screen = 'main'; render();
      toast(`Знакомься — ${state.profile.petName}! Стартовый бюджет: ${START_BALANCE} 🪙`);
      break;
    }
    case 'claimDaily': claimDaily(); render(); break;
    case 'confirmPlan':
      state.plan = { ...state.draftPlan };
      save(); render();
      toast('План подтверждён! Теперь сравним его с фактом в конце периода.');
      break;
    case 'toSavings': toSavings(10); render(); toast('Положено 10 🪙 в накопления!'); break;
    case 'fromSavings': fromSavings(10); render(); break;
    case 'taskNext': {
      const rt = taskRuntime;
      const t = CONTENT.tasks.find((x) => x.id === rt.taskId);
      if (rt.step < t.scenario.length - 1) {
        rt.step++; rt.picked = null; render();
      } else {
        const allCorrect = rt.correctCount === t.scenario.length;
        const reward = t.reward + (allCorrect ? 2 : 0);
        state.profile.balance += reward;
        addTx('INCOME_TASK', reward, 'Задание: ' + t.title);
        state.tasksProgress[t.id] = { completed: true, attempts: (state.tasksProgress[t.id]?.attempts || 0) + 1, lastCorrect: allCorrect };
        taskRuntime = null;
        save(); screen = 'tasks'; render();
        toast(`Задание завершено! Награда +${reward} 🪙${allCorrect ? ' (всё верно — бонус +2!)' : ''}`);
      }
      break;
    }
    case 'closePeriod': {
      const msg = closePeriod();
      window.__periodMsg = msg;
      screen = 'periodEnd'; render();
      break;
    }
    case 'nextPeriod': screen = 'main'; render(); break;
    case 'toggleDemo':
      state.demo = !state.demo; save(); render();
      toast(state.demo ? 'Демонстрационный режим включён.' : 'Демонстрационный режим выключен.');
      break;
    case 'resetProfile':
      if (confirm('Сбросить тестовый профиль к исходному состоянию? Прогресс будет удалён.')) {
        const demo = state.demo;
        localStorage.removeItem(LS_KEY);
        state = defaultState();
        state.demo = demo;
        taskRuntime = null; screen = 'main';
        save(); render();
        toast('Профиль сброшен. Можно пройти сценарий заново.');
      }
      break;
  }
}

load();
