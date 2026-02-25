import json
import re

def format_float(val):
    if val == 0.0:
        return "0f"
    if val == 1.0:
        return "1f"
    s = f"{val:.6f}"
    if '.' in s:
        s = s.rstrip('0')
    if s.endswith('.'):
        s += '0'
    return s + "f"

with open("c:/Users/wecke/Projects/OreHeightIndicator/ore_distribution_wiki.json", "r") as f:
    data = json.load(f)

ores = data["ores"]
order = ["coal", "copper", "iron", "gold", "redstone", "lapis", "diamond", "emerald"]

# generate the content for ORES array
array_content = ""
for ore_name in order:
    if ore_name not in ores: continue
    ore_data = ores[ore_name]
    min_y = ore_data["min_y"]
    max_y = ore_data["max_y"]
    peak_value = ore_data["peak_value"]
    values = ore_data["values"]
    
    length = max_y - min_y + 1
    scores = [0.0] * length
    for y_str, val in values.items():
        y = int(y_str)
        if min_y <= y <= max_y:
            score = val / peak_value
            scores[y - min_y] = score
    
    array_content += f'        // {ore_name.capitalize()}: Y {min_y} to {max_y}, peak={peak_value} at Y={ore_data["peak_y"]}\n'
    array_content += f'        new OreTable("{ore_name.capitalize()}", {min_y}, {max_y}, new float[] {{\n'
    
    for i in range(0, len(scores), 10):
        chunk = scores[i:i+10]
        formatted = [format_float(s) for s in chunk]
        line = "            " + ", ".join(formatted) + ("," if i + 10 < len(scores) else "")
        array_content += line + "\n"
    
    comma = "," if ore_name != order[-1] else ""
    array_content += f"        }}){comma}\n"

# read existing Java file
java_file = "c:/Users/wecke/Projects/OreHeightIndicator/src/main/java/dev/wecke/oreheightindicator/data/StaticVanilla1211Provider.java"
with open(java_file, "r") as f:
    java_code = f.read()

# Update javadoc
java_code = re.sub(
    r"where 1\.0 equals\s+\*\s+the global peak \(Copper at Y=44: 37,855,643 per 100k blocks\)\.",
    r"where 1.0 equals\n * the local peak for that specific ore.",
    java_code
)

# Replace ORES
java_code = re.sub(
    r"(private static final OreTable\[\] ORES = \{\n).*?(\n    \};\n)",
    r"\1" + array_content.rstrip('\n') + r"\2",
    java_code,
    flags=re.DOTALL
)

with open(java_file, "w") as f:
    f.write(java_code)

print("Java file updated successfully!")
