# 16. Guía de Prueba End-to-End del MVP

Esta guía describe el camino mínimo para probar el MVP completo:
backend local sin Docker, Cognito Hosted UI, app Android y flujos
offline-first con QR.

> Estado de partida: el backend ya compila y levanta con el perfil
> `local` (H2 en memoria) y la app ya compila con tráfico HTTP habilitado
> en builds debug. Lo único que falta son los valores reales de Cognito.

---

## 1. Panorama

| Componente | Requisito para probar | Dónde se configura |
|---|---|---|
| Backend | JDK 21 | — |
| Backend | User Pool + App Client de Cognito | `fs2-api/.env` |
| Backend | Perfil `local` (H2) o PostgreSQL | `fs2-api/.env` |
| App | Backend accesible desde el emulador/dispositivo | `FormularioKotlin/.env` |
| App | Mismos datos de Cognito que el backend | `FormularioKotlin/.env` |
| App | Redirect URI registrado en Cognito | Consola Cognito |
| Usuarios | Grupos `docente` / `estudiante` | Consola Cognito |

Regla de oro: **el `COGNITO_CLIENT_ID` del backend y el de la app deben
ser el mismo App Client**, porque el backend valida el claim `client_id`
del access token.

---

## 2. Backend local (sin Docker ni PostgreSQL)

Ya está preparado:

- `build.gradle` incluye H2 en runtime.
- `src/main/resources/application-local.yml` usa H2 en memoria,
  `ddl-auto: create-drop` y Flyway deshabilitado.
- `application.yml` activa el perfil desde `SPRING_PROFILES_ACTIVE`.
- `.env` ya existe con `SPRING_PROFILES_ACTIVE=local`.

```powershell
cd C:\Users\admn\Downloads\fs2-api
.\gradlew.bat bootRun
```

Verificar que responde:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
# status = UP
```

Verificar que un endpoint protegido exige token:

```powershell
try { Invoke-WebRequest http://localhost:8080/api/auth/me } catch {
  $_.Exception.Response.StatusCode.value__   # 401
}
```

> ⚠️ H2 en memoria: **los datos se pierden cada vez que reinicias el
> backend**. Si necesitas persistencia entre reinicios, cambia en
> `application-local.yml` la URL a
> `jdbc:h2:file:./data/syncpass;AUTO_SERVER=TRUE` y `ddl-auto` a `update`.
> Para la prueba de humo de una sola sesión, la memoria es suficiente.

---

## 3. Configurar Cognito (una sola vez)

### 3.1 Crear el User Pool

En la consola de AWS → **Cognito** → **User pools** → **Create user pool**:

1. **Sign-in options:** `Email`.
2. **Password policy:** la que exija el curso (mínimo 8 caracteres).
3. **MFA:** deshabilitado para el MVP.
4. **User account recovery:** email.
5. Crear el pool.

Anota el **User Pool ID** (ej. `us-east-1_AB12CD34E`).

### 3.2 Crear el App Client

Dentro del pool → **App clients** → **Create app client**:

1. **Application type:** `Mobile application` (public client, sin secret).
2. **App client name:** `syncpass-mobile`.
3. **Authentication flows:** habilitar `ALLOW_USER_SRP_AUTH` y
   `ALLOW_REFRESH_TOKEN_AUTH` (el login real lo hace el Hosted UI).
4. **OAuth 2.0 grant types:** `Authorization code`.
5. **OpenID Connect scopes:** `openid`, `email`, `profile`, `phone`.
6. **Callback URL:**
   ```
   com.rojas.syncpass://cognito/callback
   ```
7. **Sign-out URL:**
   ```
   com.rojas.syncpass://cognito/callback
   ```
8. Crear y anotar el **Client ID**.

> No generes client secret: AppAuth en Android es un cliente público y
> usa PKCE. Un secret en el APK no aporta seguridad.

### 3.3 Configurar el dominio / Managed login

En el pool → **Branding** / **Domain**:

1. Crear un dominio Cognito (prefijo único) o usar uno propio.
2. Si la consola lo pide, crear un **Managed login style** y asignarlo
   al App Client.

El SDK descubre el dominio automáticamente a partir del issuer
(`/.well-known/openid-configuration`), por lo que no hay que ponerlo
en `.env`.

### 3.4 Crear los grupos

Pool → **Groups** → **Create group**:

| Grupo | Rol interno |
|---|---|
| `docente` | `DOCENTE` |
| `estudiante` | `ESTUDIANTE` |
| `admin` (opcional) | `ADMIN` + `DOCENTE` |

El backend normaliza además `profesor`, `teacher`, `coordinador`,
`alumno`, `student`, etc. Si el usuario no tiene grupo, el backend lo
trata como `ESTUDIANTE` por defecto.

### 3.5 Crear usuarios de prueba

Pool → **Users** → **Create user**:

| Usuario | Grupo | Uso |
|---|---|---|
| `docente@test.cl` | `docente` | Crear proyectos/eventos, escanear QR, ver asistentes |
| `estudiante@test.cl` | `estudiante` | Inscribirse, marcar asistencia, mostrar QR, responder encuesta |

Marca el email como verificado y define una contraseña permanente
(o deja que el Hosted UI la cambie en el primer login).

### 3.6 Volcar los valores en `fs2-api/.env`

```env
AWS_REGION=us-east-1
AWS_COGNITO_USER_POOL_ID=us-east-1_AB12CD34E
AWS_COGNITO_CLIENT_ID=1a2b3c4d5e6f7g8h9i0j
AWS_COGNITO_CLIENT_SECRET=
COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AB12CD34E
COGNITO_JWK_SET_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AB12CD34E/.well-known/jwks.json
```

> `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` solo son necesarios para
> los endpoints `/api/auth/login`, `/api/auth/registro` y recuperación de
> contraseña (usan la Admin API). El login por Hosted UI + `/api/auth/me`
> **no** los necesita: el backend solo valida la firma con el JWKS público.
> Déjalos vacíos para el MVP.

Reiniciar el backend para tomar los nuevos valores.

---

## 4. Configurar y compilar la app

Edita `FormularioKotlin/.env`:

```env
API_BASE_URL=http://10.0.2.2:8080/
COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AB12CD34E
COGNITO_CLIENT_ID=1a2b3c4d5e6f7g8h9i0j
COGNITO_REDIRECT_URI=com.rojas.syncpass://cognito/callback
COGNITO_LOGOUT_URI=com.rojas.syncpass://cognito/callback
COGNITO_SCOPES=email openid phone
```

| Escenario | `API_BASE_URL` |
|---|---|
| Emulador Android | `http://10.0.2.2:8080/` |
| Dispositivo físico en la misma red | `http://<IP-LAN-del-PC>:8080/` |
| Backend desplegado | `https://<host>/` |

Compilar e instalar:

```powershell
cd C:\Users\admn\Downloads\FormularioKotlin
.\gradlew.bat :app:assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> El `AndroidManifest.xml` ya declara el `intent-filter`
> `com.rojas.syncpass://cognito/callback`. Si cambias el redirect en
> `.env`, debes cambiar también el manifest y la consola de Cognito.
>
> El tráfico HTTP plano funciona **solo en debug**
> (`app/src/debug/AndroidManifest.xml`). Un build release exige HTTPS.

---

## 5. Prueba de humo (en orden)

### Paso 0 — Backend arriba

- [ ] `GET /api/health` → `200 UP`.
- [ ] `GET /api/auth/me` sin token → `401`.

### Paso 1 — Login del docente

1. Abrir la app → **Iniciar sesión con Cognito**.
2. Se abre el navegador con el Hosted UI.
3. Ingresar `docente@test.cl`.
4. Volver a la app.

Esperado:
- Home muestra "Docente / Coordinación".
- Botones **Nuevo proyecto** y **Nuevo evento**.
- Lista de eventos (vacía la primera vez).
- En el backend aparece el usuario auto-provisionado (`/api/auth/me`).

### Paso 2 — Crear datos

1. **Nuevo proyecto** → completar nombre y área temática → guardar.
2. **Nuevo evento** → elegir el proyecto, nombre, fecha/hora, recinto.
3. Verificar que el evento aparece en la lista.

### Paso 3 — Estudiante

1. Cerrar sesión en la app.
2. Login con `estudiante@test.cl`.

Esperado:
- Home muestra "Estudiante voluntario" y el botón **Mi QR**.
- Al abrir el evento: **Inscribirme en el evento**.
- Tras inscribirse: **Marcar mi asistencia**.

### Paso 4 — Asistencia y encuesta

1. Inscribirse en el evento.
2. **Marcar mi asistencia** (aceptar permiso de ubicación).
3. **Responder encuesta de satisfacción** (1–5 + comentario).

Esperado:
- Mensajes de guardado local.
- En Home el contador de pendientes sube si no hay red, o baja al
  sincronizar.

### Paso 5 — Docente ve los resultados

1. Cerrar sesión, login como docente.
2. Abrir el evento.

Esperado:
- **Lista de asistentes** con el estudiante como `Presente`.
- **Encuestas recibidas** con los puntajes.
- Contadores `Inscritos` / `Presentes` actualizados.

### Paso 6 — QR presencial (requiere 2 dispositivos/emuladores)

> El escaneo valida que el docente sea **responsable del proyecto** (quien
> lo creó) o `ADMIN`. Usa la misma cuenta docente que creó el proyecto.

1. Dispositivo A: estudiante → **Mi QR** (contenido `ESTUDIANTE_<id>`).
2. Dispositivo B: docente → evento → **Escanear QR de estudiante**.
3. Escanear el QR de A.

Esperado:
- Toast "Asistencia guardada en la cola local".
- El asistente aparece como presente al refrescar.
- Si no hay red, queda pendiente y se envía al sincronizar.

### Paso 7 — Offline-first

1. Activar modo avión.
2. Como estudiante: inscribirse / marcar asistencia / responder encuesta.
3. Volver a Home: debe indicar operaciones pendientes sin errores fatales.
4. Desactivar modo avión → **Sincronizar** (o esperar el worker periódico).

Esperado:
- Las operaciones pendientes se envían.
- El contador llega a 0 y los datos del docente se actualizan.

### Paso 8 — Separación de roles

- [ ] Estudiante **no** ve "Nuevo proyecto", "Nuevo evento" ni "Escanear QR".
- [ ] Docente **no** ve "Mi QR" ni "Inscribirme".
- [ ] Un estudiante que llama `GET /api/eventos/{id}/asistentes` recibe `403`.
- [ ] Un docente que llama `POST /api/sync/encuestas` recibe `403`.

---

## 6. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| La app muestra "Falta configuración de Cognito" | `COGNITO_ISSUER` o `COGNITO_CLIENT_ID` vacíos | Completar `.env` y **recompilar** (van a `BuildConfig`) |
| Se abre el navegador y vuelve con error `redirect_mismatch` | Callback distinto en Cognito | Debe ser exactamente `com.rojas.syncpass://cognito/callback` |
| Login parece funcionar pero vuelve al login | `intent-filter` no coincide con el redirect | Revisar manifest y `.env` |
| `401` con "Invalid issuer" en el backend | `COGNITO_ISSUER` no coincide con el pool | Copiar el issuer exacto (sin barra final) |
| `401` con "client_id del token no coincide" | App y backend usan App Clients distintos | Usar el mismo Client ID en ambos `.env` |
| `CLEARTEXT communication not permitted` | Build release o falta el overlay debug | Compilar `:app:assembleDebug` |
| Error de red en emulador con `localhost` | El emulador no ve el host como `localhost` | Usar `http://10.0.2.2:8080/` |
| `403` al escanear | Usuario sin grupo `docente`, o token viejo | Agregar al grupo y **volver a iniciar sesión** |
| `403` al ver asistentes | El usuario es `ESTUDIANTE` | Usar cuenta docente |
| La sesión caduca ~1 h | No hay refresh token automático | Volver a iniciar sesión (limitación conocida) |
| Los datos desaparecen al reiniciar el backend | H2 en memoria | Es esperado en perfil `local` |
| `Port 8080 was already in use` | Otro proceso usa el puerto | Cerrar el proceso o cambiar `SERVER_PORT` en `.env` |
| Los grupos no aparecen en el token | El usuario inició sesión antes de ser agregado al grupo | Cerrar sesión y volver a entrar |

---

## 7. Checklist final del MVP

### Infraestructura
- [ ] Backend levanta con perfil `local` (H2) o PostgreSQL.
- [ ] `/api/health` responde `UP`.
- [ ] User Pool, App Client, dominio y grupos creados.
- [ ] Callback y sign-out registrados en Cognito.

### Autenticación
- [ ] Login docente por Hosted UI + PKCE.
- [ ] Login estudiante por Hosted UI + PKCE.
- [ ] `/api/auth/me` devuelve el rol correcto.
- [ ] Logout limpia la sesión local.

### Flujos funcionales
- [ ] Docente crea proyecto.
- [ ] Docente crea evento.
- [ ] Estudiante se inscribe.
- [ ] Estudiante marca asistencia con GPS.
- [ ] Estudiante responde encuesta.
- [ ] Docente ve asistentes y encuestas.
- [ ] Docente escanea QR de estudiante.

### Offline-first
- [ ] Operaciones se encolan sin red.
- [ ] El worker o el botón Sincronizar envían la cola.
- [ ] La cola se vacía y los datos remotos quedan consistentes.
- [ ] Los `4xx` definitivos no se encolan en la operación inmediata.
- [ ] Si una operación ya encolada falla con `4xx`, el error queda visible y
      la fila permanece pendiente para revisión (no se descarta sola).

### Seguridad
- [ ] Endpoints protegidos devuelven `401` sin token.
- [ ] Roles restringen endpoints (`403` cuando corresponde).
- [ ] No hay secretos en el APK ni en archivos `.kt`.
- [ ] `.env` no está versionado.
