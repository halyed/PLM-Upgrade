# Architecture — PLM Upgrade v2

## High-Level Architecture

```
Browser (Angular 17)
        ↓  HTTP / Bearer token (Keycloak JWT)
API Gateway (Nginx :80)
        ↓  routes /api/* to backend microservices
┌─────────────────────────────────────────────────────────┐
│  plm-core-service :8080   │  workflow-service :8082      │
│  search-service   :8083   │  integration-service :8084   │
└─────────────────────────────────────────────────────────┘
        ↓                         ↓
  PostgreSQL :5432          Camunda 8 Zeebe :26500
  MinIO :9000               Elasticsearch :9200
        ↓                         ↓
                 Apache Kafka :9092
```

## Service Responsibilities

| Service | Port | Responsibility |
|---|---|---|
| **api-gateway** | 80 | Nginx — routes all `/api/*` traffic, serves frontend |
| **auth-service** (Keycloak) | 8081 | OAuth2 / OpenID Connect — issues JWTs, manages users & roles |
| **plm-core-service** | 8080 | Items, Revisions, BOM, Documents, Change Requests |
| **cad-service** | 5000 | Python Flask — STEP → GLB conversion (pythonocc-core) |
| **workflow-service** | 8082 | Camunda 8 Zeebe workers — revision approval BPMN |
| **search-service** | 8083 | Elasticsearch indexing + full-text search |
| **integration-service** | 8084 | Pluggable connectors to external systems (Odoo, MES, FreeCAD) |
| **frontend-angular** | 4200 | Angular 17 SPA with Three.js 3D viewer |

## Auth Flow

```
Browser → Keycloak login page
        ← access_token (JWT)
Browser → /api/* with Authorization: Bearer <token>
API Gateway passes through to microservice
Microservice validates JWT via Keycloak JWKS endpoint
KeycloakRoleConverter extracts realm_access.roles → ROLE_ADMIN/ENGINEER/VIEWER
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
Frontend fetches GLB via GET /api/documents/{id}/file (JWT-authenticated stream)
Three.js renders GLB in browser
```

## Event Flow (Kafka)

```
plm-core-service  ──► plm.item-events      ──► search-service (indexes to ES)
                                            ──► integration-service (fan-out to connectors)

workflow-service  ──► plm.workflow-events  ──► search-service (indexes revision state)
                                            ──► integration-service (notifies external systems)

integration-service ──► plm.external-events (inbound events from Odoo/MES/FreeCAD)

plm-core-service  ──► plm.conversion       ──► cad-service (STEP→GLB jobs)
```

## Revision Approval Workflow (Camunda 8 BPMN)

```
POST /api/workflows/revisions/{id}/start
        ↓
Zeebe process: revision-approval.bpmn
        ↓
Manager Review task ──► Approve ──► Quality Approval task ──► Approve ──► ReleaseRevisionWorker
                    └─► Reject ──────────────────────────────────────────► NotifyRejectionWorker
        ↓
Worker publishes REVISION_RELEASED / REVISION_REJECTED to plm.workflow-events
```

## Integration Service — Connector Pattern

```
Kafka plm.item-events / plm.workflow-events
        ↓
EventBridgeService
        ├── OdooConnector   (enabled: ODOO_ENABLED=true)    → Odoo JSON-RPC /web/dataset/call_kw
        ├── MesConnector    (enabled: MES_ENABLED=true)     → MES REST API
        └── FreeCADConnector (enabled: FREECAD_ENABLED=true) → FreeCAD REST API (register/export STEP+PDF)

POST /api/integration/webhook/{source}  ← inbound from external systems
        ↓
publishes to plm.external-events Kafka topic
```

## Infrastructure

### Docker (local dev)
```
docker compose -f infrastructure/docker/docker-compose.yml up
```
13 services: postgres, minio, kafka, zookeeper, redis, elasticsearch, keycloak-db,
auth-service, plm-core-service, cad-service, workflow-service, search-service,
integration-service, api-gateway, frontend-angular

### Kubernetes (production)
```
kubectl apply -k infrastructure/kubernetes/overlays/prod
```
- Base: Kustomize manifests for all services + StatefulSets for databases
- Prod overlay: 3 replicas for core/gateway/frontend, pinned image tags
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
- **Webhook endpoints**: Permitted without JWT (external systems push events)
- **All other API endpoints**: Require valid JWT

## Monitoring (planned)

- **Actuator**: `/actuator/health` exposed on all Spring Boot services
- **Prometheus** — scrape Spring Boot Actuator metrics
- **Grafana** — dashboards for API latency, DB connections, Kafka lag
- **Camunda Operate** — Zeebe workflow monitoring (port 8088)
