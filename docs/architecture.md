# Architecture — PLM Upgrade v2

## High-Level Architecture

```
Browser (Angular 17)
        ↓  HTTP / Bearer token (Keycloak JWT)
API Gateway (Nginx :80)
        ↓  routes /api/* to backend microservices
┌──────────────────────────────────────────────────────────────────────┐
│  plm-core-service :8080   │  workflow-service   :8082                │
│  search-service   :8083   │  integration-service :8084               │
│  notification-service :8085 │ reporting-service  :8086               │
└──────────────────────────────────────────────────────────────────────┘
        ↓                         ↓
  PostgreSQL :5432          Camunda 8 Zeebe :26500
  MinIO :9000               Elasticsearch :9200
        ↓                         ↓
                 Apache Kafka :9092
                         ↓
           Prometheus :9090  /  Grafana :3000
```

## Service Responsibilities

| Service | Port | Responsibility |
|---|---|---|
| **api-gateway** | 80 | Nginx — routes all `/api/*` traffic, serves frontend, WebSocket upgrade |
| **auth-service** (Keycloak) | 8081 | OAuth2 / OpenID Connect — issues JWTs, manages users & roles |
| **plm-core-service** | 8080 | Items, Revisions, BOM, Documents, Change Requests + Kafka publisher |
| **cad-service** | 5000 | Python Flask — STEP → GLB conversion (pythonocc-core) |
| **workflow-service** | 8082 | Camunda 8 Zeebe workers — revision approval BPMN + task inbox REST API |
| **search-service** | 8083 | Elasticsearch indexing + full-text search |
| **integration-service** | 8084 | Pluggable connectors to external systems (Odoo, MES, FreeCAD) + HMAC webhook |
| **notification-service** | 8085 | Kafka consumer → WebSocket STOMP push + email alerts + REST notification API |
| **reporting-service** | 8086 | Dashboard summary + Excel BOM export + CSV items export |
| **frontend-angular** | 4200 | Angular 17 SPA with Three.js 3D viewer, Task Inbox, Reports, Notifications |

## Auth Flow

```
Browser → Keycloak login page
        ← access_token (JWT)
Browser → /api/* with Authorization: Bearer <token>
API Gateway passes through to microservice
Microservice validates JWT via Keycloak JWKS endpoint
KeycloakRoleConverter extracts realm_access.roles → ROLE_ADMIN/ENGINEER/VIEWER

Role enforcement:
  VIEWER   → read-only (no create/edit/delete/upload/workflow)
  ENGINEER → full write access
  ADMIN    → full write access
```

## CAD File Flow

```
User uploads STEP file via POST /api/revisions/{id}/documents
        ↓
plm-core-service stores raw STEP → MinIO (bucket: cad-files-raw)
        ↓
Publishes conversion job to Kafka topic: plm.conversion
        ↓
cad-service consumes job → converts STEP → GLB (pythonocc-core + trimesh)
        ↓
GLB stored → MinIO (bucket: cad-files-gltf), gltf_path saved to DB
        ↓
WebSocket push to frontend: conversionStatus = DONE / FAILED
Frontend fetches GLB via GET /api/documents/{id}/file (JWT-authenticated stream)
Three.js renders GLB in browser
```

## Event Flow (Kafka)

```
plm-core-service  ──► plm.item-events      ──► search-service       (indexes to ES)
  (ItemEventPublisher)                      ──► integration-service  (fan-out to connectors)
  ITEM_CREATED                              ──► notification-service (alerts to users)
  ITEM_UPDATED
  ITEM_DELETED
  LIFECYCLE_CHANGED

workflow-service  ──► plm.workflow-events  ──► notification-service  (approval alerts)
  REVISION_RELEASED
  REVISION_REJECTED

integration-service ──► plm.external-events ──► plm-core-service    (ExternalEventConsumer)
  (inbound webhook from Odoo/MES/FreeCAD)

plm-core-service  ──► plm.conversion       ──► cad-service          (STEP→GLB jobs)
```

## Revision Approval Workflow (Camunda 8 BPMN)

```
POST /api/workflows/revisions/{id}/start
        ↓
Zeebe process: revision-approval.bpmn
        ↓
ManagerReviewWorker (serviceTask, autoComplete=false, 24h timeout)
  → stores jobKey → step = MANAGER_REVIEW
  → UI: GET /api/workflows/tasks  (list pending tasks)
  → UI: POST /api/workflows/tasks/{jobKey}/complete  {decision: APPROVED|REJECTED, comment}
        ↓ APPROVED
QualityReviewWorker (serviceTask, autoComplete=false, 24h timeout)
  → stores jobKey → step = QUALITY_REVIEW
        ↓ APPROVED
ReleaseRevisionWorker → step = RELEASED
  → publishes REVISION_RELEASED to plm.workflow-events
        ↓ REJECTED (any stage)
NotifyRejectionWorker → step = REJECTED
  → publishes REVISION_REJECTED to plm.workflow-events
```

## Integration Service — Connector Pattern

```
Kafka plm.item-events / plm.workflow-events
        ↓
EventBridgeService
        ├── OdooConnector   (ODOO_ENABLED=true)     → Odoo JSON-RPC /web/dataset/call_kw
        │     createOdooProduct / updateOdooProduct / releaseOdooProduct (search+write)
        ├── MesConnector    (MES_ENABLED=true)       → MES REST API
        └── FreeCADConnector (FREECAD_ENABLED=true)  → FreeCAD REST API (register/export STEP+PDF)

POST /api/integration/webhook/{source}
        ↓  HMAC-SHA256 signature verified (X-Hub-Signature-256 header)
        ↓  (skipped if WEBHOOK_SECRET not set — dev mode)
publishes to plm.external-events Kafka topic
        ↓
plm-core-service ExternalEventConsumer logs and processes inbound events
```

## Notification Service Flow

```
Kafka consumer (plm.item-events, plm.workflow-events)
        ↓
NotificationService.create()
        ├── saves Notification entity to PostgreSQL
        └── pushes via WebSocket STOMP to /topic/notifications/{recipient}

Frontend shell polls GET /api/notifications/unread-count every 30s → badge on bell icon
Frontend Notifications page: GET /api/notifications, PUT /api/notifications/{id}/read
```

## Reporting Service

```
GET /api/reports/summary
  → calls plm-core-service /api/items + /api/change-requests
  → returns counts by lifecycleState and CR status

GET /api/reports/items/export?format=csv
  → streams all items as CSV (application/octet-stream)

GET /api/reports/bom/{revisionId}/export
  → calls plm-core-service /api/revisions/{id}/bom/children
  → builds XSSFWorkbook (Apache POI) with BOM tree
  → returns as .xlsx (application/vnd.openxmlformats...)
```

## Infrastructure

### Docker (local dev)
```
docker compose -f infrastructure/docker/docker-compose.yml up
```
15 services: postgres, minio, kafka, zookeeper, redis, elasticsearch, keycloak-db,
auth-service, plm-core-service, cad-service, workflow-service, search-service,
integration-service, notification-service, reporting-service, api-gateway, frontend-angular,
prometheus, grafana

### Kubernetes (production)
```
kubectl apply -k infrastructure/kubernetes/overlays/prod
```
- Base: Kustomize manifests for all 10 services + StatefulSets for databases
- Prod overlay: 3 replicas for core/gateway/frontend, pinned image tags (`1.0.0`)
- Ingress: AWS ALB (internet-facing, HTTPS redirect)

### Terraform (AWS)
```
cd infrastructure/terraform
terraform init && terraform apply -var-file=example.tfvars
```
Modules: `vpc` (3 AZs, NAT gateway), `eks` (managed node group), `rds` (PostgreSQL 15, encrypted),
`s3` (CAD files bucket, versioned + private)

## Security

- **Auth**: Keycloak 24 OAuth2 / OpenID Connect
- **Roles**: `ADMIN`, `ENGINEER`, `VIEWER` (stored as Keycloak realm roles)
- **JWT validation**: Each microservice validates tokens independently via Keycloak JWKS
- **Role-based UI**: Angular hides write actions from VIEWER role (`*ngIf="!auth.isViewer()"`)
- **Webhook endpoints**: HMAC-SHA256 signature verification (`X-Hub-Signature-256`)
- **All other API endpoints**: Require valid JWT

## Monitoring

- **Actuator**: `/actuator/health` + `/actuator/prometheus` on all Spring Boot services
- **Micrometer**: `micrometer-registry-prometheus` on all 6 Spring Boot services
- **Prometheus** (port 9090): scrapes all Spring Boot actuator endpoints
- **Grafana** (port 3000): dashboards for API latency, DB connections, Kafka lag
- **Camunda Operate** (port 8088): Zeebe workflow monitoring
