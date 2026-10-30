# Ejecución y despliegue

## Requisitos del equipo

- Android Studio compatible con AGP 9.3.3, SDK 37, Platform Tools y SDK Command-line Tools.
- JDK 21 para el backend y entorno de comprobación Android. La JVM de Gradle debe ser compatible con el wrapper/AGP; no confundirla con el bytecode JVM 11 declarado por Android.
- Internet para descargar dependencias y autenticar con Cognito.
- Emulador o teléfono Android con depuración USB. minSdk declarado: 24; cámara opcional para instalar, necesaria para escanear.
- PostgreSQL para persistencia real del backend, o perfil local H2 para pruebas rápidas.

Los wrappers incluidos seleccionan Gradle 9.5.0 en Android y 9.8.2 en Backend. No es necesario instalar Gradle globalmente.

## Clonar y abrir

```powershell
git clone https://github.com/issa121212/SyncPass.git
cd SyncPass
git switch develop
```

Utilizar `develop` para el trabajo de integración y `main` para la entrega estable. Mantener el directorio de trabajo sin cambios locales incompatibles al cambiar de rama.

Abrir `Android/` desde Android Studio. Esperar Gradle Sync. Elegir la configuración compartida `SyncPass Local` (`Android/.run/SyncPass_Local.run.xml`) o una Android App sobre el módulo app y Default Activity. Si aparece `<no module>`, comprobar que se abrió Android y terminó la importación.

## Configuración Android

Crear `.env` desde `.env.example` cuando se quiera sobrescribir el ejemplo:

```powershell
Copy-Item Android/.env.example Android/.env
```

Hacerlo solo si no existe `.env`; no sobrescribir la configuración personal. El build lee `.env`, o el ejemplo si falta, con prioridad frente a las variables del sistema para las claves definidas. Los valores se incorporan a BuildConfig: cambiar la URL exige recompilar e instalar nuevamente.

| Variable | Finalidad |
|---|---|
| API_BASE_URL | URL base terminada en `/`; remoto HTTPS o backend local |
| COGNITO_ISSUER | Issuer completo del pool; no solo el poolId |
| COGNITO_CLIENT_ID | App Client público móvil, sin client secret |
| COGNITO_REDIRECT_URI | `com.rojas.syncpass://cognito/callback` |
| COGNITO_LOGOUT_URI | URL permitida de cierre de sesión, compatible con Android |
| COGNITO_SCOPES | Ejemplo actual: `openid`; ampliar solo con ámbitos habilitados |

Pool y client ID son identificadores públicos, no contraseñas. La app necesita el atributo name disponible para el saludo. No incrustar un client secret en Android. Si Cognito no está configurado, completar el entorno antes de probar login.

## Dispositivos y Run

Seleccionar un emulador o teléfono autorizado y pulsar Run. Para ambos, instalar/ejecutar en cada uno por separado. Cada dispositivo mantiene sus propios datos Room.

Con `API_BASE_URL` remoto, ambos acceden directamente a internet. No se necesita adb reverse ni levantar el backend local. `PREPARAR_RUN.ps1` utiliza la URL para decidir si prepara un servidor local; no se debe asumir que siempre inicia uno.

Para backend local:

- Emulador estándar: `http://10.0.2.2:8080/` apunta al PC anfitrión.
- Con adb reverse, tanto emulador como teléfono USB pueden usar `http://127.0.0.1:8080/`.
- Un teléfono por red LAN puede usar la IP del PC si el servidor escucha en la interfaz adecuada y el firewall lo permite.
- HTTP sin TLS solo se admite en debug; release utiliza HTTPS.

Diagnóstico en PowerShell, utilizando el serial real listado:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <serial-real> reverse tcp:8080 tcp:8080
```

El identificador literal `<serial-real>` se reemplaza. No utilizar emulator-5554 si no aparece conectado. Una desconexión USB o reinicio puede exigir repetir reverse. La tarea `prepareSyncPassLocal` usa el script raíz antes de Run y prepara los dispositivos disponibles cuando corresponde. No es necesario conservar scripts manuales adicionales.

## Backend local: carpeta fs2-api o Backend

En este checkout ejecutar desde `Backend/`. Si se trabaja desde una copia independiente llamada fs2-api, entrar a esa carpeta y comprobar primero que corresponde a la revisión del backend compartido.

```powershell
cd Backend
# Solo si no existe .env:
Copy-Item .env.example .env
.\gradlew.bat bootRun --console=plain
```

El ejemplo activa `SPRING_PROFILES_ACTIVE=local`. Utiliza H2 en memoria, no requiere Docker/PostgreSQL y pierde los datos al reiniciar. Cognito sigue siendo necesario para solicitudes autenticadas.

Para persistencia PostgreSQL, definir fuera de Git:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/syncpass
DATABASE_USER=<usuario>
DATABASE_PASSWORD=<secreto>
SERVER_PORT=8080
AWS_REGION=us-east-1
AWS_COGNITO_USER_POOL_ID=<poolId>
AWS_COGNITO_CLIENT_ID=<cliente-principal>
AWS_COGNITO_ALLOWED_CLIENT_IDS=<cliente-android>,<cliente-web>
COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/<poolId>
CORS_ENABLED=true
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://syncpassweb.vercel.app
CORS_ALLOW_CREDENTIALS=false
CORS_ALLOWED_HEADERS=Authorization,Content-Type
```

El perfil prod usa la configuración PostgreSQL por defecto; no existe un archivo application-prod.yml específico en el código revisado. Flyway migra y Hibernate valida. No activar H2 local para guardar información que se deba conservar.

## Despliegue existente en EC2

La instalación registrada usa Amazon Linux 2023, Java 21, Nginx y PostgreSQL local. EC2 ejecuta únicamente el backend y sus servicios, no la interfaz Android. El APK se ejecuta en el dispositivo.

Elementos operativos del despliegue:

- JAR: `/opt/syncpass/backend.jar`.
- Configuración privada: `/etc/syncpass/backend.env`.
- Servicio: `syncpass.service`, ejecutado con usuario de servicio.
- Aplicación: `127.0.0.1:8080`.
- PostgreSQL: `127.0.0.1:5432`.
- Entrada pública HTTPS: Nginx, puerto 443.
- Puerto 80 reservado para el desafío ACME configurado.
- IP elástica registrada: `18.211.37.221`.
- API Gateway HTTP: ruta `ANY /api/{proxy+}`, integración `https://18.211.37.221/api/{proxy}`, etapa `$default`.
- URL pública estable para los clientes: `https://yw4llkw0lb.execute-api.us-east-1.amazonaws.com/`.

La IP elástica evita cambiar la integración al detener/iniciar la misma instancia, mientras permanezca asignada. No garantiza disponibilidad si la instancia está apagada. No liberar la dirección sin planificar la actualización del certificado y la integración.

### Operación básica

Desde la sesión SSH de EC2, no desde PowerShell local:

```bash
sudo systemctl status syncpass --no-pager
sudo journalctl -u syncpass -n 80 --no-pager
sudo systemctl restart syncpass
sudo nginx -t
sudo systemctl status nginx --no-pager
```

Reiniciar después de un cambio válido en backend.env. Revisar que la base de datos y la aplicación terminen de arrancar: un estado systemd activo al primer instante no demuestra por sí solo que Spring esté listo.

Cerrar la terminal SSH no detiene el servicio systemd. Detener EC2 o el laboratorio sí interrumpe el acceso remoto. Respetar el presupuesto y apagar recursos cuando no sean necesarios, considerando los cargos que pueden continuar por almacenamiento/IP. No se incluye una promesa de despliegue gratuito.

### Actualización del backend

Generar el JAR con `bootJar`, transferirlo mediante un canal autorizado, conservar un respaldo, reemplazar el artefacto con permisos de servicio, reiniciar y comprobar health más una operación autenticada. El archivo generado está en `Backend/build/libs/`; su nombre se obtiene del build, no se presupone fijo en cada release. No copiar el código Android a EC2 para ejecutar la app móvil.

### Certificado y renovación

El despliegue utiliza un certificado de IP de corta duración. El directorio registrado es `/etc/letsencrypt/live/18.211.37.221/`; se configuró `syncpass-certbot.timer` y recarga de Nginx tras renovación. Revisar timer, vencimiento y resultado de renovación: no basta haber recibido un certificado una sola vez. Nunca publicar privkey.pem.

### CORS para la web compartida

Permitir orígenes exactos sin barra final, separados por coma. Mantener Authorization y Content-Type entre las cabeceras permitidas y métodos necesarios. Android nativo no depende del control CORS del navegador. No abrir todos los orígenes para resolver un problema de tokens. La aplicación web se despliega por separado en Vercel.

## Seguridad operativa

Mantener PostgreSQL y el puerto de aplicación sin acceso público directo en el diseño descrito. Restringir SSH, usar rol IAM cuando se necesite AWS SDK y evitar claves estáticas en archivos del repositorio. No compartir PEM ni contraseñas en capturas. Si una contraseña se expuso, rotarla y actualizar su configuración; escribir documentación no ejecuta esa rotación.

## Diagnóstico

- Service Unavailable: comprobar Lab, EC2, Nginx, Spring y base de datos antes de reinstalar Android.
- TLS: revisar fecha del certificado y renovación.
- 401: verificar issuer, expiración, access token y allowlist de clientes.
- 403: verificar rol/propiedad; en navegador comprobar también CORS.
- Falta configuración Cognito: revisar `.env`, issuer completo y recompilar.
- Módulo no especificado: abrir Android/ y completar Gradle Sync.
- Puerto local usado: reutilizar el servicio correcto o identificar el proceso; no terminar procesos desconocidos indiscriminadamente.
