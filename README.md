# Drone_de_Inventario

# 🚀 DroneInventoryScanner - Roadmap V2.0 (Professional Edition)

Este documento define las especificaciones técnicas y funcionales para la **Fase 2** del desarrollo. El objetivo es evolucionar de la "Prueba de Concepto" (SPP básico) a una herramienta profesional de gestión de inventarios (WMS) adaptada a drones.

---

## 1. Gestión de Sesiones (Session Management)
Antes de iniciar el vuelo, el operario debe configurar el contexto del inventario para ordenar los datos.

* **Pantalla de Inicio (Lobby):**
    * **Input:** "Nombre del Cliente / Zona" (Ej: `CocaCola_Rack_B`).
    * **Selector:** "Perfil de Escaneo" (Ver sección 2).
* **Generación de Archivos:**
    * Los CSV se guardarán automáticamente con el formato: `[CLIENTE]_[TIMESTAMP].csv`.
    * *Ejemplo:* `CocaCola_Rack_B_20260119_1430.csv`.

---

## 2. Estrategia de Validación de Códigos (The "Bouncer")
Implementación de filtros por software para descartar lecturas erróneas o irrelevantes según el tipo de trabajo. Se utilizarán **Perfiles de Escaneo** seleccionables:

### 📦 Perfil A: Logística & Pallets (Altura)
*Uso:* Identificación de cajas master y pallets en racks altos.
* **Filtros Activos:**
    * **Code-128 (GS1-128):** Estándar logístico mundial (alfanumérico).
    * **ITF-14:** Código de barras "gordo" impreso en cartón corrugado (marco negro).
* **Regla de Validación:** Rechazar cadenas menores a 6 caracteres.

### 🛒 Perfil B: Retail & Picking (Producto Unitario)
*Uso:* Inventario de mercadería suelta o exhibición.
* **Filtros Activos:**
    * **EAN-13 / GTIN-13:** Estándar de consumo masivo.
    * **UPC-A:** Estándar americano.
    * **EAN-8:** Productos pequeños.

### 🚚 Perfil C: Transporte & Paquetería
*Uso:* Lectura de etiquetas de envío complejas (Shipping Labels).
* **Filtros Activos:**
    * **PDF417:** Código apilado de alta densidad.
* **Nota para el Piloto:** Requiere vuelo de alta precisión y alineación horizontal (yaw) debido a la naturaleza lineal del código.

### 📍 Perfil D: Ubicaciones (Racking)
*Uso:* Identificación de la posición física del rack (no del producto).
* **Filtros Activos:**
    * **QR Code:** Matriz 2D.
    * **Data Matrix:** (Opcional) Para componentes pequeños.

### 🔓 Perfil E: GOD MODE (Sin Filtros)
* Acepta cualquier cadena de texto que no sea vacía.
* *Uso:* Diagnóstico o formatos desconocidos.

---

## 3. Manejo de Duplicados con UI Flotante (Overlay)
Sistema de decisión en tiempo real sobre la app de vuelo (DJI Fly) mediante "Ventanas Flotantes" (System Alert Window).

* **Lógica de Detección:**
    * Si `Codigo_Nuevo == Ultimo_Codigo_Escaneado`: **DETENER GUARDADO AUTOMÁTICO**.
    * Disparar la UI Flotante.

* **Diseño de Overlay (Heads-up Display):**
    * Ventana semitransparente no intrusiva.
    * **Texto:** "DUPLICADO: [12345...]"
    * **Botón [VERDE] "AGREGAR":** Confirma que es una nueva unidad del mismo SKU. -> *Guarda y emite sonido positivo.*
    * **Botón [ROJO] "RECHAZAR":** Confirma que fue una re-lectura accidental. -> *Descarta y limpia el buffer.*
    * **Timeout:** Auto-rechazo a los 10 segundos si no hay acción.

---

## 4. Requisitos Técnicos Adicionales
* **Permisos:** Agregar `android.permission.SYSTEM_ALERT_WINDOW` al Manifest.
* **Sonidos:** Implementar feedback auditivo distintivo para:
    * *Scan Correcto* (Beep simple).
    * *Error de Formato* (Sonido grave/buzzer).
    * *Duplicado Detectado* (Doble tono).
