# 🔨 CÓMO GENERAR CÓDIGO OBJETO (.obj) - GUÍA COMPLETA

## 🎯 Concepto Clave

**NO generas código objeto directamente desde Java.** En su lugar:

1. Tu compilador genera **código ensamblador** (texto .asm)
2. **NASM** convierte .asm → .obj (código objeto binario)
3. **GCC** linkea .obj → .exe (ejecutable final)

---

## 📝 PASO 1: Generar Código Ensamblador (.asm)

### Clase: `GeneradorEnsamblador.java`

**Ubicación:** `src/modulos/ir/GeneradorEnsamblador.java`

**Método principal:**
```java
public String generar(List<Cuadruplo> cuadruplos) {
    StringBuilder codigo = new StringBuilder();
    
    // 1. Header - Directivas NASM
    codigo.append("bits 64\n");           // Arquitectura 64-bit
    codigo.append("default rel\n\n");     // Direccionamiento relativo
    
    // 2. Declarar funciones externas (libc)
    codigo.append("extern printf\n");
    codigo.append("extern scanf\n");
    codigo.append("extern malloc\n\n");
    
    // 3. Sección .data - Strings y constantes
    generarSeccionData(codigo);
    
    // 4. Sección .text - Código ejecutable
    codigo.append("section .text\n");
    codigo.append("    global main\n\n");
    codigo.append("main:\n");
    codigo.append("    push rbp\n");
    codigo.append("    mov rbp, rsp\n");
    codigo.append("    sub rsp, " + calcularTamanoStack() + "\n\n");
    
    // 5. Traducir cada cuádruplo a instrucciones ASM
    for (Cuadruplo c : cuadruplos) {
        traducir(c);
    }
    
    // 6. Epílogo - Return
    codigo.append("    xor rax, rax\n");    // return 0
    codigo.append("    mov rsp, rbp\n");
    codigo.append("    pop rbp\n");
    codigo.append("    ret\n");
    
    return codigo.toString();
}
```

**Ejemplo de ASM generado:**
```asm
bits 64
default rel

extern printf
extern scanf

section .data
    fmt_int db '%d', 10, 0
    str0 db 'Hola Mundo', 0

section .text
    global main

main:
    push rbp
    mov rbp, rsp
    sub rsp, 16
    
    ; x = 5
    mov rax, 5
    mov [rbp-8], rax
    
    ; print(x)
    lea rcx, [fmt_int]
    mov rdx, [rbp-8]
    sub rsp, 32
    call printf
    add rsp, 32
    
    xor rax, rax
    mov rsp, rbp
    pop rbp
    ret
```

---

## 💾 PASO 2: Guardar a Archivo .asm

### Clase: `Main.java`

**Método:** `compilarYEjecutar(String codigoASM)`

```java
private void compilarYEjecutar(String codigoASM) throws IOException {
    // 1. Obtener nombre del archivo
    String nombreBase = obtenerNombreSinExtension();
    
    // 2. Definir ruta de salida (Documentos)
    Path dirDocumentos = Paths.get(
        System.getProperty("user.home"), 
        "OneDrive", 
        "Documentos"
    );
    
    Path archivoASM = dirDocumentos.resolve(nombreBase + ".asm");
    Path archivoOBJ = dirDocumentos.resolve(nombreBase + ".obj");
    Path archivoEXE = dirDocumentos.resolve(nombreBase + ".exe");
    
    // 3. Escribir código ASM a archivo
    Files.write(
        archivoASM, 
        codigoASM.getBytes(StandardCharsets.UTF_8)
    );
    
    System.out.println("✓ Archivo ASM generado: " + archivoASM);
    
    // Continúa en paso 3...
}
```

**Resultado:**
```
C:\Users\usuario\OneDrive\Documentos\programa.asm
```

---

## 🔧 PASO 3: Ensamblar con NASM (.asm → .obj)

### Herramienta: NASM (Netwide Assembler)

**Instalación:**
```powershell
choco install nasm
```

**Comando desde Java:**
```java
// Construir proceso NASM
ProcessBuilder pbNasm = new ProcessBuilder(
    "nasm",                      // Comando
    "-f", "win64",               // Formato: Windows 64-bit COFF
    archivoASM.toString(),       // Input: programa.asm
    "-o", archivoOBJ.toString()  // Output: programa.obj
);

// Configurar directorio de trabajo
pbNasm.directory(archivoASM.getParent().toFile());

// Redirigir output y error
pbNasm.redirectErrorStream(true);

// Ejecutar proceso
Process pNasm = pbNasm.start();

// Capturar output (para debugging)
BufferedReader reader = new BufferedReader(
    new InputStreamReader(pNasm.getInputStream())
);
String line;
while ((line = reader.readLine()) != null) {
    System.out.println("NASM: " + line);
}

// Esperar a que termine
int exitCode = pNasm.waitFor();

if (exitCode != 0) {
    throw new IOException("NASM falló con código: " + exitCode);
}

System.out.println("✓ Código objeto generado: " + archivoOBJ);
```

**Equivalente en línea de comandos:**
```bash
nasm -f win64 programa.asm -o programa.obj
```

---

## 🔍 ¿Qué contiene el archivo .obj?

**Formato:** COFF (Common Object File Format)

**Estructura:**
```
┌─────────────────────┐
│   COFF Header       │  Información del archivo
├─────────────────────┤
│  Section Headers    │  Metadatos de secciones
├─────────────────────┤
│   .text Section     │  Código máquina (bytes)
│   48 89 E5          │  ← mov rbp, rsp
│   48 83 EC 10       │  ← sub rsp, 16
│   ...               │
├─────────────────────┤
│   .data Section     │  Datos inicializados
│   "Hola\0"          │
├─────────────────────┤
│  Symbol Table       │  Símbolos definidos/externos
│  main: CODE 0x0000  │
│  printf: UNDEF      │  ← Externo (sin resolver)
│  scanf: UNDEF       │
├─────────────────────┤
│ Relocation Table    │  Direcciones a ajustar
│  0x0015 → printf    │  ← Donde está el call printf
└─────────────────────┘
```

**Símbolos UNDEF:** Funciones externas que GCC resolverá.

---

## 🔗 PASO 4: Linkear con GCC (.obj → .exe)

### Herramienta: GCC (GNU Compiler Collection)

**Instalación:**
```powershell
choco install mingw
```

**Comando desde Java:**
```java
// Construir proceso GCC
ProcessBuilder pbGcc = new ProcessBuilder(
    "gcc",                       // Comando
    archivoOBJ.toString(),       // Input: programa.obj
    "-o", archivoEXE.toString(), // Output: programa.exe
    "-mconsole"                  // Subsistema: Consola
);

// Configurar directorio
pbGcc.directory(archivoOBJ.getParent().toFile());

// Redirigir output
pbGcc.redirectErrorStream(true);

// Ejecutar
Process pGcc = pbGcc.start();

// Capturar output
BufferedReader reader = new BufferedReader(
    new InputStreamReader(pGcc.getInputStream())
);
String line;
while ((line = reader.readLine()) != null) {
    System.out.println("GCC: " + line);
}

// Esperar
int exitCode = pGcc.waitFor();

if (exitCode != 0) {
    throw new IOException("GCC falló con código: " + exitCode);
}

System.out.println("✓ Ejecutable generado: " + archivoEXE);
```

**Equivalente en línea de comandos:**
```bash
gcc programa.obj -o programa.exe -mconsole
```

---

## 🔍 ¿Qué hace GCC?

### 1. Resolver Símbolos Externos

```
printf  → msvcrt.dll!printf
scanf   → msvcrt.dll!scanf
malloc  → msvcrt.dll!malloc
```

### 2. Crear Import Address Table (IAT)

```
0x401000: printf  → thunk a msvcrt.dll
0x401008: scanf   → thunk a msvcrt.dll
0x401010: malloc  → thunk a msvcrt.dll
```

### 3. Ajustar Relocaciones

**Antes (en .obj):**
```asm
call 0x00000000  ; Dirección temporal
```

**Después (en .exe):**
```asm
call 0x401050    ; Dirección real del thunk
```

### 4. Crear Ejecutable PE32+

```
┌─────────────────┐
│   DOS Header    │  "MZ" signature
├─────────────────┤
│   PE Header     │  Machine: AMD64
│                 │  Subsystem: CONSOLE
├─────────────────┤
│ Section Headers │
│  .text  (RX)    │  Código ejecutable
│  .data  (RW)    │  Datos
│  .rdata (R)     │  Constantes
│  .idata         │  Import Directory
├─────────────────┤
│  .text Section  │
│  [Código]       │
├─────────────────┤
│  .data Section  │
│  [Strings]      │
├─────────────────┤
│ .idata Section  │
│  Import Table   │
│  - msvcrt.dll   │
│    * printf     │
│    * scanf      │
│  - kernel32.dll │
│    * ExitProcess│
└─────────────────┘
```

---

## 🚀 PASO 5: Ejecutar el .exe

### Desde Java

```java
private void ejecutarEnPowerShell(Path archivoEXE) throws IOException {
    // Crear script temporal de PowerShell
    String script = String.format(
        "& '%s'; " +
        "Write-Host ''; " +
        "Write-Host 'Presione cualquier tecla...'; " +
        "$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')",
        archivoEXE.toString()
    );
    
    // Ejecutar en nueva ventana PowerShell
    ProcessBuilder pb = new ProcessBuilder(
        "powershell",
        "-NoExit",
        "-Command", script
    );
    
    pb.start();
}
```

---

## 📊 Código Completo - `Main.java`

```java
private void compilarYEjecutar(String codigoASM) throws IOException {
    String nombreBase = obtenerNombreSinExtension();
    Path dirDocs = Paths.get(System.getProperty("user.home"), 
                             "OneDrive", "Documentos");
    
    Path archivoASM = dirDocs.resolve(nombreBase + ".asm");
    Path archivoOBJ = dirDocs.resolve(nombreBase + ".obj");
    Path archivoEXE = dirDocs.resolve(nombreBase + ".exe");
    
    // PASO 2: Guardar .asm
    Files.write(archivoASM, codigoASM.getBytes(StandardCharsets.UTF_8));
    System.out.println("✓ ASM: " + archivoASM);
    
    // PASO 3: NASM .asm → .obj
    ProcessBuilder pbNasm = new ProcessBuilder(
        "nasm", "-f", "win64", 
        archivoASM.toString(), 
        "-o", archivoOBJ.toString()
    );
    pbNasm.redirectErrorStream(true);
    Process pNasm = pbNasm.start();
    
    // Leer output NASM
    try (BufferedReader r = new BufferedReader(
            new InputStreamReader(pNasm.getInputStream()))) {
        String line;
        while ((line = r.readLine()) != null) {
            System.out.println("NASM: " + line);
        }
    }
    
    if (pNasm.waitFor() != 0) {
        throw new IOException("NASM falló");
    }
    System.out.println("✓ OBJ: " + archivoOBJ);
    
    // PASO 4: GCC .obj → .exe
    ProcessBuilder pbGcc = new ProcessBuilder(
        "gcc", 
        archivoOBJ.toString(), 
        "-o", archivoEXE.toString(), 
        "-mconsole"
    );
    pbGcc.redirectErrorStream(true);
    Process pGcc = pbGcc.start();
    
    // Leer output GCC
    try (BufferedReader r = new BufferedReader(
            new InputStreamReader(pGcc.getInputStream()))) {
        String line;
        while ((line = r.readLine()) != null) {
            System.out.println("GCC: " + line);
        }
    }
    
    if (pGcc.waitFor() != 0) {
        throw new IOException("GCC falló");
    }
    System.out.println("✓ EXE: " + archivoEXE);
    
    // PASO 5: Ejecutar
    ejecutarEnPowerShell(archivoEXE);
}
```

---

## 🎯 Resumen para Explicar

### ❌ Lo que NO se hace:

```java
// ESTO NO EXISTE en Java estándar
CodigoObjeto obj = Java.compilarDirectoAOBJ(codigoASM);
```

### ✅ Lo que SÍ se hace:

1. **Tu compilador genera texto ASM** (String)
2. **Guardas a archivo** (.asm)
3. **Llamas a NASM** (vía ProcessBuilder)
4. **Llamas a GCC** (vía ProcessBuilder)
5. **Ejecutas el .exe** (vía ProcessBuilder)

---

## 🔧 Herramientas Necesarias

### Windows (Chocolatey)
```powershell
choco install nasm
choco install mingw
```

### Verificar instalación
```powershell
nasm -version
gcc --version
```

---

## 🐛 Troubleshooting

### Problema: "nasm no reconocido"

**Solución:**
```powershell
# Añadir NASM al PATH
$env:Path += ";C:\Program Files\NASM"
```

### Problema: "gcc no reconocido"

**Solución:**
```powershell
# Añadir MinGW al PATH
$env:Path += ";C:\ProgramData\chocolatey\lib\mingw\tools\install\mingw64\bin"
```

### Problema: "No encuentra printf"

**Solución:** GCC necesita linkear con `msvcrt.dll` (automático con `-mconsole`)

---

## 🎓 Conclusión

**Generar código objeto desde un compilador Java:**

1. ✅ Genera ASM (tu clase `GeneradorEnsamblador`)
2. ✅ Usa NASM para ensamblar
3. ✅ Usa GCC para linkear
4. ✅ Ejecuta con ProcessBuilder

**No hay librerías mágicas.** Es un pipeline de herramientas estándar que llamas desde Java.

**Simple, efectivo, y estándar de la industria.** 🚀
