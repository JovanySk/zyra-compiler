# REPORTE 3: GENERACIÓN DE ENSAMBLADOR Y CÓDIGO OBJETO

## 🎯 PIPELINE DE GENERACIÓN

```
Cuádruplos IR → GeneradorEnsamblador → ASM x86-64 → NASM → .obj → GCC → .exe
```

---

## 📝 GENERACIÓN DE ENSAMBLADOR x86-64

### Clase: `GeneradorEnsamblador`

**Responsabilidad:** Traducir cuádruplos a código ensamblador NASM x86-64.

### Estructura del ASM Generado

```asm
bits 64                    ; Arquitectura 64-bit
default rel                ; Direccionamiento relativo

extern printf              ; Funciones de libc
extern scanf
extern malloc

section .data              ; Datos inicializados
    fmt_int db '%d', 10, 0
    fmt_str db '%s', 10, 0
    str0 db 'Mensaje', 0

section .text              ; Código ejecutable
    global main

main:
    push rbp
    mov rbp, rsp
    sub rsp, 64            ; Espacio para variables locales
    
    ; ... código generado ...
    
    xor rax, rax           ; return 0
    mov rsp, rbp
    pop rbp
    ret
```

---

## 🔧 CONVENCIONES WINDOWS X64 ABI

### Registros de Parámetros

| Parámetro | Registro | Uso |
|-----------|----------|-----|
| 1° | `rcx` | printf, scanf, malloc |
| 2° | `rdx` | Segundo argumento |
| 3° | `r8` | Tercer argumento |
| 4° | `r9` | Cuarto argumento |
| 5°+ | Stack | Push en orden inverso |

### Registros de Propósito General

- **`rax`** - Acumulador, valor de retorno
- **`rbx`** - Base, preservado por callee
- **`rcx`** - Contador, 1er argumento
- **`rdx`** - Datos, 2do argumento
- **`rsp`** - Stack pointer (MUST be 16-byte aligned)
- **`rbp`** - Frame pointer

### Shadow Space

Windows x64 requiere **32 bytes de shadow space** antes de llamadas:

```asm
sub rsp, 32    ; Reservar shadow space
call printf
add rsp, 32    ; Liberar
```

---

## 🔨 TRADUCCIÓN DE CUÁDRUPLOS

### 1. Asignación (`=`)

**Cuádruplo:** `(=, 5, _, x)`

**ASM:**
```asm
; x = 5
mov rax, 5
mov [rbp-8], rax
```

**Método:**
```java
private void genAsig(Cuadruplo c) {
    comentario(c.toReadableString());
    cargar("rax", c.getArg1());
    gen("mov " + loc(c.getResultado()) + ", rax");
}
```

---

### 2. Suma (`+`)

**Cuádruplo:** `(+, a, b, t0)`

**ASM:**
```asm
; t0 = a + b
mov rax, [rbp-8]     ; a
add rax, [rbp-16]    ; b
mov [rbp-24], rax    ; t0
```

**Método:**
```java
case "+":
    if (esNum(c.getArg2()))
        gen("add rax, " + c.getArg2());
    else
        gen("add rax, " + loc(c.getArg2()));
    break;
```

---

### 3. Multiplicación (`*`)

**Cuádruplo:** `(*, a, b, t0)`

**ASM:**
```asm
; t0 = a * b
mov rax, [rbp-8]     ; a
mov rbx, [rbp-16]    ; b
imul rbx             ; rax = rax * rbx
mov [rbp-24], rax    ; t0
```

---

### 4. División (`/`)

**Cuádruplo:** `(/, a, b, t0)`

**ASM:**
```asm
; t0 = a / b
mov rax, [rbp-8]     ; a
cqo                  ; Extender signo RDX:RAX
mov rbx, [rbp-16]    ; b
idiv rbx             ; RAX = RDX:RAX / RBX
mov [rbp-24], rax    ; t0
```

**Nota:** `cqo` extiende `rax` a `rdx:rax` para divisiones con signo.

---

### 5. Comparación (`<`)

**Cuádruplo:** `(<, a, b, t0)`

**ASM:**
```asm
; t0 = (a < b)
mov rax, [rbp-8]     ; a
cmp rax, [rbp-16]    ; Comparar con b
jl .Lt               ; Si a < b, saltar
mov rax, 0           ; Falso
jmp .Le
.Lt:
mov rax, 1           ; Verdadero
.Le:
mov [rbp-24], rax    ; t0
```

**Saltos condicionales:**
- `jl` - Jump if Less (signed)
- `jg` - Jump if Greater
- `je` - Jump if Equal
- `jne` - Jump if Not Equal
- `jle` - Jump if Less or Equal
- `jge` - Jump if Greater or Equal

---

### 6. Salto Condicional (`if_false`)

**Cuádruplo:** `(if_false, t0, _, L1)`

**ASM:**
```asm
mov rax, [rbp-8]     ; t0
cmp rax, 0
je L1                ; Si t0 == 0, saltar a L1
```

---

### 7. Etiqueta (`label`)

**Cuádruplo:** `(label, _, _, L0)`

**ASM:**
```asm
L0:
```

---

### 8. Salto Incondicional (`goto`)

**Cuádruplo:** `(goto, _, _, L0)`

**ASM:**
```asm
jmp L0
```

---

### 9. Print de Entero (`print`)

**Cuádruplo:** `(print, x, _, _)`

**ASM:**
```asm
lea rcx, [fmt_int]   ; 1er arg: formato "%d\n"
mov rdx, [rbp-8]     ; 2do arg: valor de x
sub rsp, 32          ; Shadow space
call printf
add rsp, 32
```

---

### 10. Print de String (`print_str`)

**Cuádruplo:** `(print_str, "Hola", _, _)`

**ASM:**
```asm
lea rcx, [fmt_str]   ; Formato "%s\n"
lea rdx, [str0]      ; Dirección del string
sub rsp, 32
call printf
add rsp, 32
```

---

### 11. Read (`read`)

**Cuádruplo:** `(read, _, _, t0)`

**ASM:**
```asm
lea rcx, [fmt_scan]  ; Formato "%d"
lea rdx, [rbp-8]     ; Dirección donde guardar
sub rsp, 32
call scanf
add rsp, 32
```

---

### 12. Acceso a Array (`=[]`)

**Cuádruplo:** `(=[], arr, i, t0)`  
**Significado:** `t0 = arr[i]`

**ASM:**
```asm
; t0 = arr[i]
mov rcx, [rbp-16]    ; i
imul rcx, 8          ; i * sizeof(int64)
mov rax, [rbp-8]     ; Puntero arr
add rax, rcx         ; arr + (i * 8)
mov rax, [rax]       ; Leer valor
mov [rbp-24], rax    ; t0
```

---

### 13. Asignación a Array (`[]=`)

**Cuádruplo:** `([]=, arr, i, 10)`  
**Significado:** `arr[i] = 10`

**ASM:**
```asm
; arr[i] = 10
mov rdx, 10          ; Valor
mov rcx, [rbp-16]    ; i
imul rcx, 8
mov rax, [rbp-8]     ; Puntero arr
add rax, rcx
mov [rax], rdx       ; Escribir valor
```

---

### 14. Allocación de Array (`new_array`)

**Cuádruplo:** `(new_array, int, 5, t0)`

**ASM:**
```asm
; t0 = malloc(5 * 8)
mov rcx, 5
imul rcx, 8          ; 5 * sizeof(int64) = 40
sub rsp, 32
call malloc
add rsp, 32
mov [rbp-8], rax     ; t0 = puntero
```

---

## 🔨 ENSAMBLAJE CON NASM

### Comando

```bash
nasm -f win64 programa.asm -o programa.obj
```

### Parámetros

- `-f win64` - Formato Windows 64-bit (COFF/PE)
- `-o` - Archivo objeto de salida

### Salida: Código Objeto (.obj)

**Formato:** COFF (Common Object File Format)

**Contenido:**
```
┌─────────────────┐
│  File Header    │ Machine: AMD64
├─────────────────┤
│ Section Table   │ .text, .data, .rdata
├─────────────────┤
│  .text Section  │ Código máquina (bytes)
│  [Código]       │ 48 89 E5 ...
├─────────────────┤
│  .data Section  │ Strings, constantes
├─────────────────┤
│ Symbol Table    │ main: CODE 0x0000
│                 │ printf: UNDEF EXTERN
│                 │ scanf: UNDEF EXTERN
├─────────────────┤
│ Relocation Tbl  │ 0x0015 → printf
│                 │ 0x002A → scanf
└─────────────────┘
```

---

## 🔗 LINKEO CON GCC

### Comando

```bash
gcc programa.obj -o programa.exe -mconsole
```

### Proceso del Linker

#### 1. Resolver Símbolos Externos

```
printf  → msvcrt.dll!printf
scanf   → msvcrt.dll!scanf
malloc  → msvcrt.dll!malloc
```

#### 2. Combinar Secciones

```
.text (todos los .obj) → .text ejecutable
.data (todos los .obj) → .data inicializada
```

#### 3. Relocación

Ajustar direcciones absolutas:

**Antes:**
```asm
call 0x00000000  ; Dirección temporal
```

**Después:**
```asm
call 0x00401050  ; Dirección real en .exe
```

#### 4. Crear Import Address Table (IAT)

```
0x401000: printf  → thunk a msvcrt.dll
0x401008: scanf   → thunk a msvcrt.dll
0x401010: malloc  → thunk a msvcrt.dll
```

---

## 📦 EJECUTABLE FINAL (.exe)

### Formato: PE32+ (Portable Executable)

```
┌─────────────────┐
│   DOS Header    │ "MZ" signature
├─────────────────┤
│   PE Header     │ Machine: AMD64
│                 │ Subsystem: CONSOLE
├─────────────────┤
│ Section Headers │
│  .text          │ RX (Read/Execute)
│  .data          │ RW (Read/Write)
│  .rdata         │ R  (Read-only)
│  .idata         │ Import Directory
├─────────────────┤
│  .text Section  │
│  [main code]    │
├─────────────────┤
│  .data Section  │
│  [strings]      │
├─────────────────┤
│ .idata Section  │
│  Import Table   │
│  - msvcrt.dll   │
│    * printf     │
│    * scanf      │
│  - kernel32.dll │
│    * ExitProcess│
├─────────────────┤
│ Reloc Table     │ Para ASLR
└─────────────────┘
```

### Entry Point

```asm
_start:
    sub rsp, 40          ; Alinear stack (16-byte)
    call main            ; Ejecutar main()
    mov rcx, rax         ; Exit code
    call ExitProcess     ; Terminar
```

---

## 🚀 EJECUCIÓN

### 1. Windows Loader

- Carga .exe en memoria
- Mapea DLLs (msvcrt.dll, kernel32.dll)
- Resuelve Import Address Table
- Aplica relocaciones (ASLR)

### 2. Inicialización CRT

- Inicializar heap
- Configurar stdio
- Parsear argumentos

### 3. Llamar Entry Point

- Saltar a `main()`

### 4. Ejecutar Programa

- Código de usuario se ejecuta

### 5. Terminar

- Llamar `ExitProcess(exit_code)`

---

## 📊 EJEMPLO COMPLETO

### Código Zyra

```java
int x = 5;
print(x);
```

### Cuádruplos

```
(=, 5, _, x)
(print, x, _, _)
```

### ASM Generado

```asm
bits 64
default rel

extern printf

section .data
    fmt_int db '%d', 10, 0

section .text
    global main

main:
    push rbp
    mov rbp, rsp
    sub rsp, 16          ; 1 variable local
    
    ; x = 5
    mov rax, 5
    mov [rbp-8], rax
    
    ; print(x)
    lea rcx, [fmt_int]
    mov rdx, [rbp-8]
    sub rsp, 32
    call printf
    add rsp, 32
    
    ; return 0
    xor rax, rax
    mov rsp, rbp
    pop rbp
    ret
```

### Bytes Máquina (.obj)

```
55                   ; push rbp
48 89 E5             ; mov rbp, rsp
48 83 EC 10          ; sub rsp, 16
48 C7 C0 05 00 00 00 ; mov rax, 5
48 89 45 F8          ; mov [rbp-8], rax
...
```

### Ejecutable (.exe)

- Tamaño: ~80 KB (con CRT)
- Dependencias: msvcrt.dll, kernel32.dll
- Entry point: 0x401000

---

## 🎯 CONCLUSIÓN

Zyra genera código ensamblador x86-64 **eficiente y estándar**:

✅ **Convenciones Windows x64 ABI**  
✅ **Código objeto relocatable**  
✅ **Linkeo con libc**  
✅ **Ejecutables PE32+ nativos**  

**Resultado:** Programas Zyra compilados a binarios Windows nativos optimizados.
