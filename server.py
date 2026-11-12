"""
server.py - Phone ki photo leta hai, AI se ghar ka 3D shape banata hai, 360 viewer dikhata hai.
Setup: pip install flask pillow rembg onnxruntime trimesh
       TripoSR clone karo aur TRIPOSR_DIR me uska path likho.
Chalao: python server.py   (PC aur phone ek hi WiFi par ho)
"""
import subprocess, sys
from pathlib import Path
from flask import Flask, request, send_from_directory
from PIL import Image
from rembg import remove
import trimesh

TRIPOSR_DIR = Path("TripoSR").resolve()   # <-- apna path
WORK = Path("work").resolve()
WORK.mkdir(exist_ok=True)
app = Flask(__name__)


@app.post("/upload")
def upload():
    f = request.files["image"]
    size_ft = float(request.form.get("size_ft", 0) or 0)
    src = WORK / "input.jpg"
    f.save(src)
    clean = WORK / "clean.png"
    remove(Image.open(src).convert("RGB")).save(clean)          # background hatao
    r = subprocess.run([sys.executable, "run.py", str(clean), "--output-dir", str(WORK),
                        "--no-remove-bg", "--mc-resolution", "320"], cwd=TRIPOSR_DIR)
    if r.returncode != 0:
        return "TripoSR fail", 500
    mesh = trimesh.load(str(WORK / "0" / "mesh.obj"), force="mesh")
    if size_ft:
        mesh.apply_scale(size_ft / max(mesh.extents))            # asli size
    mesh.export(str(WORK / "ghar.glb"))
    return "ok"


@app.get("/model.glb")
def model():
    return send_from_directory(WORK, "ghar.glb")


@app.get("/view")
def view():   # 360 viewer page (ungli se ghumao)
    return """<!doctype html><meta name=viewport content="width=device-width,initial-scale=1">
<script type=module src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.5.0/model-viewer.min.js"></script>
<body style="margin:0"><model-viewer src="/model.glb" camera-controls auto-rotate
 style="width:100vw;height:100vh;background:#cfe3f5"></model-viewer>"""


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
  
