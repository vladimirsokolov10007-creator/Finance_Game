#!/bin/bash
# Helper: fresh UI dump -> stdout texts
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
DEV="emulator-5554"
"$ADB" -s $DEV shell 'rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml' >/dev/null 2>&1
sleep 1
"$ADB" -s $DEV shell 'cat /sdcard/ui.xml' | python -c "
import sys, re, html
xml = sys.stdin.read()
out = []
for m in re.finditer(r'<node[^>]*text=\"([^\"]*)\"', xml):
    t = html.unescape(m.group(1)).strip()
    if t:
        out.append(t)
print('\n'.join(out))
"
