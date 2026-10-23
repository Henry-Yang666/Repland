"""Build auditable, per-case results and feature references from this audit run."""
import hashlib
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/verification/user-audit-2026-10-11'

def write(name, value):
    (OUT/name).write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

manifest = json.loads((ROOT/'docs/verification/final-0.1.35/source-manifest.json').read_text(encoding='utf-8'))
checked = []
for f in manifest['files']:
    data = (ROOT/f['path']).read_bytes()
    digest = hashlib.sha256(data.replace(b'\r\n', b'\n')).hexdigest()
    checked.append({'path':f['path'], 'group':f['group'], 'matchesAcceptedSourceLf':digest==f['sha256Lf']})
write('source-check.json', {'checked':len(checked), 'mismatches':[x for x in checked if not x['matchesAcceptedSourceLf']], 'files':checked})

log = (OUT/'device-instrumentation.raw.log').read_text(encoding='utf-8-sig', errors='replace')
status = {}
cases = []
for line in log.splitlines():
    m = re.match(r'INSTRUMENTATION_STATUS: ([^=]+)=(.*)', line)
    if m: status[m[1]] = m[2]
    m = re.match(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)', line)
    if m:
        code = int(m[1])
        if code != 1 and 'class' in status and 'test' in status:
            cases.append({'class':status['class'], 'test':status['test'], 'code':code,
                          'result':{0:'passed',-1:'error',-2:'failed',-3:'ignored',-4:'assumption'}.get(code,'unknown')})
        status = {}
write('device-tests.json', {'runner':'adb shell am instrument -w -r', 'package':'com.swan1127.repland.qa',
      'summary':dict(Counter(x['result'] for x in cases)), 'runFinished':'INSTRUMENTATION_CODE: -1' in log,
      'reportedOk':re.findall(r'OK \(\d+ tests?\)', log), 'cases':cases})
units = []
for file in (ROOT/'android/app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    doc = ET.parse(file).getroot()
    for c in doc.findall('testcase'):
        units.append({'class':c.get('classname'),'test':c.get('name'),'seconds':c.get('time'),
                      'result':'failed' if c.find('failure') is not None else 'error' if c.find('error') is not None else 'skipped' if c.find('skipped') is not None else 'passed'})
write('unit-tests.json', {'summary':dict(Counter(x['result'] for x in units)), 'cases':units})

features = []
for f in sorted((ROOT/'docs/features').glob('*.md')):
    if not re.fullmatch(r'[NTKPESH]\d{2}', f.stem): continue
    content = f.read_text(encoding='utf-8')
    refs = sorted(set(re.findall(r'\b([A-Z][A-Za-z0-9]*(?:Test|UiTest))\b', content)))
    relevant = [c for c in cases+units if c['class'].split('.')[-1] in refs]
    features.append({'id':f.stem,'title':content.splitlines()[0].removeprefix('# '),
                     'documentation':'../../features/'+f.name, 'referencedTestClasses':refs,
                     'relatedResults':dict(Counter(x['result'] for x in relevant)),
                     'scopeNote':'关联类结果仅证明其实际断言，不证明该功能全部分支或系统外部服务。'})
write('feature-test-map.json', {'featureCount':len(features), 'features':features})
print(json.dumps({'sourceMismatches':[x['path'] for x in checked if not x['matchesAcceptedSourceLf']],
                  'device':dict(Counter(x['result'] for x in cases)), 'unit':dict(Counter(x['result'] for x in units)),
                  'featureCount':len(features)}, ensure_ascii=False))
