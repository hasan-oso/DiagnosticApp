content = open('app/src/main/java/com/diagnostics/app/data/collectors/BatteryCollector.kt').read().splitlines()
stack = []
for i, line in enumerate(content):
    for char in line:
        if char == '{': stack.append(i + 1)
        elif char == '}':
            if stack: stack.pop()
            else: print('Extra } at line', i + 1)
if stack: print('Unclosed { at lines:', stack)
