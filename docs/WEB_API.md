# API compartida por Android y web

La API conserva el cliente principal de Android (AWS_COGNITO_CLIENT_ID) para sus operaciones Cognito existentes. La validación de JWT admite una lista exacta y separada:

```dotenv
AWS_COGNITO_ALLOWED_CLIENT_IDS=3jgb0o4be28sb0cnqmjqihrfq4,4pp1hrlitnamn0pl2es7e8ejmn
CORS_ENABLED=true
CORS_ALLOWED_ORIGINS=http://localhost:5173
CORS_ALLOWED_HEADERS=Authorization,Content-Type
CORS_ALLOW_CREDENTIALS=false
```

Son identificadores públicos. No reemplazar el cliente de Android por el web. Si falta la nueva variable, la API sigue aceptando únicamente AWS_COGNITO_CLIENT_ID, conservando compatibilidad. Una allowlist vacía rechaza los tokens.

Se mantienen las validaciones de firma, issuer, expiración, token_use=access y los permisos de negocio. No se aceptan ID tokens ni coincidencias parciales de client_id.

El origen de Vercel se agregará explícitamente cuando exista el dominio definitivo, separado por coma. La configuración de CORS de Spring se usa a través de la integración HTTP de API Gateway; no duplicarla en Gateway sin revisar los encabezados que este reemplaza.

El CRUD mantiene sus endpoints y la propiedad del responsable. No se duplicaron servicios, controladores ni base de datos para la web.

## Despliegue de esta adaptación

EC2 utiliza ahora la Elastic IP 18.211.37.221, asociada a i-0813191c66146877d (172.31.17.177). La asignación eipalloc-0edbd389c910c7325 se conserva al detener e iniciar la instancia mientras no se libere. La integración ANY /api/{proxy+} de la API yw4llkw0lb apunta a https://18.211.37.221/api/{proxy}. Nginx usa un certificado para la nueva IP y el timer syncpass-certbot continúa activo. Las renovaciones de las IP anteriores quedaron desactivadas, conservando sus archivos.

El JAR se compiló localmente y se desplegaron incrementalmente las clases de SecurityConfig y CognitoAccessTokenValidator y application.yml. Se verificó que las dependencias del JAR instalado coincidieran antes de aplicar el cambio. Se preservaron backend.before-web.jar y backend.before-web.env como respaldo en sus directorios de EC2, sin publicar secretos.

En próximas actualizaciones puede desplegarse el JAR completo generado con bootJar. No se modificó el esquema ni se eliminaron datos de PostgreSQL.

La Elastic IP tiene cargos mientras permanezca reservada, incluso con EC2 detenida. No se necesita cambiar la URL de API Gateway usada por Android o web. La instancia debe estar activa y accesible para renovar su certificado de corta duración antes del vencimiento.
