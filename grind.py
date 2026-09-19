# -*- coding: utf-8 -*-
"""Автоматизация смоук-теста v0.5: гринд до победы."""
import subprocess, time, re, html, sys

ADB = None  # set in main
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

def close_week():
    """С главного: завершить неделю, затем начать следующую."""
    ts = texts()
    if "🏁 Завершить неделю" not in ts:
        print("close_week: не на главном!", ts[:5]); return False
    tap_text("🏁 Завершить неделю"); wait(2.5)
    ts = texts()
    start = [t for t in ts if t.startswith("Начать неделю")]
    if start:
        tap_text(start[0]); wait(2.5)
        return True
    if any("Игра окончена" in t for t in ts):
        print("GAME OVER!"); return "GAMEOVER"
    print("close_week: неожиданный экран", ts[:6]); return False

def go_home():
    ts = texts()
    if "🏠 Главный экран" in ts:
        tap_text("🏠 Главный экран"); wait(2)

def buy_improvement(name):
    """С главного: купить улучшение по имени."""
    if not tap_text("🛒 Покупки"): return False
    wait(2)
    if not tap_text("🔧 Улучшения"): return False
    wait(2)
    nodes = dump_nodes()
    idx = next((i for i, (t, _, _) in enumerate(nodes) if t == name), None)
    if idx is None:
        print("buy: предмет не найден:", name); return False
    btn = next(((x, y) for t, x, y in nodes[idx+1:idx+5] if t == "Купить"), None)
    if not btn:
        print("buy: кнопка Купить не найдена у", name); return False
    tap(*btn); wait(2)
    if not tap_text("Купить"):  # подтверждение в диалоге
        print("buy: нет диалога подтверждения"); return False
    wait(2.5)
    go_home()
    return True

def balance():
    for t in texts():
        m = re.match(r"Баланс: (\d+) 🪙", t)
        if m: return int(m.group(1))
    return None

def main():
    global ADB
    import os
    ADB = os.path.expandvars(r"%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe")
    # последовательность: (действие, имя/None)
    plan = [
        ("close", None),                 # конец недели 1 (штраф, +20)
        ("buy", "Лежанка «Облако»"),
        ("close", None),
        ("buy", "Автокормушка"),
        ("close", None),
        ("close", None),                 # неделя без улучшения (штраф)
        ("buy", "Игровой комплекс"),
        ("close", None),
        ("close", None),                 # неделя без улучшения (штраф)
        ("buy", "Полноценный рацион"),
        ("close", None),
        ("close", None),                 # неделя без улучшения (штраф)
        ("close", None),                 # ещё неделя — копим на домик
        ("buy", "Домик мечты"),          # ПОБЕДА
    ]
    for action, name in plan:
        go_home(); wait(1)
        ts = texts()
        if any("Игра окончена" in t for t in ts):
            print("ИГРА ОКОНЧЕНА до победы"); return
        if action == "close":
            print("== закрываю неделю, баланс:", balance())
            r = close_week()
            if r == "GAMEOVER": return
            wait(1)
        else:
            print("== покупаю:", name, "баланс:", balance())
            if not buy_improvement(name):
                print("ПОКУПКА НЕ УДАЛАСЬ"); return
            wait(1)
    ts = texts()
    print("ФИНАЛЬНЫЙ ЭКРАН:")
    for t in ts[:12]:
        print("  ", t)

if __name__ == "__main__":
    main()
