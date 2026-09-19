# -*- coding: utf-8 -*-
"""Смоук v0.5: полное прохождение — онбординг, создание питомца, 5 недель, победа."""
import subprocess, time, re, html, os

ADB = os.path.expandvars(r"%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe")
DEV = "emulator-5554"

def sh(*args):
    return subprocess.run(args, capture_output=True, text=True, encoding='utf-8', errors='replace').stdout

def adb(*args):
    return sh(ADB, "-s", DEV, *args)

def dump_nodes():
    adb("shell", "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml")
    time.sleep(1.2)
    xml = adb("shell", "cat /sdcard/ui.xml")
    nodes = []
    for m in re.finditer(r'<node[^>]*text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        nodes.append((html.unescape(m.group(1)).strip(),
                      (int(m.group(2)) + int(m.group(4))) // 2,
                      (int(m.group(3)) + int(m.group(5))) // 2))
    return nodes

def texts():
    return [t for t, _, _ in dump_nodes() if t]

def tap(x, y):
    adb("shell", f"input swipe {x} {y} {x+1} {y+1} 300")

def tap_text(t, occurrence=0):
    nodes = dump_nodes()
    matches = [(x, y) for tt, x, y in nodes if tt == t]
    if len(matches) <= occurrence:
        print(f"NOT FOUND: {t}")
        return False
    x, y = matches[occurrence]
    tap(x, y)
    print(f"tapped: {t} @ {x},{y}")
    return True

def wait(s=2.0):
    time.sleep(s)

def go_home():
    ts = texts()
    if "🏠 Главный экран" in ts:
        tap_text("🏠 Главный экран"); wait(2)

def balance():
    for t in texts():
        m = re.match(r"🪙 (\d+)", t)
        if m: return int(m.group(1))
        m = re.match(r"Баланс: (\d+) 🪙", t)
        if m: return int(m.group(1))
    return None

def confirm_plan():
    """С главного: открыть план и подтвердить (0/0/0 — только чтобы активировать неделю)."""
    if not tap_text("📋 План бюджета"): return False
    wait(2)
    if not tap_text("Подтвердить план"): return False
    wait(2)
    go_home()
    return True

def buy_improvement(name):
    """С главного: купить улучшение по имени."""
    if not tap_text("🛒 Покупки"): return False
    wait(2)
    if not tap_text("🔧 Улучшения"): return False
    wait(2)
    nodes = dump_nodes()
    idx = next((i for i, (t, _, _) in enumerate(nodes) if t == name), None)
    if idx is None:
        # возможно предмет ниже видимой области — скроллим и ищем снова
        for _ in range(3):
            adb("shell", "input swipe 540 1700 540 400 400"); wait(1.5)
            nodes = dump_nodes()
            idx = next((i for i, (t, _, _) in enumerate(nodes) if t == name), None)
            if idx is not None: break
    if idx is None:
        print("buy: предмет не найден:", name); return False
    btn = next(((x, y) for t, x, y in nodes[idx+1:idx+5] if t == "Купить"), None)
    if not btn:
        print("buy: кнопка Купить не найдена у", name); return False
    tap(*btn); wait(2)
    if not tap_text("Купить"):  # подтверждение в диалоге
        print("buy: нет диалога подтверждения"); return False
    wait(2.5)
    ts = texts()
    if any("Победа" in t for t in ts):
        print("!!! ЭКРАН ПОБЕДЫ !!!")
        return "VICTORY"
    go_home()
    return True

def close_week():
    """С главного: завершить неделю, затем начать следующую."""
    ts = texts()
    if "🏁 Завершить неделю" not in ts:
        # контент может быть ниже видимой области — скроллим вверх (к низу списка)
        for _ in range(3):
            adb("shell", "input swipe 540 1800 540 500 400"); wait(1.5)
            ts = texts()
            if "🏁 Завершить неделю" in ts: break
    if "🏁 Завершить неделю" not in ts:
        print("close_week: не на главном!", ts[:5]); return False
    tap_text("🏁 Завершить неделю"); wait(2.5)
    ts = texts()
    if any("Игра окончена" in t for t in ts):
        print("GAME OVER!"); return "GAMEOVER"
    start = [t for t in ts if t.startswith("Начать неделю")]
    if start:
        tap_text(start[0]); wait(2.5)
        return True
    print("close_week: неожиданный экран", ts[:6]); return False

def main():
    IMPROVEMENTS = ["Лежанка «Облако»", "Автокормушка", "Игровой комплекс",
                    "Полноценный рацион", "Домик мечты"]

    # --- онбординг ---
    ts = texts()
    if any("Питомец Финни" in t for t in ts) and any("Понятно" in t for t in ts):
        print("== онбординг")
        tap_text("Понятно, начнём!"); wait(2)

    # --- создание питомца ---
    ts = texts()
    if any("Создай питомца" in t for t in ts):
        print("== создаю питомца")
        if not tap_text("Имя питомца"): return
        wait(1.5)
        # эмулятор любит открывать стилус-панель вместо клавиатуры — закрываем
        ts = texts()
        if any("stylus" in t.lower() for t in ts):
            adb("shell", "input swipe 708 2290 709 2290 300"); wait(2)
            adb("shell", "input swipe 540 745 541 745 300"); wait(1.5)
        adb("shell", "input text Finni"); wait(1.5)
        ts = texts()
        if not any(t == "Finni" for t in ts):
            print("имя не введено, пробую ещё")
            adb("shell", "input swipe 708 2290 709 2290 300"); wait(2)
            adb("shell", "input text Finni"); wait(1.5)
        # закрываем клавиатуру, чтобы кнопка была доступна
        adb("shell", "input keyevent 4"); wait(1.5)
        # скролл вниз до кнопки
        adb("shell", "input swipe 540 1600 540 600 400"); wait(1.5)
        if not tap_text("Готово, знакомиться!"):
            adb("shell", "input swipe 540 1600 540 400 400"); wait(1.5)
            if not tap_text("Готово, знакомиться!"): return
        wait(3)

    # --- 5 недель ---
    for i, name in enumerate(IMPROVEMENTS):
        ts = texts()
        if any("Игра окончена" in t for t in ts):
            print("ИГРА ОКОНЧЕНА до победы"); return
        print(f"== неделя {i+1}, баланс: {balance()}")
        if not confirm_plan():
            print("ПЛАН НЕ ПОДТВЕРДИЛСЯ"); return
        print(f"== покупаю: {name}, баланс: {balance()}")
        r = buy_improvement(name)
        if r == "VICTORY": break
        if not r:
            print("ПОКУПКА НЕ УДАЛАСЬ"); return
        print("== закрываю неделю, баланс:", balance())
        r = close_week()
        if r == "GAMEOVER": return
        wait(1)

    ts = texts()
    print("ФИНАЛЬНЫЙ ЭКРАН:")
    for t in ts[:14]:
        print("  ", t)

if __name__ == "__main__":
    main()
