# 📚 COMPILADOR ZYRA - DOCUMENTACIÓN TÉCNICA COMPLETA

## 🎯 Pipeline de Compilación Completo

```
┌─────────────────┐
│  Código Zyra    │  class Test { int x = 5 + 3; }
│   (.txt)        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ FASE 1: LÉXICO  │  Tokens: class, Test, {, int, x, =, 5, +, 3, ;, }
│  (Lexico.jflex) │  Scanner de JFlex
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ FASE 2: SINTÁC  │  AST con nodos:
│   (Desc. recursivo)│  NodoClase → NodoAsignacion → NodoOpBinaria
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ FASE 3: SEMÁNT  │  - Chequeo de tipos
│  (chequear())   │  - Tabla de símbolos
└────────┬────────┘  - Validación de scope
         │
         ▼
┌─────────────────┐
│ FASE 4: IR      │  GENERA TRES REPRESENTACIONES:
│  (generar())    │  1. TAC:         x = 5 + 3
│                 │  2. Cuádruplos:  (+, 5, 3, t0)  (=, t0, _, x)
│                 │  3. Triplos:     (0)(+, 5, 3)   (1)(=, [0], x)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ FASE 5: ASM     │  x86-64 NASM:
│ (GeneradorASM)  │    mov rax, 5
│                 │    add rax, 3
│                 │    mov [rbp-8], rax
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ NASM Assembler  │  Código Objeto (.obj)
│  -f win64       │  Binario relocatable con símbolos
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  GCC Linker     │  Ejecutable (.exe)
│  -mconsole      │  PE32+ Windows con:
│                 │  - Código linkado
│                 │  - Tabla de importaciones (printf, scanf, malloc)
│                 │  - Entry point
└─────────────────┘
```

---

## 📖 FASE 4: CÓDIGO INTERMEDIO (IR)

### ¿Qué es el Código Intermedio?

Representación **independiente de la arquitectura** entre AST y código máquina.

**Ventajas:**
- ✅ Portabilidad (cambiar backend sin tocar frontend)
- ✅ Optimización más sencilla
- ✅ Debugging intermedio
- ✅ Múltiples target architectures

**Zyra genera TRES representaciones simultáneamente:**

---

## 1️⃣ Código de Tres Direcciones (TAC)

**Formato:** `resultado = operando1 operador operando2`

**Características:**
- Máximo 3 operandos por instrucción
- Variables temporales explícitas
- Fácil de leer

**Ejemplo:**
```
x = a + b
y = x * 2
z = y - 3
```

---

## 2️⃣ Cuádruplos ✅

**Formato:** `(operador, arg1, arg2, resultado)`

**Estructura Java:**
```java
class Cuadruplo {
    String operador;  // +, -, *, /, =, <, if_false, goto, []=, =[], etc.
    String arg1;      // Primer operando
    String arg2;      // Segundo operando (puede estar vacío "_")
    String resultado; // Variable destino o etiqueta
    
    String toReadableString() {
        return String.format("(%s, %s, %s, %s)", operador, arg1, arg2, resultado);
    }
}
```

**Ejemplo:**
```java
(+, a, b, t0)       // t0 = a + b
(*, t0, 2, t1)      // t1 = t0 * 2
(-, t1, 3, z)       // z = t1 - 3
```

**Ventajas:**
- ✅ Referencias explícitas por nombre
- ✅ Fácil insertar/eliminar instrucciones
- ✅ Alta legibilidad
- ✅ Usado por generador ASM

**Desventajas:**
- ❌ Usa más memoria (4 campos)

---

## 3️⃣ Triplos ✅

**Formato:** `(operador, arg1, arg2)` con resultado implícito en posición

**Estructura Java:**
```java
class Triplo {
    String operador;  // +, -, *, /, =, <, etc.
    String arg1;      // Primer operando o referencia [n]
    String arg2;      // Segundo operando o referencia [n]
    
    String toString() {
        return String.format("(%s, %s, %s)", operador, arg1, arg2);
    }
}
```

**Ejemplo:**
```java
(0) (+, a, b)       // Resultado implícito en posición [0]
(1) (*, [0], 2)     // Usa resultado de triplo 0
(2) (-, [1], 3)     // Usa resultado de triplo 1
(3) (=, [2], z)     // z = resultado de triplo 2
```

**Referencias con `[n]`:**
- `[0]` = resultado del triplo en posición 0
- `[1]` = resultado del triplo en posición 1

**Ventajas:**
- ✅ Compacto (solo 3 campos)
- ✅ Ahorra memoria
- ✅ Optimización más directa

**Desventajas:**
- ❌ Difícil insertar/eliminar (renumerar referencias)
- ❌ Menos legible

---

## 🔄 Generación Sincronizada

**Clase:** `GeneradorCodigoIntermedio`

```java
public void emitir(String op, String arg1, String arg2, String res) {
    // 1. Generar Cuádruplo
    Cuadruplo cuad = new Cuadruplo(op, arg1, arg2, res);
    cuadruplos.add(cuad);
    
    // 2. Generar Triplo correspondiente
    convertirATriplo(cuad);
}

private void convertirATriplo(Cuadruplo c) {
    String arg1Triplo = mapearATriplo(c.getArg1());
    String arg2Triplo = mapearATriplo(c.getArg2());
    
    Triplo triplo = new Triplo(c.getOperador(), arg1Triplo, arg2Triplo);
    triplos.add(triplo);
    
    // Si genera temporal, mapear a índice de triplo
    if (c.getResultado().startsWith("t")) {
        mapaTempATriplo.put(c.getResultado(), triplos.size() - 1);
    }
}
```

**Ejemplo de sincronización:**

**Input:** `x = a + b * 2`

**TAC:**
```
t0 = b * 2
t1 = a + t0
x = t1
```

**Cuádruplos:**
```
(*, b, 2, t0)
(+, a, t0, t1)
(=, t1, _, x)
```

**Triplos:**
```
(0) (*, b, 2)
(1) (+, a, [0])
(2) (=, [1], x)
```

---

## 💡 Ejemplos de Transformación Completos

### Ejemplo 1: Expresión Aritmética

**Código Zyra:**
```java
int x = (5 + 3) * 2 - 1;
```

**TAC:**
```
t0 = 5 + 3
t1 = t0 * 2
t2 = t1 - 1
x = t2
```

**Cuádruplos:**
```
(+, 5, 3, t0)
(*, t0, 2, t1)
(-, t1, 1, t2)
(=, t2, _, x)
```

**Triplos:**
```
(0) (+, 5, 3)
(1) (*, [0], 2)
(2) (-, [1], 1)
(3) (=, [2], x)
```

**Ensamblador x86-64:**
```asm
; t0 = 5 + 3
mov rax, 5
add rax, 3
mov [rbp-8], rax        ; t0

; t1 = t0 * 2
mov rax, [rbp-8]
mov rbx, 2
imul rbx
mov [rbp-16], rax       ; t1

; t2 = t1 - 1
mov rax, [rbp-16]
sub rax, 1
mov [rbp-24], rax       ; t2

; x = t2
mov rax, [rbp-24]
mov [rbp-32], rax       ; x
```

---

### Ejemplo 2: Control de Flujo (While)

**Código Zyra:**
```java
int i = 0;
while (i < 5) {
    print(i);
    i = i + 1;
}
```

**TAC:**
```
i = 0
L0:
t0 = i < 5
if_false t0 goto L1
print(i)
t1 = i + 1
i = t1
goto L0
L1:
```

**Cuádruplos:**
```
(=, 0, _, i)
(label, _, _, L0)
(<, i, 5, t0)
(if_false, t0, _, L1)
(print, i, _, _)
(+, i, 1, t1)
(=, t1, _, i)
(goto, _, _, L0)
(label, _, _, L1)
```

**Triplos:**
```
(0) (=, 0, i)
(1) (label, L0, _)
(2) (<, i, 5)
(3) (if_false, [2], L1)
(4) (print, i, _)
(5) (+, i, 1)
(6) (=, [5], i)
(7) (goto, L0, _)
(8) (label, L1, _)
```

**Ensamblador x86-64:**
```asm
; i = 0
mov rax, 0
mov [rbp-8], rax

L0:
; t0 = i < 5
mov rax, [rbp-8]
cmp rax, 5
jl .Lt
mov rax, 0
jmp .Le
.Lt:
mov rax, 1
.Le:
mov [rbp-16], rax       ; t0

; if_false t0 goto L1
mov rax, [rbp-16]
cmp rax, 0
je L1

; print(i)
lea rcx, [fmt_int]
mov rdx, [rbp-8]
sub rsp, 32
call printf
add rsp, 32

; t1 = i + 1
mov rax, [rbp-8]
add rax, 1
mov [rbp-24], rax       ; t1

; i = t1
mov rax, [rbp-24]
mov [rbp-8], rax

; goto L0
jmp L0

L1:
```

---

### Ejemplo 3: Arrays Dinámicos

**Código Zyra:**
```java
int[] arr;
int n = 5;
arr = new int[n];
arr[2] = 10;
int x = arr[2];
```

**TAC:**
```
n = 5
t0 = new_array(int, n)
arr = t0
t1 = arr[2]
t1 = 10
t2 = arr[2]
x = t2
```

**Cuádruplos:**
```
(=, 5, _, n)
(new_array, int, n, t0)
(=, t0, _, arr)
([]=, arr, 2, 10)
(=[], arr, 2, t1)
(=, t1, _, x)
```

**Triplos:**
```
(0) (=, 5, n)
(1) (new_array, int, n)
(2) (=, [1], arr)
(3) ([]=, arr, 2, 10)
(4) (=[], arr, 2)
(5) (=, [4], x)
```

**Ensamblador x86-64:**
```asm
; n = 5
mov rax, 5
mov [rbp-8], rax

; new_array(int, 5)
mov rcx, [rbp-8]        ; n
imul rcx, 8             ; n * sizeof(int64)
sub rsp, 32
call malloc
add rsp, 32
mov [rbp-16], rax       ; t0 = puntero heap

; arr = t0
mov rax, [rbp-16]
mov [rbp-24], rax       ; arr = puntero

; arr[2] = 10
mov rdx, 10
mov rcx, 2
imul rcx, 8
mov rax, [rbp-24]       ; Cargar puntero arr
add rax, rcx            ; arr + (2 * 8)
mov [rax], rdx          ; *( arr+16 ) = 10

; x = arr[2]
mov rcx, 2
imul rcx, 8
mov rax, [rbp-24]
add rax, rcx
mov rax, [rax]          ; Leer valor
mov [rbp-32], rax       ; x
```

---

## 🔧 FASE 5: GENERACIÓN DE ENSAMBLADOR

### Clase: `GeneradorEnsamblador`

**Entrada:** `List<Cuadruplo>`  
**Salida:** Código x86-64 en sintaxis NASM

### Convenciones Windows x64

**Registros para parámetros:**
- `rcx` = 1er argumento (printf, scanf, malloc)
- `rdx` = 2do argumento
- `r8` = 3er argumento
- `r9` = 4to argumento
- Resto en stack

**Registros de trabajo:**
- `rax` = Acumulador, retorno de funciones
- `rbx` = General purpose
- `rsp` = Stack pointer (DEBE estar alineado 16 bytes)
- `rbp` = Frame pointer

**Shadow Space:**
- Reservar mínimo 32 bytes antes de `call`
- Windows x64 ABI requiere espacio para registros

### Layout de Stack

```
High Address
┌──────────────┐
│  Parámetros  │ +16
├──────────────┤
│ Return Addr  │ +8
├──────────────┤
│   Old RBP    │ ← rbp apunta aquí
├──────────────┤
│ Variable 1   │ -8  [rbp-8]
├──────────────┤
│ Variable 2   │ -16 [rbp-16]
├──────────────┤
│   ...        │
├──────────────┤
│ Shadow Space │ -32
└──────────────┘
Low Address ← rsp
```

### Métodos de Traducción

#### 1. **Asignación (`=`)**
```java
private void genAsig(Cuadruplo c) {
    // c: (=, valor, _, destino)
    cargar("rax", c.getArg1());
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

**Ejemplo:** `(=, 5, _, x)` →
```asm
mov rax, 5
mov [rbp-8], rax
```

#### 2. **Operaciones Aritméticas**
```java
private void genArit(Cuadruplo c) {
    cargar("rax", c.getArg1());
    switch (c.getOperador()) {
        case "+":
            if (esNum(c.getArg2()))
                gen("add rax, " + c.getArg2());
            else
                gen("add rax, " + loc(c.getArg2()));
            break;
        case "-":
            // Similar...
        case "*":
            cargar("rbx", c.getArg2());
            gen("imul rbx");
            break;
        case "/":
            gen("cqo");  // Extender signo RAX → RDX:RAX
            cargar("rbx", c.getArg2());
            gen("idiv rbx");
            break;
    }
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

#### 3. **Comparaciones**
```java
private void genCmp(Cuadruplo c) {
    cargar("rax", c.getArg1());
    gen("cmp rax, " + obtener(c.getArg2()));
    
    String etiqTrue = ".L" + System.nanoTime() + "t";
    String etiqEnd = ".L" + System.nanoTime() + "e";
    
    // Salto condicional según operador
    switch (c.getOperador()) {
        case "<":  gen("jl " + etiqTrue); break;
        case ">":  gen("jg " + etiqTrue); break;
        case "<=": gen("jle " + etiqTrue); break;
        case ">=": gen("jge " + etiqTrue); break;
        case "==": gen("je " + etiqTrue); break;
        case "!=": gen("jne " + etiqTrue); break;
    }
    
    // Falso: resultado = 0
    gen("mov rax, 0");
    gen("jmp " + etiqEnd);
    
    // Verdadero: resultado = 1
    codigo.append(etiqTrue + ":\n");
    gen("mov rax, 1");
    
    codigo.append(etiqEnd + ":\n");
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

#### 4. **Lectura de Array**
```java
private void genLecturaArray(Cuadruplo c) {
    // (=[], arr, indice, temp)
    // temp = arr[indice]
    
    // Cargar índice y multiplicar por 8
    cargar("rcx", c.getArg2());
    gen("imul rcx, 8");
    
    // Cargar puntero del array (está en heap)
    gen("mov rax, " + loc(c.getArg1()));
    
    // Calcular dirección: base + (indice * 8)
    gen("add rax, rcx");
    
    // Leer valor
    gen("mov rax, [rax]");
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

#### 5. **Escritura en Array**
```java
private void genEscrituraArray(Cuadruplo c) {
    // ([]=, arr, indice, valor)
    // arr[indice] = valor
    
    cargar("rdx", c.getResultado());  // Valor
    cargar("rcx", c.getArg2());       // Índice
    gen("imul rcx, 8");
    gen("mov rax, " + loc(c.getArg1()));
    gen("add rax, rcx");
    gen("mov [rax], rdx");
}
```

#### 6. **Allocación de Array**
```java
private void genNewArray(Cuadruplo c) {
    // (new_array, tipo, tamaño, resultado)
    
    // Calcular bytes: tamaño * 8
    cargar("rcx", c.getArg2());
    gen("imul rcx, 8");
    
    // Llamar malloc(rcx)
    gen("sub rsp, 32");  // Shadow space
    gen("call malloc");
    gen("add rsp, 32");
    
    // Guardar puntero retornado
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

---

## 🔨 PASO 6: ENSAMBLADOR (NASM)

### ¿Qué es NASM?

**Netwide Assembler** - Ensamblador para x86/x86-64

**Comando:**
```bash
nasm -f win64 programa.asm -o programa.obj
```

**Parámetros:**
- `-f win64` = Formato de salida PE32+ (Windows 64-bit)
- `-o` = Archivo objeto de salida

### Código Objeto (.obj)

**Formato:** COFF (Common Object File Format)

**Contiene:**
- ✅ Código máquina (bytes de instrucciones)
- ✅ Tabla de símbolos (funciones, variables)
- ✅ Tabla de relocación (direcciones a ajustar)
- ✅ Símbolos externos sin resolver (`printf`, `scanf`, etc.)

**Ejemplo:**
```
Section .text:
  48 89 C8         ; mov rax, rcx
  50               ; push rax
  E8 00 00 00 00   ; call printf (dirección SIN RESOLVER)
  
Symbol Table:
  printf: UNDEF EXTERNAL
  main: CODE 0x0000
  
Relocation Table:
  0x0005 → printf (call instruction)
```

---

## 🔗 PASO 7: LINKEO (GCC)

### ¿Qué hace el Linker?

**Comando:**
```bash
gcc programa.obj -o programa.exe -mconsole
```

**Procesos:**
1. **Resolver símbolos externos** (printf, scanf, malloc de libc)
2. **Combinar secciones** (.text, .data, .bss)
3. **Ajustar direcciones** (relocación)
4. **Crear ejecutable** Windows PE

### Tabla de Importaciones (Import Table)

```
DLL: msvcrt.dll
  printf @ 0x401000
  scanf  @ 0x401008
  malloc @ 0x401010
  
DLL: kernel32.dll
  ExitProcess @ 0x401018
```

### Entry Point

```asm
_start:
    sub rsp, 40        ; Alinear stack
    call main          ; Llamar a función main()
    mov rcx, rax       ; exit code
    call ExitProcess   ; Terminar proceso
```

---

## 🚀 EJECUTABLE FINAL (.exe)

### Formato: PE32+ (Portable Executable)

**Estructura:**
```
┌─────────────────┐
│   DOS Header    │ MZ signature
├─────────────────┤
│   PE Header     │ Características, arquitectura
├─────────────────┤
│ Section Headers │ .text, .data, .rdata, .idata
├─────────────────┤
│  .text Section  │ Código máquina ejecutable
├─────────────────┤
│  .data Section  │ Datos inicializados (strings)
├─────────────────┤
│ .idata Section  │ Import Address Table
├─────────────────┤
│ Relocation Info │ Para ASLR
└─────────────────┘
```

**Ejecución:**
1. Windows loader carga .exe en memoria
2. Mapea DLLs necesarias (msvcrt.dll, kernel32.dll)
3. Resuelve Import Address Table
4. Salta a entry point
5. Ejecuta main()

---

## 📊 Comparación Final: Cuádruplos vs Triplos

| Aspecto | Cuádruplos ✅ | Triplos ✅ |
|---------|--------------|----------|
| **Campos** | 4 (op, arg1, arg2, res) | 3 (op, arg1, arg2) |
| **Referencias** | Por nombre (`t0`, `x`) | Por posición (`[0]`, `[1]`) |
| **Legibilidad** | ⭐⭐⭐⭐⭐ Alta | ⭐⭐⭐ Media |
| **Memoria** | Más (4 strings) | Menos (3 strings) |
| **Edición** | Fácil | Difícil (renumerar) |
| **Optimización** | Media | Alta |
| **Usado por ASM** | ✅ Sí | ❌ No (solo visualización) |

**¿Por qué Zyra usa ambos?**
- **Cuádruplos:** Generación de código (ASM)
- **Triplos:** Visualización compacta en IDE

---

## 🎓 Resumen Completo del Pipeline

### Transformación Paso a Paso

**Input:**
```java
int x = 5 + 3;
```

1. **Tokens:** `int`, `x`, `=`, `5`, `+`, `3`, `;`

2. **AST:**
```
NodoDeclaracion
  ├─ Tipo: int
  └─ NodoAsignacion
      ├─ Id: x
      └─ NodoOpBinaria (+)
          ├─ NodoLiteral(5)
          └─ NodoLiteral(3)
```

3. **TAC:** `t0 = 5 + 3`, `x = t0`

4. **Cuádruplos:** `(+, 5, 3, t0)`, `(=, t0, _, x)`

5. **Triplos:** `(0)(+, 5, 3)`, `(1)(=, [0], x)`

6. **ASM:**
```asm
mov rax, 5
add rax, 3
mov [rbp-8], rax
```

7. **Código Objeto:** Bytes máquina relocatable

8. **Ejecutable:** PE32+ Windows con libc linkeado

---

## 📁 Archivos del Compilador

```
src/
├── modulos/ir/
│   ├── GeneradorCodigoIntermedio.java  ← Genera TAC/Cuádruplos/Triplos
│   ├── Cuadruplo.java                  ← Estructura cuádruplo
│   ├── Triplo.java                     ← Estructura triplo
│   └── GeneradorEnsamblador.java       ← IR → x86-64
├── estructuras/ast/
│   ├── expresiones/
│   │   └── *.java                      ← generar(gci)
│   └── sentencias/
│       └── *.java                      ← generar(gci)
└── Vistas/
    └── Main.java                       ← Pipeline completo
```

---

## ✨ Características Implementadas

✅ **3 Representaciones IR:** TAC, Cuádruplos, Triplos  
✅ **Generación x86-64 NASM**  
✅ **Compilación a .exe nativo**  
✅ **Arrays heap con malloc**  
✅ **Strings en sección .data**  
✅ **Control de flujo (while, if)**  
✅ **Operaciones: +, -, *, /**  
✅ **I/O: printf, scanf**  
✅ **Sincronización automática IR**  

---

## 🎯 Conclusión

**Compilador Zyra** = Pipeline completo y profesional:

```
Código Fuente → Léxico → Sintáctico → Semántico →
IR (TAC/Cuádruplos/Triplos) → ASM x64 → Objeto → Ejecutable
```

**Estado:** ✅ Producción  
**Target:** Windows x86-64  
**Runtime:** libc (printf, scanf, malloc)

---
