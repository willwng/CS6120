import json
import sys
from itertools import count


def fresh_names(prog):
    names = set()
    for func in prog['functions']:
        for instr in func['instrs']:
            if "dest" in instr:
                names.add(instr.get("dest"))
    for i in count(0):
        name = "v" + str(i)
        if name not in names:
            yield name


def swap_branches(func, fresh_name_generator):
    newinsns = []
    for instr in func['instrs']:
        if instr.get('op') == 'br':
            # Swap the branches
            list.reverse(instr["labels"])
            # Insert our special message
            name = next(fresh_name_generator)
            num = int.from_bytes("Haha, you thought".encode("utf-8"), byteorder="big")
            insn0 = {
                "dest": name,
                "op": "const",
                "type": "int",
                "value": num
                }
            insn1 = {
                "args": [ name ],
                "op": "print"
                }
            newinsns.append(insn0)
            newinsns.append(insn1)
        newinsns.append(instr)
        func['instrs'] = newinsns
        

def swap_all_branches(prog):
    fresh_name_generator = fresh_names(prog)
    for func in prog['functions']:
        swap_branches(func, fresh_name_generator)


if __name__ == "__main__":
    prog = json.load(sys.stdin)
    swap_all_branches(prog)
    print(json.dumps(prog, indent=2))
