# Cambios de seguridad (revisión de octubre 2026)

## Admin
- El admin se crea SOLO al arrancar, sin configurar nada: `admin@example.com` / `123456789` (como antes, por decisión del dueño).
- ADVERTENCIA: esa contraseña es pública y conocida. Para endurecerla más adelante, define `ADMIN_PASSWORD` (y opcionalmente
  `ADMIN_EMAIL`) y reinicia: si el admin ya existe, su contraseña se actualiza.
- Si el admin ya existe, no se modifica nada (salvo lo anterior).

## Control de acceso
- `SecurityConfig`: `/api/**`, `/report/**`, `/categorias/**`, Swagger y los listados genéricos son solo ADMIN.
  Siguen para cualquier usuario logueado: `/api/mensajes/**`, `/api/pagos/culqi-config`, `/api/pagos/{id}/procesar-culqi`.
- Se quitó `formLogin` (`/login_proc`); el login es `/auth/login` y `/tecnicos/login`.
- Se quitó `/uploads/**` de las rutas públicas.
- Se eliminó `/auth/register` (permitía elegir el rol al registrarse).
- `Usuario.password` ya no sale en JSON.

## CSRF (protección activa)
- CSRF queda ACTIVADO. Para que siga funcionando todo: `perfil.html` y `panel.html` envían el token en el chat y en la
  edición de perfil (función `csrfHeaders()`), y el botón "Cerrar Sesión" del dashboard admin ahora es un formulario POST.
- Si algún día quieres desactivarlo (no recomendado), en `SecurityConfig` cambia `.csrf(Customizer.withDefaults())`
  por `.csrf(csrf -> csrf.disable())`.

## Pagos
- `/api/pagos/{id}/procesar-culqi` y `/pagos/{id}/mp-iniciar`: solo el cliente dueño del pago (o admin).
- `/pagos/mp-retorno`: verifica dueño del pago, `external_reference`, monto y moneda (PEN) contra MercadoPago.
- `/api/pagos/{id}/pagar` y `POST /api/pagos` quedan solo para ADMIN.

## Archivos subidos
- Tipo validado por contenido (PDF / PNG / JPEG), nombre generado con UUID, protección contra rutas manipuladas.
- Certificados: solo los ve el admin o el técnico dueño.
- Fotos: se sirven solo png/jpg con `nosniff`.
- Al reemplazar un archivo se guarda el nuevo antes de borrar el anterior (y el borrado ahora sí funciona).

## Login
- Máximo 5 intentos fallidos por usuario+IP en 15 minutos (`LoginAttemptService`, en memoria).
- Se cambia el id de sesión al autenticar (anti session fixation).

## RENIEC
- El token se enmascara en logs y mensajes; tiempos de espera de 5 s / 8 s.

## .gitignore
- Se agregaron `.env`, `application-*.properties`, `uploads/`, `logs/`, llaves y certificados.

## Pendiente / no verificado
- No se pudo compilar en el entorno de revisión (sin acceso a Maven Central): ejecuta `mvn clean package` antes de desplegar.
- Rotar las claves que aparecieron en capturas y en el código (Aiven, Gmail, RENIEC).
- Los archivos de `uploads/` se pierden en cada redeploy si no se monta un volumen o almacenamiento externo.
