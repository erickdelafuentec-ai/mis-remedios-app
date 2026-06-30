# Mis Remedios — App nativa Android (Kotlin)

Recordatorio de medicamentos para adulto mayor. La alarma suena **a la hora exacta aunque el teléfono esté con la pantalla bloqueada y la app cerrada**, porque usa `AlarmManager.setAlarmClock()` — la misma API del reloj de Android, que atraviesa el modo de ahorro de energía (Doze).

## Características
- Letra grande, botones grandes, alto contraste.
- Pantalla de alarma a pantalla completa (se enciende sola, sobre la pantalla bloqueada).
- 5 sonidos a elegir + modo **Volumen extra fuerte** (suena al volumen de alarma, no de notificación).
- Vibración fuerte mientras suena.
- "Ya lo tomé" / "Recordar en 10 minutos".
- Se reprograma sola tras reiniciar el teléfono.
- Todo guardado en el teléfono, sin internet ni servidores.

---

## Cómo generar el APK en la nube (GitHub Actions)

No necesitas instalar Android Studio. GitHub compila el APK por ti.

### 1. Subir el proyecto a GitHub
- Crea un repositorio nuevo (ej. `mis-remedios-app`), público o privado.
- Sube **toda la carpeta** del proyecto (todos los archivos y subcarpetas).

### 2. Compilar
- Apenas subes a la rama `main`, el workflow se ejecuta solo.
- También puedes ir a la pestaña **Actions → Compilar APK → Run workflow**.
- Espera ~3-5 minutos a que termine (círculo verde ✓).

### 3. Descargar el APK
- Entra al workflow terminado → sección **Artifacts** (abajo).
- Descarga **MisRemedios-APK** (es un .zip que contiene el .apk).
- Descomprímelo: dentro está `app-release.apk`.

### 4. Instalar en el teléfono
- Pasa el APK al teléfono (WhatsApp, Drive, cable).
- Ábrelo. Android pedirá permitir **"Instalar apps desconocidas"** → acepta.
- Instala.

---

## Configuración en el teléfono (IMPORTANTE)

Para que suene siempre, una vez instalada:

1. Al abrirla, acepta el permiso de **notificaciones**.
2. Si aparece el aviso naranjo "Activar permiso", tócalo y permite **Alarmas y recordatorios**.
3. Ve a **Ajustes → Aplicaciones → Mis Remedios → Batería → "Sin restricciones"**.
4. En Samsung/Xiaomi/Motorola: quita la app de "apps en suspensión" / "ahorro de batería".

Usa el botón **Probar alarma** (engranaje arriba a la derecha) para verificar que suena bien.

---

## Notas técnicas
- `applicationId`: `cl.mersoftware.misremedios`
- minSdk 24 (Android 7) · targetSdk 34 (Android 14)
- El APK sale firmado con la *debug key* para que sea instalable directo. Para publicar en Play Store habría que firmarlo con una *release key* propia.

MER Software
