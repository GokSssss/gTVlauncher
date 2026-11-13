from pathlib import Path
import subprocess,os
root=Path(__file__).resolve().parent.parent;os.chdir(root);out=root/'tests/build/bridge';out.mkdir(parents=True,exist_ok=True)
subprocess.run(['java','-jar',str(root/'.cache/ecj.jar'),'-8','-nowarn','-encoding','UTF-8','-d',str(out),*[str(p) for p in (root/'bridge/src/main/java').rglob('*.java')],'tests/BridgeCheck.java','tests/TextTransferCheck.java','tests/RepeatTransferCheck.java'],check=True)
subprocess.run(['java','-cp',str(out),'ru.truetv.bridge.BridgeCheck'],check=True,timeout=80)

subprocess.run(["java","-cp",str(out),"ru.truetv.bridge.TextTransferCheck"],check=True,timeout=80)

subprocess.run(["java","-cp",str(out),"ru.truetv.bridge.RepeatTransferCheck"],check=True,timeout=80)
