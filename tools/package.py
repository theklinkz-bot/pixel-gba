"""Package the tested APK and complete rebuildable source; exclude local toolchains."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root=Path(__file__).resolve().parents[1]
dist=root/'dist'
dist.mkdir(exist_ok=True)
shutil.copy2(root/'app/build/outputs/apk/debug/app-debug.apk',dist/'PixelGBA-1.1.apk')
for name in ['README.md','THIRD-PARTY-NOTICES.md','LICENSE']:
    shutil.copy2(root/name,dist/name)
excluded={'.git','.tools','.gradle','.cxx','build','dist','__pycache__'}
with zipfile.ZipFile(dist/'PixelGBA-source.zip','w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(root.rglob('*')):
        rel=p.relative_to(root)
        if p.is_file() and not any(part in excluded for part in rel.parts) and p.name!='local.properties':
            z.write(p,rel.as_posix())
for p in [dist/'PixelGBA-1.1.apk',dist/'PixelGBA-source.zip']:
    digest=hashlib.sha256(p.read_bytes()).hexdigest()
    (dist/(p.name+'.sha256')).write_text(digest+'  '+p.name+'\n')
    print(p.name,p.stat().st_size,'bytes',digest)
