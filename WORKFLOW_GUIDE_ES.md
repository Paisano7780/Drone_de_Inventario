# Guía de Compilación Automática - DroneInventoryScanner

Esta guía explica cómo obtener el APK compilado usando los workflows automatizados de GitHub Actions.

## ⭐ IMPORTANTE: Compilación Automática tras Merge al Main

**Cuando apruebes el Pull Request y se haga merge a la rama `main`:**

✅ El workflow se ejecutará **automáticamente**
✅ Los tests unitarios se ejecutarán primero
✅ Se compilará el APK
✅ El APK se **guardará automáticamente** en la carpeta `release/` del repositorio
✅ Se creará un **Release automático** con número de versión (ej: `v1.0`)
✅ El APK estará disponible en **tres lugares**: carpeta `release/`, sección **Releases**, y **Artifacts**

**Para descargar el APK después del merge:**

**Opción más fácil - Carpeta release/:**
1. Ve a la carpeta `release/` en el repositorio
2. Descarga directamente `DroneInventoryScanner-v1.0-debug.apk`
3. ¡Listo! Ya tienes tu APK

**Opción alternativa - Releases:**
1. Ve a la pestaña **Releases** (lado derecho de la página principal)
2. Haz clic en el release más reciente (ej: `v1.0`)
3. Descarga `DroneInventoryScanner-v1.0-debug.apk`

**Versionado automático:**
- Primera versión: `v1.0`
- Versiones siguientes: Se incrementa automáticamente (`v1.1`, `v1.2`, etc.)

## 📁 Opción 1: Descargar APK desde la Carpeta Release ⭐ MÁS FÁCIL

**La forma más rápida y directa:**

### Paso 1: Ir a la carpeta release/
1. Abre el repositorio en GitHub
2. Haz clic en la carpeta **release/** en la página principal
3. Verás los APKs compilados allí

### Paso 2: Descargar el APK
4. Haz clic en el APK más reciente (ej: `DroneInventoryScanner-v1.0-debug.apk`)
5. Haz clic en el botón **Download** (Descargar)
6. ¡El APK se descargará directamente a tu computadora!

**Ventajas:**
- ✅ Acceso directo sin pasos adicionales
- ✅ No necesitas navegar por Actions o Releases
- ✅ El APK está versionado y listo para usar
- ✅ Actualizado automáticamente con cada merge a main

## 🚀 Opción 2: Descargar APK desde Releases

### Paso 1: Ir a Releases
1. Abre el repositorio en GitHub
2. En el lado derecho, verás la sección **Releases**
3. Haz clic en **Releases** o en el número de la última versión

### Paso 2: Descargar el APK
4. Verás el release más reciente (ej: `DroneInventoryScanner v1.0`)
5. En la sección **Assets**, haz clic en `DroneInventoryScanner-v1.0-debug.apk`
6. ¡El APK se descargará directamente!

## 📦 Opción 3: Descargar APK desde Artifacts

### Paso 1: Ir a la pestaña Actions
1. Abre el repositorio en GitHub
2. Haz clic en la pestaña **Actions** (entre Pull requests y Projects)

### Paso 2: Seleccionar el workflow
3. En el lado izquierdo, verás "Build APK"
4. Haz clic en "Build APK"
5. Verás una lista de ejecuciones del workflow

### Paso 3: Descargar el APK
6. Haz clic en la ejecución más reciente (la de arriba)
7. Desplázate hacia abajo hasta la sección **Artifacts**
8. Haz clic en **app-debug-v1.0** para descargar
9. Descomprime el archivo ZIP
10. ¡Ya tienes tu APK!

## 🔄 Opción 4: Ejecutar un Nuevo Build Manualmente

### Si quieres compilar una nueva versión manualmente:

1. Ve a **Actions** → **Build APK**
2. Haz clic en **Run workflow** (botón azul a la derecha)
3. Selecciona la rama
4. Haz clic en **Run workflow**
5. Espera 3-5 minutos mientras se compila
6. Descarga desde la carpeta release/, Artifacts o Releases

## 🎁 Opción 5: Crear un Release con Versión Personalizada

### Para crear una versión con número específico:

1. Ve a **Actions** → **Build Release APK**
2. Haz clic en **Run workflow**
3. Ingresa el número de versión (ejemplo: `2.0.0`)
4. Haz clic en **Run workflow**
5. Espera a que termine
6. Ve a la pestaña **Releases**
7. Verás el release con el APK adjunto
8. Descarga el APK directamente desde ahí

## 📱 Instalación en tu Dispositivo

### Una vez que tengas el APK:

1. **Transferir a tu teléfono:**
   - Por USB: Copia el archivo APK a tu teléfono
   - Por correo: Envíate el APK por email y ábrelo desde el teléfono
   - Por Google Drive: Sube el APK y descárgalo en el teléfono

2. **Habilitar instalación:**
   - Ve a **Configuración** → **Seguridad**
   - Activa **Fuentes desconocidas** o **Instalar apps desconocidas**

3. **Instalar:**
   - Abre el archivo APK desde tu teléfono
   - Toca **Instalar**
   - Espera a que termine
   - Toca **Abrir** para probar la app

## ✅ Verificación

### El workflow automático hace:
- ✅ Ejecuta los 20 tests unitarios
- ✅ Compila el APK
- ✅ Verifica que no hay errores
- ✅ Sube el APK como artifact

### Si el workflow falla:
- Revisa los logs en la pestaña Actions
- El workflow mostrará exactamente dónde falló
- Los tests deben pasar antes de compilar

## 📊 Información Técnica

**Entorno de compilación:**
- Sistema: Ubuntu (última versión)
- Java: JDK 17
- Gradle: Wrapper del proyecto
- Tiempo: 3-5 minutos

**APK generado:**
- Ubicación: `app/build/outputs/apk/debug/app-debug.apk`
- Tipo: Debug (sin firmar)
- Tamaño: ~2-5 MB
- Retención: 30 días (builds normales), 90 días (releases)

## 🔧 Compilación Local (Alternativa)

Si prefieres compilar en tu computadora:

```bash
# Clonar repositorio
git clone https://github.com/Paisano7780/Drone_de_Inventario.git
cd Drone_de_Inventario

# Compilar APK
./gradlew assembleDebug

# APK estará en: app/build/outputs/apk/debug/app-debug.apk
```

**Requisitos para compilación local:**
- Android Studio
- Android SDK 33
- Java 11+

## 📝 Notas Importantes

- El workflow se ejecuta automáticamente en cada push
- Los APKs están disponibles solo para usuarios con acceso al repositorio
- Para APKs firmados (producción), se necesita configurar keystore
- Los artifacts se borran después de 30-90 días

## 🆘 Solución de Problemas

**"No veo la pestaña Actions":**
- Asegúrate de tener permisos en el repositorio
- El repositorio debe ser tuyo o debes ser colaborador

**"El workflow no se ejecuta":**
- Verifica que los archivos estén en `.github/workflows/`
- Revisa que el archivo YAML sea válido

**"El APK no instala":**
- Verifica que hayas habilitado "Fuentes desconocidas"
- Asegúrate de que el archivo se descargó completamente

**"La app no funciona":**
- Otorga permisos de Bluetooth
- Otorga permisos de Notificaciones
- Empareja primero el escáner en Configuración Bluetooth

## 📞 Ayuda Adicional

Para más información, consulta:
- `README.md` - Guía completa del proyecto
- `BUILD_INSTRUCTIONS.md` - Instrucciones detalladas de compilación
- `.github/workflows/README.md` - Documentación técnica de workflows

---

**¡Listo!** Ahora tienes tres formas de obtener el APK sin necesidad de compilar localmente. 🎉
