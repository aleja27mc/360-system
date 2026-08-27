# Vista 360° del Estudiante — Prueba Técnica

Este repo contiene la Parte 2 de la prueba (el servicio) más las respuestas de las Partes 3 y 4.
El diseño de arquitectura de la Parte 1 (diagrama + documento) lo entregué aparte.

## Uso de IA

Usé Claude (Anthropic, con Claude Code) durante toda la prueba, más o menos así:

- Para el diseño, me ayudó a estructurar y redactar el documento de arquitectura, y también
  pedí una revisión cruzada del plan antes de ponerme a codear — un segundo agente, mismo
  modelo, haciendo de evaluador independiente para pescar huecos de seguridad o cosas
  inconsistentes antes de que quedaran metidas en el código.
- Para la implementación, generó el código de Spring Boot, el esquema SQL y las pruebas,
  siguiendo el contrato que yo ya había definido en el diseño. Las decisiones de arquitectura,
  el modelo de datos y qué simplificar (y por qué) las tomé yo; están declaradas explícitas
  más abajo.
- Para las Partes 3 y 4, usé la IA para redactar las respuestas sobre decisiones que ya había
  tomado yo.

Antes de dar esto por cerrado corrí y probé todo de punta a punta (ver "Cómo probarlo" más
abajo), incluyendo un par de rondas donde encontré bugs reales revisando el código con más
calma y los corregí.

---

## Parte 1 — Arquitectura (resumen)

El detalle completo está en el documento aparte, pero para que este repo tenga contexto
propio, dejo acá lo esencial.

### Los supuestos que hice

El caso es deliberadamente abierto, así que tuve que decidir varias cosas:

1. Vista 360° es una capa de agregación más datos propios, no un ERP paralelo. Lo académico,
   lo financiero y el LMS se leen en vivo desde los sistemas fuente (sin duplicar nada, salvo
   quizá un cache de pocos segundos); los reportes de acompañamiento, alertas y solicitudes sí
   son datos nuevos de Vista 360°, porque no existen en ningún sistema previo.
2. Asumí que el ecosistema tiene un Sistema Académico (datos personales, matrícula, notas,
   condición académica), un Sistema Financiero (saldos, becas), un LMS (actividad de campus
   virtual) y un Data Warehouse que solo consume, nunca es fuente.
3. Doy por hecho que los sistemas fuente exponen o pueden exponer API REST/SOAP. Si alguno no
   puede, dejo como salida de emergencia leer directo de una vista SQL, pero aclaro que es una
   excepción, no algo equivalente a integrarse por API.
4. Si el enunciado pide el estado financiero "de inmediato", entiendo que eso implica consulta
   síncrona sin cache (o con TTL de segundos) — ahí la exactitud pesa más que la latencia.
5. Un cambio de condición académica es un evento de negocio con varios consumidores que no
   necesitan enterarse al mismo tiempo, así que lo resuelvo con un bus de eventos, no con
   polling.
6. Asumo que la universidad ya tiene un IdP institucional (algo tipo SSO/OIDC) y que Vista 360°
   se federa contra él en vez de inventar su propio sistema de usuarios.
7. La asignación acompañante-estudiante es información nueva, propia de Vista 360°.

### Cómo se comunican los componentes

```
Frontend (SPA)
   │ HTTPS + JWT
   ▼
BFF / API Gateway (Spring Boot)
 - valida JWT, autoriza por rol
 - agrega las respuestas de cada sección de la vista; si una falla, esa sección degrada sola
 - propaga X-Correlation-Id
   │ REST interno síncrono (timeout corto + circuit breaker)
   ├──► Adaptador Académico ──► Sistema Académico (REST/SOAP/vista SQL)
   ├──► Adaptador Financiero ──► Sistema Financiero
   ├──► Adaptador LMS ──► LMS
   └──► Servicio de Acompañamiento (CRUD propio) ──► Oracle propia
              (reportes, alertas, solicitudes, asignaciones)

Sistema Académico ──evento CambioCondicionAcademica (eventId+timestamp)──► Bus (Kafka)
                                                                                    │
                                              ┌─────────────────────────────────────┤
                                              ▼                                     ▼
                                   Motor de alertas tempranas              Pipeline ETL → Data Warehouse
                                   (acompañamiento)                        (+ batch/CDC nocturno para históricos)
```

Algunas decisiones que tomé y por qué:

Puse un solo BFF como entrada del frontend para no tener que exponer la topología interna y
para centralizar autenticación/autorización y logging en un solo lugar. Cada sección de la
vista (académico, financiero, LMS) degrada por separado — si el LMS está caído, no tiene
sentido que el estudiante se quede sin ver ni siquiera sus notas.

Cada sistema fuente tiene su propio adaptador en vez de meter todo en un integrador
gigante, porque así el ciclo de cambios de cada sistema legado queda aislado del resto.

Las consultas son síncronas porque los sistemas fuente siguen siendo la fuente de verdad; no
los replico porque eso metería el riesgo de mostrar información desactualizada.

Los datos de acompañamiento viven en una base propia simplemente porque no existen en ningún
otro lado.

El bus de eventos lo uso solo para eventos de negocio de verdad (cambio de condición, alertas),
no para todo — meter eventos en cada interacción hubiera sido sobre-ingeniería. Los eventos sí
llevan `eventId` y `timestamp` para que los consumidores (alertas, DW) puedan procesarlos de
forma idempotente y en orden.

El Data Warehouse se alimenta de dos maneras: con los mismos eventos del bus para lo que es
casi tiempo real, y con un batch/CDC nocturno para lo histórico — reutilizando el bus que ya
existe en vez de armar una pipeline aparte.

Ojo con un punto: usar CDC como respaldo para publicar eventos es un supuesto de bastante
riesgo, porque en la práctica pocas universidades dan acceso a los redo logs de su ERP. Lo dejo
anotado como algo que habría que validar con el equipo dueño del sistema académico antes de
darlo por sentado.

### De dónde sale cada dato

| Dato | Origen | Por qué |
|---|---|---|
| Datos personales, matrícula, notas, condición académica | Sistema Académico | Es la fuente única de verdad académica |
| Estado financiero / saldos | Sistema Financiero | Es la fuente única de verdad financiera |
| Actividad en campus virtual | LMS | Es el único sistema que registra eso |
| Reportes, alertas, solicitudes de acompañamiento; asignación acompañante-estudiante | BD propia Vista 360° | Es información nueva, no existe en el ecosistema previo |
| Modelos analíticos | Data Warehouse | Solo consume, no genera nada |

---

## Parte 2 — Servicio implementado

Antes de entrar al detalle: este servicio es la implementación de referencia del "Adaptador
Académico" que mencioné en la Parte 1. Expone el mismo contrato REST que el BFF consumiría en
producción, solo que aquí tiene su propio backing store en vez de proxyar un ERP real — así
puede correr de punta a punta sin depender de infraestructura externa. En un escenario real,
`enrollment` y `grade` serían tablas del ERP académico, no de este servicio.

### Contrato

```
GET /api/v1/students/{studentId}/courses
Authorization: Bearer <JWT>
```

Dado el id de un estudiante, devuelve sus materias matriculadas en el periodo actual y las
notas registradas para esas matrículas:

```json
{
  "studentId": "E001",
  "enrolledCourses": [
    { "courseCode": "MAT101", "courseName": "Calculo I", "groupCode": "01",
      "term": "2026-2", "credits": 3, "status": "ENROLLED" }
  ],
  "currentGrades": [
    { "courseCode": "MAT101", "term": "2026-2", "assessment": "MIDTERM_1",
      "score": 4.2, "recordedDate": "2026-08-20" }
  ]
}
```

Códigos de respuesta:

- `200` — incluso si el estudiante no tiene matrículas en el periodo actual (devuelve arrays
  vacíos, no es un error).
- `400` — si `studentId` no tiene el formato esperado (alfanumérico, máximo 20 caracteres,
  igual que la columna de la tabla). Se valida en el controlador antes de tocar la base.
- `403` — si quien llama no tiene permiso sobre ese `studentId`. Un `STUDENT` solo puede pedir
  su propio id; un `ADVISOR` solo puede pedir estudiantes que tenga asignados, y eso se checkea
  contra la tabla `advisor_assignment`, nunca contra algo que venga en el token.
- `404` — si el `studentId` directamente no existe.
- `500` — como respaldo genérico para cualquier excepción que no haya previsto, sin exponer
  detalle interno al cliente.

Todo esto queda documentado en OpenAPI/Swagger (ver más abajo cómo levantarlo).

### Modelo de datos

```
student(student_id PK, full_name, document_id)
course(course_code PK, course_name, credits)
enrollment(enrollment_id PK, student_id FK, course_code FK, term, group_code, status)
grade(grade_id PK, enrollment_id FK, assessment, score, recorded_date)
advisor_assignment(advisor_id, student_id FK)         -- dato nuevo de negocio
audit_log(audit_id PK, actor_id, actor_role, student_id, action, result, correlation_id, occurred_at)
```

El `term` lo dejé como un `VARCHAR` suelto, sin una entidad propia de periodo académico —
simplificación consciente para el alcance de esta prueba.

### Cómo quedó la seguridad de este servicio puntualmente

Usa JWT firmado con HS256, con claims `role` (`STUDENT` o `ADVISOR`) y `studentId` (solo
cuando el rol es `STUDENT`). Aclaro que esto es una simulación: en un caso real este servicio
no firmaría tokens, solo validaría los que emite el IdP institucional contra su JWKS. El
secreto compartido que hay acá es únicamente para poder correr la prueba de punta a punta sin
depender de un IdP de verdad.

La autorización se revalida en el propio servicio, no solo en la puerta de entrada: un
`ADVISOR` nunca lleva en el token la lista de estudiantes que tiene asignados, siempre se
consulta `advisor_assignment` en cada request. Si no fuera así, un cambio de asignación a
mitad de sesión quedaría "congelado" en un token que ya se emitió.

Cada consulta —permitida, denegada por falta de autorización, o fallida porque el `studentId`
no existe— se registra en `audit_log`, junto con el `correlation_id` de esa request. Esto es
justo lo que permite responder con confianza a un reclamo de acceso indebido más adelante (ver
Parte 4B). Ese registro corre en su propia transacción, separada de la transacción de solo
lectura que usa la consulta, para que quede guardado aunque la operación termine lanzando una
excepción — de hecho, ese fue uno de los bugs que encontré y corregí (más abajo cuento el
detalle).

También hay un `X-Correlation-Id` que viaja de punta a punta, propagado o generado por un
filtro y disponible en cada línea de log. Esto es lo que hace posible diagnosticar fallas
intermitentes (Parte 4A).

### Stack

Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, H2 en modo compatibilidad Oracle,
springdoc-openapi y JJWT. Usé Gradle en vez de Maven porque era lo único que tenía disponible y
pude verificar en esta máquina — no cambia ninguna decisión de diseño, es solo la herramienta
de build.

### Cómo correrlo

```bash
./gradlew.bat bootRun      # Windows
./gradlew bootRun          # Linux/Mac
```

Levanta en `http://localhost:8080`. La base H2 en memoria se pobla sola al arrancar
(`schema.sql` + `data.sql`) con 2 estudiantes, 3 materias, algunas matrículas y notas de
ejemplo, y una asignación de acompañamiento (`A001` → `E001`).

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Consola H2: `http://localhost:8080/h2-console` (la consola ya trae precargada la JDBC URL
  correcta; usuario `sa`, sin contraseña)

### Tests

```bash
./gradlew.bat test      # Windows
./gradlew test          # Linux/Mac
```

Hay 3 clases de test, todas contra un contexto Spring Boot real levantado en un puerto
aleatorio (nada de mocks):

- **`StudentControllerAuthorizationIT`** cubre toda la matriz de autorización: estudiante
  consultando lo suyo (200), estudiante consultando a otro (403), acompañante con estudiante
  asignado (200) y sin asignar (403), estudiante que no existe (404), sin token (403),
  `studentId` con formato inválido (400), y un estudiante con matrículas pero todavía sin
  notas (200, con `currentGrades` vacío).
- **`AuditLoggingIT`** verifica que cada lectura, permitida o denegada, realmente quede
  guardada en `audit_log` — este test fue el que me ayudó a encontrar el bug que menciono
  abajo.
- **`GenerateTestTokensTest`** no es un test de negocio, es una utilidad que imprime tokens de
  prueba para poder probar manualmente con curl o Swagger.

Cada clase de test usa una base H2 con nombre único (le agregué `${random.uuid}` a la URL en
`application.yml`) para que distintos contextos de Spring no se pisen entre sí tratando de
insertar los mismos datos semilla.

### Un par de cosas que se me escaparon la primera vez

Vale la pena dejarlo anotado porque muestra cómo probé esto, no solo que lo escribí:

- El registro de auditoría no se estaba guardando. El método de consulta corre en una
  transacción de solo lectura, y Hibernate nunca hace flush de un INSERT dentro de una
  transacción así — el código se veía bien pero en la práctica no persistía nada. Lo detecté
  con un test de integración y lo arreglé moviendo ese registro a su propia transacción
  independiente.
- Las clases de test competían por la misma base H2 en memoria, así que la segunda que
  arrancaba intentaba reinsertar datos que ya existían y fallaba. Se resolvió dándole a cada
  contexto de Spring un nombre de base único.

### Generar tokens de prueba

```bash
./gradlew.bat test --tests "*GenerateTestTokensTest*"
```

Esto imprime en consola tokens firmados con el mismo secreto que usa la app, para poder probar
a mano con curl o desde Swagger:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/students/E001/courses
```

---

## Parte 3 — Seguridad y comunicación

### 3.1 Seguridad

La autenticación la resolvería con el IdP institucional (OAuth2/OIDC, flujo de Authorization
Code): el frontend nunca toca credenciales, solo reenvía el JWT como `Bearer` en cada llamada.

La autorización la pienso en dos capas. En el borde, el BFF valida que el JWT sea válido y mira
el rol (`STUDENT` o `ADVISOR`) antes de dejar pasar la request. Pero no me quedo solo con eso:
cada microservicio backend revalida el acceso al recurso puntual que se está pidiendo, sin
confiar ciegamente en que el gateway ya hizo bien su trabajo. Un `STUDENT` solo puede tocar su
propio `studentId`; un `ADVISOR` solo los estudiantes que tenga en la tabla de asignación, y
esa tabla se consulta en cada request — nunca la meto como claim del token, porque las
asignaciones cambian por su cuenta (una reasignación a mitad de semestre, por ejemplo) y no
quiero que un token viejo quede desincronizado de la realidad.

Para las llamadas entre servicios, propago el mismo JWT en vez de montar mTLS con tokens de
servicio separados — para el alcance de este caso eso sería más complejidad de la que vale la
pena. Lo único que sí agregaría es un claim tipo `aud` por servicio destino, para poder
distinguir en logs y en auditoría qué servicio hizo una llamada interna, no solo qué usuario la
originó — útil si algún adaptador se ve comprometido o tiene un bug de logging.

El JWT debería tener una vida corta (minutos u horas, no días) con refresh, para que si a un
acompañante le quitan una asignación a mitad de sesión, eso se refleje pronto sin depender de
un mecanismo de revocación activa.

Y algo que no quiero dejar fuera aunque no esté implementado en el código de la Parte 2: los
datos sensibles (notas, estado financiero, alertas de acompañamiento) deberían cifrarse también
en reposo, no solo en tránsito con TLS — es información de un sector regulado.

### 3.2 Comunicación

**Escenario A, el estado financiero inmediato.** Lo resolvería con una consulta síncrona del
BFF al Adaptador Financiero, que a su vez llama en tiempo real al Sistema Financiero, sin
cache (o con un TTL de pocos segundos si acaso). La razón es simple: el estudiante necesita ese
dato exacto en ese momento porque puede condicionar una matrícula o un grado, y cachearlo por
más tiempo mete el riesgo de mostrar un saldo viejo justo en una decisión que importa. Sí, se
paga el costo de la latencia del sistema fuente, pero el timeout y el circuit breaker del
adaptador evitan que una caída del Financiero se lleve puesta toda la vista — esa sección en
particular degrada con un mensaje claro y el resto sigue funcionando.

**Escenario B, el cambio de condición académica.** Aquí el Sistema Académico publica un evento
(`AcademicStatusChanged`, con `eventId`, `studentId`, el nuevo estado y un `timestamp`) a un
bus tipo Kafka. De ahí lo consumen dos procesos que no dependen uno del otro: el motor de
alertas de acompañamiento, que dispara la intervención temprana casi al instante (de forma
idempotente por `eventId`, para no duplicar alertas si el evento llega repetido), y un pipeline
hacia el Data Warehouse que registra el cambio como hecho histórico para analítica. La idea de
fondo es que ninguno de los dos necesita respuesta síncrona ni debería bloquear al Sistema
Académico — publicar/suscribir evita hacer polling y deja que cada consumidor sea independiente
de la disponibilidad del otro.

---

## Parte 4 — Operación y calidad

### Escenario A — la carga académica falla de forma intermitente

Lo primero que necesitaría desde el diseño para poder investigar esto en serio es
trazabilidad de punta a punta: un `X-Correlation-Id` que nazca en el borde (o que venga
propagado desde el frontend) y que viaje por cada log del BFF, cada adaptador y cada servicio
backend. Esto ya está sembrado en este repo con el `CorrelationIdFilter`. Sin eso, un fallo
intermitente es imposible de diagnosticar: no hay forma de saber si fue un timeout del
adaptador, un circuit breaker que se abrió, o un error real del sistema fuente, y mucho menos
de conectar "esta queja de un director de centro de apoyo" con "esta línea de error en los
logs".

Cómo encararía el incidente en la práctica:

1. Pedir el `X-Correlation-Id` (o al menos el timestamp exacto) del reporte, y rastrearlo por
   los logs desde el BFF hasta el sistema fuente para esa request puntual.
2. Revisar las métricas del circuit breaker del Adaptador Académico en esa ventana de tiempo —
   la palabra "intermitente" casi siempre apunta a saturación puntual del sistema fuente o a un
   pool de conexiones agotado, no a un bug que se repite siempre igual.
3. Ver si el patrón se concentra en ciertos estudiantes (algo raro en su dato, o caracteres que
   rompen el adaptador) o en cierta franja horaria (carga pico del ERP, un batch nocturno que
   se solapa con el tráfico normal).

Más allá de lo que ya hay en este repo, lo que realmente haría falta tener desde el diseño son
métricas expuestas (latencia p95/p99 y tasa de error por adaptador, algo tipo Micrometer +
`/actuator`), alertas cuando el circuit breaker se abre, y logs en formato estructurado para
poder filtrar por `correlationId` y por `studentId` en un stack de observabilidad centralizado.

### Escenario B — un estudiante reclama que consultaron o alteraron su información

Esto es justo lo que la tabla `audit_log` de este servicio ya resuelve. Cada lectura de datos
de un estudiante que pasa por el sistema de permisos —no solo las escrituras— queda registrada
con quién hizo la consulta, a quién consultó, cuándo, y si el resultado fue permitido, denegado
por falta de autorización, o "no encontrado" porque el id ni siquiera existía. Todo eso
enlazado por `correlation_id` a los logs de esa misma request.

Con eso, ante un reclamo la institución puede responder con algo concreto en vez de "no
sabemos":

1. Filtrar `audit_log` por el `student_id` del reclamante, en la ventana de tiempo que
   indique.
2. Ver exactamente quién accedió o intentó acceder — un intento denegado también cuenta como
   evidencia de que el sistema bloqueó algo que no debía pasar.
3. Si aparece un acceso permitido que resulta sospechoso (por ejemplo, un acompañante que ya no
   debería tener esa asignación), cruzarlo contra el historial de `advisor_assignment` para
   confirmar si el acceso ocurrió mientras la asignación seguía vigente.

Lo que le agregaría a esto desde el diseño, más allá de lo implementado: que `audit_log` sea
append-only de verdad, sin permisos de `UPDATE` ni `DELETE` a nivel de base de datos ni siquiera
para quien administra la aplicación, y replicar esos registros a un almacenamiento externo e
inmutable (algo con retención WORM), para que ni un compromiso del propio servicio pueda borrar
su rastro.
