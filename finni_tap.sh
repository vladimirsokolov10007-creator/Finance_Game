#!/bin/bash
# Helper: tap on UI element by its text via fresh uiautomator dump
# usage: finni_tap.sh "text"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
DEV="emulator-5554"
TEXT="$1"

"$ADB" -s $DEV shell 'rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml' >/dev/null 2>&1
sleep 1
COORDS=$("$ADB" -s $DEV shell 'cat /sdcard/ui.xml' | python -c "
import sys, re, html
xml = sys.stdin.read()
want = '''$TEXT'''
for m in re.finditer(r'<node[^>]*text=\"([^\"]*)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    txt = html.unescape(m.group(1)).strip()
    if txt == want:
        x = (int(m.group(2)) + int(m.group(4))) // 2
        y = (int(m.group(3)) + int(m.group(5))) // 2
        print(f'{x} {y}')
        break
")
if [ -z "$COORDS" ]; then
  echo "NOT_FOUND: $TEXT"
  exit 1
fi
X=${COORDS% *}; Y=${COORDS#* }
"$ADB" -s $DEV shell input swipe "$X" "$Y" "$((X+1))" "$((Y+1))" 300
echo "TAPPED: $TEXT at $X,$Y"
