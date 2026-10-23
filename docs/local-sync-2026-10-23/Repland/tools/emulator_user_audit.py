"""Read public UI and drive the single, explicitly selected audit emulator.

Never reads application private storage or provider credentials. Screenshots and
UI text should only be recorded on non-sensitive screens.
"""
import argparse
import json
import re
import subprocess
import time
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')

ROOT = Path(__file__).resolve().parents[1]
ADB = ROOT.parent / 'Environment/Android/sdk/platform-tools/adb.exe'
OUT = ROOT / 'docs/verification/user-audit-2026-10-11'

def adb(*args):
    p = subprocess.run([str(ADB), '-s', 'emulator-5556', *args], capture_output=True, timeout=30)
    if p.returncode:
        raise RuntimeError(p.stderr.decode('utf-8', 'replace'))
    return p.stdout

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/audit-window.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/audit-window.xml'))

def visible(root):
    return [{k:n.get(k, '') for k in ('text', 'content-desc', 'bounds', 'class', 'clickable', 'enabled')}
            for n in root.iter('node') if n.get('text') or n.get('content-desc')]

def click(value, desc=False, contains=False):
    root = tree()
    attr = 'content-desc' if desc else 'text'
    found = [n for n in root.iter('node') if value in n.get(attr, '')] if contains else [n for n in root.iter('node') if n.get(attr)==value]
    if len(found) != 1:
        raise RuntimeError(f'Expected one {attr}={value!r}, got {len(found)}')
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', found[0].get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.7)

def capture(name):
    OUT.mkdir(parents=True, exist_ok=True)
    root = tree()
    (OUT / f'{name}.ui.json').write_text(json.dumps(visible(root), ensure_ascii=False, indent=2), encoding='utf-8')
    (OUT / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    print(json.dumps({'capture':name, 'text':[n.get('text') or n.get('content-desc') for n in root.iter('node') if n.get('text') or n.get('content-desc')]}, ensure_ascii=False))

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('action', choices=['show', 'capture', 'click', 'desc', 'contains', 'back', 'up', 'down', 'text', 'replace', 'tap'])
    p.add_argument('value', nargs='?')
    a = p.parse_args()
    if a.action == 'show': print(json.dumps(visible(tree()), ensure_ascii=False))
    elif a.action == 'capture': capture(a.value)
    elif a.action in ('click', 'desc', 'contains'): click(a.value, a.action=='desc', a.action=='contains')
    elif a.action == 'back': adb('shell', 'input', 'keyevent', '4')
    elif a.action in ('up', 'down'):
        coords = ('540','1900','540','700') if a.action=='up' else ('540','700','540','1900')
        adb('shell','input','swipe', *coords, '400')
    elif a.action == 'text': adb('shell', 'input', 'text', a.value)
    elif a.action == 'replace':
        fields = [n for n in tree().iter('node') if n.get('class')=='android.widget.EditText']
        if len(fields)!=1: raise RuntimeError('Replacement requires exactly one public text field')
        n=fields[0]
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
        adb('shell','input','keyevent','123')
        if n.get('text'): adb('shell','input','keyevent',*(['67']*len(n.get('text'))))
        if a.value: adb('shell','input','text',a.value)
    elif a.action == 'tap': adb('shell', 'input', 'tap', *a.value.split(','))
