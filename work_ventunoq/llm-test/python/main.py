import time
from arduino.app_bricks.llm import LargeLanguageModel
from arduino.app_utils import App

llm = LargeLanguageModel(system_prompt="You are a concise assistant for a vacuum physics laboratory.")
print("LLM attrs:", [a for a in dir(llm) if not a.startswith("_")], flush=True)

def run_once():
    for prompt in ["Reply with exactly: READY",
                   "A vacuum gauge reads 3.2e-7 mbar and a turbo pump runs at full speed. In three sentences, say whether it is reasonable to switch on an RGA filament and why."]:
        t = time.time()
        out = llm.chat(prompt)
        dt = time.time() - t
        print("PROMPT:", prompt, flush=True)
        print("ANSWER:", out, flush=True)
        print("TIME: %.1f s, %d chars, %d words" % (dt, len(out), len(out.split())), flush=True)
    raise StopIteration

App.run(run_once)
