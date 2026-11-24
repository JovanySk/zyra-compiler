# REPORTE 1: CUÁDRUPLOS Y TRIPLOS - CÓDIGO INTERMEDIO

## 🎯 Código de Tres Direcciones (TAC)

### Definición

**Three-Address Code (TAC)** es una representación intermedia donde cada instrucción tiene como máximo **tres operandos**:
- Un operador
- Dos operandos fuente (máximo)
- Un destino

### Características

✅ **Independiente de la arquitectura** - Portable entre diferentes CPUs  
✅ **Cercanía al código máquina** - Fácil traducción a assembly  
✅ **Facilita optimización** - Estructura simple para análisis  
✅ **Temporales explícitos** - Variables intermedias visibles  

### Formato General

```
resultado = operando1 operador operando2
```

### Ejemplo

**Expresión:** `x = (a + b) * (c - d)`

**TAC:**
```
t1 = a + b
t2 = c - d
t3 = t1 * t2
x = t3
```

---

## 📦 CUÁDRUPLOS

### Estructura

Un **cuádruplo** representa una instrucción en 4 componentes:

```
(operador, arg1, arg2, resultado)
```

### Implementación en Zyra

```java
public class Cuadruplo {
    private String operador;  // Operación: +, -, *, /, =, <, goto, etc.
    private String arg1;      // Primer operando
    private String arg2;      // Segundo operando (puede ser "_" si vacío)
    private String resultado; // Variable destino o etiqueta
    
    public String toReadableString() {
        return String.format("(%s, %s, %s, %s)", 
            operador, 
            arg1.isEmpty() ? "_" : arg1,
            arg2.isEmpty() ? "_" : arg2,
            resultado.isEmpty() ? "_" : resultado);
    }
}
```

### Tipos de Cuádruplos

#### 1. Operaciones Aritméticas
```
(+, a, b, t0)      // t0 = a + b
(-, x, 5, t1)      // t1 = x - 5
(*, t0, t1, t2)    // t2 = t0 * t1
(/, y, 2, t3)      // t3 = y / 2
```

#### 2. Asignaciones
```
(=, 10, _, x)      // x = 10
(=, t0, _, y)      // y = t0
```

#### 3. Comparaciones
```
(<, a, b, t0)      // t0 = (a < b) ? 1 : 0
(>, x, 10, t1)     // t1 = (x > 10) ? 1 : 0
(==, i, n, t2)     // t2 = (i == n) ? 1 : 0
```

#### 4. Control de Flujo
```
(label, _, _, L0)          // L0:
(goto, _, _, L1)           // goto L1
(if_false, t0, _, L2)      // if (!t0) goto L2
(if_true, t0, _, L3)       // if (t0) goto L3
```

#### 5. Arrays
```
(new_array, int, 5, t0)    // t0 = malloc(5 * 8)
([]=, arr, 2, 10)          // arr[2] = 10
(=[], arr, i, t1)          // t1 = arr[i]
```

#### 6. I/O
```
(print, x, _, _)           // print(x)
(print_str, "Hola", _, _)  // print("Hola")
(read, _, _, t0)           // t0 = read()
```

---

## 🔷 TRIPLOS

### Estructura

Un **triplo** tiene solo **3 componentes**:

```
(número) (operador, arg1, arg2)
```

El **resultado es implícito** - se referencia por el número de triplo `(n)`.

### Implementación en Zyra

```java
public class Triplo {
    private int numero;       // Índice del triplo
    private String operador;  // Operación
    private String arg1;      // Primer argumento (puede ser (n))
    private String arg2;      // Segundo argumento (puede ser (n))
    
    public String getReferencia() {
        return "(" + numero + ")";  // Formato: (0), (1), (2)...
    }
    
    @Override
    public String toString() {
        return String.format("(%d) (%s, %s, %s)",
            numero, operador,
            arg1.isEmpty() ? "_" : arg1,
            arg2.isEmpty() ? "_" : arg2);
    }
}
```

### Referencias a Triplos Anteriores

Usar `(n)` para referenciar el resultado del triplo en posición `n`:

```
(0) (+, a, b)        // Resultado en (0)
(1) (*, (0), 2)      // Usa resultado de triplo 0
(2) (=, (1), x)      // x = resultado de triplo 1
```

### Ventajas y Desventajas

**Ventajas:**
- ✅ Compacto (menos memoria)
- ✅ Optimización directa

**Desventajas:**
- ❌ Difícil insertar/eliminar (renumerar referencias)
- ❌ Menos legible

---

## 🔄 COMPARACIÓN: Cuádruplos vs Triplos

### Mismo Código, Diferentes Representaciones

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
(1) (*, (0), 2)
(2) (-, (1), 1)
(3) (=, (2), x)
```

---

## 📊 Tabla Comparativa

| Característica | Cuádruplos | Triplos |
|---------------|-----------|---------|
| **Número de campos** | 4 | 3 |
| **Tamaño en memoria** | ~160 bytes | ~120 bytes |
| **Referencias** | Por nombre (`t0`) | Por posición `(0)` |
| **Legibilidad** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Inserción/Eliminación** | Fácil | Difícil |
| **Optimización** | Media | Alta |
| **Renumeración** | No necesaria | Necesaria |
| **Usado en generación ASM** | ✅ Sí | ❌ No |

---

## 🔧 GENERACIÓN SINCRONIZADA EN ZYRA

### Arquitectura

Zyra genera **ambas representaciones simultáneamente**:

```java
public class GeneradorCodigoIntermedio {
    private List<Cuadruplo> cuadruplos;
    private List<Triplo> triplos;
    private Map<String, Integer> mapaTempATriplo;
    
    public void emitir(String op, String arg1, String arg2, String res) {
        // 1. Crear cuádruplo
        Cuadruplo cuad = new Cuadruplo(op, arg1, arg2, res);
        cuadruplos.add(cuad);
        
        // 2. Convertir a triplo
        convertirATriplo(cuad);
    }
}
```

### Conversión Cuádruplo → Triplo

```java
private void convertirATriplo(Cuadruplo c) {
    // Mapear argumentos: si es temporal, usar referencia (n)
    String arg1T = mapearATriplo(c.getArg1());
    String arg2T = mapearATriplo(c.getArg2());
    
    // Crear triplo
    int num = triplos.size();
    Triplo triplo = new Triplo(num, c.getOperador(), arg1T, arg2T);
    triplos.add(triplo);
    
    // Si genera temporal, mapear a índice
    if (c.getResultado().startsWith("t")) {
        mapaTempATriplo.put(c.getResultado(), num);
    }
}

private String mapearATriplo(String arg) {
    // Si es temporal t0, t1... devolver (n)
    if (mapaTempATriplo.containsKey(arg)) {
        int num = mapaTempATriplo.get(arg);
        return "(" + num + ")";
    }
    return arg;  // Variable normal
}
```

---

## 💡 EJEMPLOS COMPLETOS

### Ejemplo 1: Expresión Aritmética

**Código:**
```java
int resultado = (a + b) * (c - d) / 2;
```

**Cuádruplos:**
```
(+, a, b, t0)
(-, c, d, t1)
(*, t0, t1, t2)
(/, t2, 2, t3)
(=, t3, _, resultado)
```

**Triplos:**
```
(0) (+, a, b)
(1) (-, c, d)
(2) (*, (0), (1))
(3) (/, (2), 2)
(4) (=, (3), resultado)
```

---

### Ejemplo 2: Bucle While

**Código:**
```java
int i = 0;
while (i < 5) {
    print(i);
    i = i + 1;
}
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
(3) (if_false, (2), L1)
(4) (print, i, _)
(5) (+, i, 1)
(6) (=, (5), i)
(7) (goto, L0, _)
(8) (label, L1, _)
```

---

### Ejemplo 3: Condicional If-Else

**Código:**
```java
if (x > 10) {
    y = x * 2;
} else {
    y = x / 2;
}
```

**Cuádruplos:**
```
(>, x, 10, t0)
(if_false, t0, _, L_else)
(*, x, 2, t1)
(=, t1, _, y)
(goto, _, _, L_end)
(label, _, _, L_else)
(/, x, 2, t2)
(=, t2, _, y)
(label, _, _, L_end)
```

**Triplos:**
```
(0) (>, x, 10)
(1) (if_false, (0), L_else)
(2) (*, x, 2)
(3) (=, (2), y)
(4) (goto, L_end, _)
(5) (label, L_else, _)
(6) (/, x, 2)
(7) (=, (6), y)
(8) (label, L_end, _)
```

---

### Ejemplo 4: Arrays

**Código:**
```java
int[] arr = new int[5];
arr[2] = 10;
int x = arr[2] + arr[3];
```

**Cuádruplos:**
```
(new_array, int, 5, t0)
(=, t0, _, arr)
([]=, arr, 2, 10)
(=[], arr, 2, t1)
(=[], arr, 3, t2)
(+, t1, t2, t3)
(=, t3, _, x)
```

**Triplos:**
```
(0) (new_array, int, 5)
(1) (=, (0), arr)
(2) ([]=, arr, 2, 10)
(3) (=[], arr, 2)
(4) (=[], arr, 3)
(5) (+, (3), (4))
(6) (=, (5), x)
```

---

## 🎓 CONCLUSIÓN

### ¿Por qué Zyra usa ambos?

**Cuádruplos:**
- Usados por el **generador de ensamblador**
- Fácil mapeo a instrucciones ASM
- Referencias explícitas evitan errores

**Triplos:**
- Visualización **compacta** en el IDE
- Representación académica estándar
- Menos espacio en memoria

### Pipeline Completo

```
Código Zyra → AST → Generador IR → {Cuádruplos, Triplos} → ASM
```

Ambas representaciones se generan automáticamente y se mantienen sincronizadas durante todo el proceso de compilación.
