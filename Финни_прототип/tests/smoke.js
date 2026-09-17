// Дымовой тест игровой экономики «Питомец Финни».
// Запуск из папки проекта: node tests/smoke.js
// Проверяет: создание профиля, контроль плана, покупки, накопления,
// ежедневный доход, закрытие периода, стадии питомца, сброс профиля.
const fs = require('fs');
const path = require('path');

/* --- заглушки окружения браузера --- */
const store = {};
global.localStorage = {
  getItem: (k) => (k in store ? store[k] : null),
  setItem: (k, v) => { store[k] = String(v); },
  removeItem: (k) => { delete store[k]; },
};
global.window = {};
global.confirm = () => true;
const fakeEl = () => ({
  innerHTML: '', textContent: '', className: '', style: {},
  addEventListener: () => {}, appendChild: () => {}, remove: () => {},
  setSelectionRange: () => {}, focus: () => {}, disabled: false,
});
global.document = {
  getElementById: () => fakeEl(),
  querySelectorAll: () => [],
  querySelector: () => null,
  createElement: fakeEl,
  body: { appendChild: () => {} },
};
global.fetch = (url) => {
  const data = fs.readFileSync(path.join(__dirname, '..', url), 'utf8');
  return Promise.resolve({ json: () => JSON.parse(data) });
};

let passed = 0, failed = 0;
function assert(cond, name) {
  if (cond) { passed++; console.log('  ✓', name); }
  else { failed++; console.error('  ✗ ПРОВАЛ:', name); }
}

(async () => {
  const src = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
  const exportsHook = `
;globalThis.__t = {
  get state() { return state; }, set state(v) { state = v; },
  CONTENT: () => CONTENT,
  defaultState, canClosePlan, buyItem, toSavings, fromSavings,
  claimDaily, closePeriod, save, onAction,
};`;
  eval(src + exportsHook);
  await globalThis.__loadPromise; // если load ещё идёт
  const T = globalThis.__t;

  // дождаться завершения load() из app.js
  for (let i = 0; i < 50 && !T.CONTENT().tasks.length; i++) {
    await new Promise((r) => setTimeout(r, 20));
  }
  const tasks = T.CONTENT().tasks, shop = T.CONTENT().shop, goals = T.CONTENT().goals;
  assert(tasks.length === 6, 'загружено 6 заданий');
  assert(shop.length === 8, 'загружено 8 товаров');
  assert(goals.length === 3, 'загружено 3 цели');

  // 1. создание профиля
  T.state.profileDraft = { childName: 'Соня', petName: 'Финни', body: 'cat', color: 'sunny', accessory: 'bow' };
  T.onAction('createProfile');
  assert(T.state.profile.balance === 40, 'стартовый баланс 40');
  assert(T.state.profile.stage === 0, 'стадия 0 на старте');

  // 2. контроль плана: сумма больше баланса запрещена
  assert(!T.canClosePlan({ m: 30, o: 20, s: 0 }), 'план сверх баланса отклонён');
  assert(T.canClosePlan({ m: 15, o: 15, s: 10 }), 'план в пределах баланса принят');

  // 3. покупки
  const food = shop.find((s) => s.id === 'food_1');
  const ball = shop.find((s) => s.id === 'toy_ball');
  assert(T.buyItem(food) === true, 'покупка корма прошла');
  assert(T.state.fact.m === 10 && T.state.profile.balance === 30, 'факт и баланс учтены');
  assert(T.state.mandatoryBought === true, 'флаг обязательной покупки');
  T.state.profile.balance = 5;
  assert(T.buyItem(ball) === false, 'покупка при нехватке отклонена');
  assert(T.state.profile.balance === 5 && T.state.fact.o === 0, 'баланс не изменился при отказе');
  T.state.profile.balance = 40;

  // 4. накопления
  T.toSavings(10);
  assert(T.state.profile.savings === 10 && T.state.fact.s === 10, 'пополнение накоплений');
  T.fromSavings(4);
  assert(T.state.profile.savings === 6 && T.state.profile.balance === 34, 'снятие с накоплений');

  // 5. демо-режим и ежедневный доход
  T.state.demo = true;
  const before = T.state.profile.balance;
  T.claimDaily();
  assert(T.state.profile.balance === before + 20, 'ежедневный доход в демо-режиме');
  T.claimDaily();
  assert(T.state.profile.balance === before + 40, 'повторный доход в демо разрешён');

  // 6. закрытие периода и новый цикл
  T.state.plan = { m: 10, o: 0, s: 10 };
  T.closePeriod();
  assert(T.state.closedPeriods.length === 1, 'период записан в историю');
  assert(T.state.periodIndex === 2, 'номер периода увеличился');
  assert(T.state.fact.m === 0 && T.state.fact.s === 0, 'факт обнулён для нового периода');
  assert(T.state.plan === null, 'план сброшен');
  const rec = T.state.closedPeriods[0];
  assert(rec.mandatoryCovered === true, 'обязательные закрыты в записи');
  assert(rec.score > 0 && rec.score <= 1, 'балл периода в диапазоне 0–1');

  // 7. серия периодов → стадия растёт и не падает
  T.state.plan = { m: 10, o: 0, s: 0 };
  for (let i = 0; i < 5; i++) {
    T.state.fact = { m: 10, o: 0, s: 5 };
    T.state.mandatoryBought = true;
    T.closePeriod();
    T.state.plan = { m: 10, o: 0, s: 0 };
  }
  assert(T.state.profile.stage >= 1, 'после серии хороших периодов стадия выросла');

  // 8. сброс тестового профиля
  T.state.demo = true;
  T.onAction('resetProfile');
  assert(T.state.profile === null && T.state.closedPeriods.length === 0, 'профиль сброшен');
  assert(T.state.demo === true, 'демо-режим сохранился после сброса');

  console.log(`\nИтог: ${passed} прошло, ${failed} провалено`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('Ошибка теста:', e); process.exit(1); });
