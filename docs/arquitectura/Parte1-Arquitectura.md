# Vista 360° del Estudiante — Parte 1: Diseño de la solución

Ver `diagrama-arquitectura.svg` para el diagrama completo. Este documento explica las
decisiones y los supuestos que lo sustentan.

## Supuestos declarados

1. Vista 360° es una capa de **agregación + datos propios**, no un ERP paralelo. Académico,
   financiero y LMS se leen *read-through* desde los sistemas fuente (no se duplican, salvo
   cache de segundos donde aplica); los reportes de acompañamiento, alertas y solicitudes son
   datos **nativos** de Vista 360° porque no existen en ningún sistema previo.
2. Sistemas fuente asumidos, con roles genéricos que representan el ecosistema típico de una
   universidad: **Sistema Académico** (datos personales, matrícula, notas, condición
   académica), **Sistema Financiero** (saldos, becas, estado de cuenta), **LMS** (actividad de
   campus virtual), y el **Data Warehouse** como destino analítico (nunca fuente de datos
   operacionales).
3. Los sistemas fuente exponen o pueden exponer API REST/SOAP; si alguno no puede, el
   fallback es lectura directa vía vista SQL/ODBC — declarado explícitamente como una
   **excepción de alto riesgo**, no como una alternativa equivalente a la integración vía API.
4. El estado financiero pedido "de inmediato" (Escenario A de la Parte 3) implica **consulta
   síncrona sin cache** (o TTL de segundos): la exactitud del dato pesa más que la latencia,
   porque puede condicionar decisiones del estudiante (matrícula, grado).
5. El cambio de condición académica (Escenario B de la Parte 3) es un evento de negocio con
   múltiples consumidores desacoplados en el tiempo (acompañamiento, Data Warehouse) → se
   resuelve **asíncrono vía bus de eventos**, no por polling.
6. Existe un **IdP institucional** (tipo SSO/OIDC) — Vista 360° se federa contra él, no
   reinventa gestión de usuarios ni contraseñas.
7. La asignación acompañante↔estudiante es un dato **nuevo**, propio de Vista 360°: no existe
   en ningún sistema del ecosistema actual, por lo tanto se modela y persiste ahí.

## Componentes y comunicación (ver diagrama)

- **Frontend (SPA)** → HTTPS + JWT → **BFF / API Gateway** (Spring Boot): valida el JWT,
  autoriza por rol, agrega las respuestas de cada sección de la vista, y propaga un
  `X-Correlation-Id` de punta a punta.
- El BFF llama de forma **síncrona** (REST interno, timeout corto + circuit breaker) a un
  **adaptador por cada sistema fuente** (Académico, Financiero, LMS) y al **servicio de
  Acompañamiento**, que es CRUD directo sobre una base Oracle propia porque su dato no existe
  en ningún otro sistema.
- Cada adaptador traduce el protocolo real del sistema fuente (REST, SOAP, o vista SQL como
  fallback) a un contrato REST/JSON uniforme hacia el BFF.
- El **Sistema Académico publica eventos de negocio** (`AcademicStatusChanged`, con `eventId`
  y `timestamp` para consumo idempotente y ordenado) a un **bus de eventos (Kafka)**.
  Consumidores independientes: el **motor de alertas tempranas** de acompañamiento, y un
  **pipeline ETL hacia el Data Warehouse**. El DW se completa con batch/CDC nocturno para
  datos históricos que no viajan como eventos.

## Decisiones clave (justificación en una línea cada una)

- **BFF único** como entrada del frontend: centraliza AuthN/AuthZ y logging, evita exponer la
  topología interna del ecosistema. Cada sección de la vista (académico/financiero/LMS)
  **degrada de forma independiente** — que el LMS esté caído no debe tumbar toda la vista.
- **Un adaptador por sistema fuente**, no un integrador monolítico: aísla el ciclo de cambio
  de cada sistema legado sin acoplar sus fallas entre sí.
- **Lectura síncrona para consulta**: los sistemas fuente son la fuente de verdad; no se
  replican para evitar el riesgo de mostrar datos desactualizados en decisiones sensibles.
- **Datos de acompañamiento en BD propia**: es información nueva de negocio, no existe fuente
  externa que integrar.
- **Bus de eventos solo para eventos de negocio reales** (cambio de condición, alertas), no
  para todo el tráfico: evita el over-engineering de una arquitectura orientada a eventos
  completa cuando no se necesita.
- **CDC como fallback de publicación de eventos es un supuesto de alto riesgo**: el acceso a
  redo logs de un ERP institucional rara vez está disponible en la práctica; se declara como
  algo que requeriría validación con el equipo dueño del sistema académico, no como una
  alternativa intercambiable con la publicación activa de eventos.
- **Autorización revalidada en cada servicio backend**, nunca confiada únicamente al gateway
  (defensa en profundidad) — desarrollado en detalle en la Parte 3.
- **Auditoría de lecturas + correlation-id de punta a punta** sembrados desde el diseño, no
  añadidos después de un incidente — necesarios para los escenarios de operación de la
  Parte 4 (diagnóstico de fallas intermitentes y respuesta a reclamos de acceso indebido).

## De dónde sale cada dato

| Dato | Origen | Por qué |
|---|---|---|
| Datos personales, matrícula, notas, condición académica | Sistema Académico | Fuente única de verdad académica |
| Estado financiero / saldos | Sistema Financiero | Fuente única de verdad financiera |
| Actividad en campus virtual | LMS | Solo el LMS tiene esos registros |
| Reportes, alertas, solicitudes de acompañamiento; asignación acompañante↔estudiante | BD propia Vista 360° | Dato nuevo, no existe en el ecosistema previo |
| Modelos analíticos | Data Warehouse | Consumidor, no fuente |

## Relación con la Parte 2

El servicio implementado en la Parte 2 (`GET /api/v1/students/{studentId}/courses`) es la
**implementación de referencia del Adaptador Académico** de este diagrama: mismo contrato REST
que el BFF consumiría en producción, con un backing store propio y autocontenido para que sea
ejecutable de punta a punta sin depender de un ERP real durante la prueba técnica.
