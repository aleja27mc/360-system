# Vista 360° del Estudiante: Parte 1, Diseño de la solución

El diagrama completo está en `diagrama-arquitectura.svg`. Aquí explico las decisiones y los
supuestos detrás de ese diagrama.

## Los supuestos:

1. Vista 360° la pienso como una capa de agregación más datos propios, no como un ERP paralelo.
   Lo académico, lo financiero y el LMS se leen en vivo desde los sistemas fuente (sin
   duplicar nada, salvo quizás un cache de pocos segundos donde tenga sentido); los reportes de
   acompañamiento, las alertas y las solicitudes sí son datos nuevos, porque no existen en
   ningún sistema previo.
2. Asumí un ecosistema típico de universidad: un Sistema Académico (datos personales,
   matrícula, notas, condición académica), un Sistema Financiero (saldos, becas, estado de
   cuenta), un LMS (actividad de campus virtual), y un Data Warehouse que solo consume, nunca
   es fuente de datos operacionales.
3. Doy por hecho que los sistemas fuente exponen o pueden exponer API REST/SOAP. Si alguno no
   puede, dejo como salida de emergencia leer directo de una vista SQL, pero lo marco como una
   excepción de alto riesgo, no como algo equivalente a integrarse por API.
4. Si el estado financiero se pide "de inmediato" (Escenario A de la Parte 3), entiendo que
   eso implica consulta síncrona sin cache o con TTL de segundos, porque ahí la exactitud pesa
   más que la latencia y puede condicionar una matrícula o un grado.
5. Un cambio de condición académica (Escenario B de la Parte 3) es un evento de negocio con
   varios consumidores que no necesitan enterarse al mismo tiempo (acompañamiento, Data
   Warehouse), así que lo resuelvo con un bus de eventos, no con polling.
6. Asumo que la universidad ya tiene un IdP institucional (tipo SSO/OIDC) y que Vista 360° se
   federa contra él en vez de inventar su propio sistema de usuarios y contraseñas.
7. La asignación acompañante-estudiante es información nueva, propia de Vista 360°: no existe
   en ningún sistema del ecosistema actual, así que se modela y persiste ahí.

## Cómo se comunican los componentes (ver diagrama)

El frontend le habla al BFF por HTTPS con JWT. El BFF valida ese JWT, autoriza según el rol,
arma la respuesta juntando cada sección de la vista, y propaga un `X-Correlation-Id` de punta a
punta.

De ahí, el BFF llama de forma síncrona (REST interno, con timeout corto y circuit breaker) a un
adaptador por cada sistema fuente (Académico, Financiero, LMS) y al servicio de Acompañamiento,
que es CRUD directo sobre una base Oracle propia porque su dato no vive en ningún otro lado.
Cada adaptador se encarga de traducir el protocolo real del sistema fuente (REST, SOAP, o una
vista SQL si no queda otra) a un contrato REST/JSON uniforme hacia el BFF.

Por otro lado, el Sistema Académico publica eventos de negocio (`AcademicStatusChanged`, con
`eventId` y `timestamp` para poder consumirlos de forma idempotente y en orden) a un bus tipo
Kafka. De ese bus consumen, de forma independiente entre sí, el motor de alertas tempranas de
acompañamiento y un pipeline hacia el Data Warehouse. El DW se completa además con un batch/CDC
nocturno para los datos históricos que no viajan como eventos.

## Por qué estas decisiones

Puse un solo BFF como entrada del frontend para no exponer la topología interna del ecosistema
y para centralizar autenticación, autorización y logging en un solo lugar. Cada sección de la
vista (académico, financiero, LMS) degrada por separado. Si el LMS está caído, no tiene
sentido que el estudiante se quede sin ver ni siquiera sus notas.

Cada sistema fuente tiene su propio adaptador en vez de meter todo en un integrador único,
para que el ciclo de cambios de cada sistema legado quede aislado y no arrastre fallas de uno
a otro.

Las consultas las dejo síncronas porque los sistemas fuente siguen siendo la fuente de verdad;
no los replico porque eso metería el riesgo de mostrar información desactualizada justo en
decisiones que importan.

Los datos de acompañamiento van en una base propia simplemente porque no hay ningún sistema
externo del que integrarlos.

El bus de eventos lo reservo para eventos de negocio de verdad (cambio de condición, alertas),
no para todo el tráfico. Meter eventos en cada interacción hubiera sido sobre-ingeniería
para lo que pide el caso.

Vale la pena aclarar un punto: usar CDC como respaldo para publicar eventos es
un supuesto de bastante riesgo. En la práctica, pocas universidades dan acceso a los redo logs
de su ERP académico, así que esto lo dejo anotado como algo que habría que validar con el
equipo dueño del sistema antes de darlo por sentado, no como una alternativa intercambiable con
que el propio sistema publique los eventos activamente.

La autorización se revalida en cada servicio backend, nunca queda solo en manos del gateway
(esto lo desarrollo con más detalle en la Parte 3, pero la decisión de fondo ya está tomada
aquí). Y la auditoría de lecturas junto con el correlation-id de punta a punta los pienso desde
el diseño, no como algo que se agrega después de un incidente: son justamente lo que hace
posible resolver los escenarios de operación de la Parte 4 (diagnosticar fallas intermitentes y
responder a un reclamo de acceso indebido).

## De dónde sale cada dato

| Dato                                                                                   | Origen                | Por qué                                                 |
| -------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------- |
| Datos personales, matrícula, notas, condición académica                             | Sistema Académico    | Es la fuente única de verdad académica                 |
| Estado financiero / saldos                                                             | Sistema Financiero    | Es la fuente única de verdad financiera                 |
| Actividad en campus virtual                                                            | LMS                   | Es el único sistema que registra eso                    |
| Reportes, alertas, solicitudes de acompañamiento; asignación acompañante-estudiante | BD propia Vista 360° | Es información nueva, no existe en el ecosistema previo |
| Modelos analíticos                                                                    | Data Warehouse        | Solo consume, no genera nada                             |
