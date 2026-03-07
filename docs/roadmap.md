# Project Roadmap

## Phase 1 — Core PLM Backend
- [x] Spring Boot project setup
- [x] PostgreSQL schema + JPA entities (Item, Revision, BomLink, Document, ChangeRequest)
- [x] Item CRUD endpoints
- [x] Revisioning logic (A → B → C auto-increment)
- [x] Lifecycle state machine (DRAFT → IN_REVIEW → RELEASED → OBSOLETE)
- [x] Multi-level BOM API
- [x] Change Request API
- [x] JWT authentication (custom jjwt — replaced in v2 by Keycloak)
- [x] Unit + integration tests (10/10 passing)

## Phase 2 — Angular Frontend
- [x] Angular 17 standalone components + Angular Material
- [x] Three.js 3D viewer (GLB/GLTF)
- [x] Items table (search, filter, pagination)
- [x] Item detail — revisions, BOM, documents, 3D viewer
- [x] Change Requests table
- [x] JWT interceptor + auth guard (replaced by Keycloak in v2)

## Phase 3 — CAD File Handling
- [x] File upload API (multipart)
- [x] MinIO integration (local S3-compatible)
- [x] STEP → GLB conversion (Python Flask + pythonocc-core + trimesh)
- [x] GLB stored in MinIO, gltf_path in DB
- [x] Streaming download endpoint (JWT-authenticated)
- [x] WebSocket push to frontend on conversion DONE / FAILED

## Phase 4 — DevOps
- [x] Dockerfile for all services (multi-stage, eclipse-temurin:17-alpine)
- [x] docker-compose.yml (full local stack)
- [x] GitHub Actions CI: Maven tests + Angular build on every push/PR
- [x] GitHub Actions CD: build + push images to GHCR on push to main
- [x] SSH deploy step to EC2
- [x] Nginx API Gateway
- [x] Layer caching via GitHub Actions cache

## Phase 5 — V2 Microservices Architecture

### 5-prep — Monorepo Restructure
- [x] backend/ → services/plm-core-service/
- [x] frontend/ → frontend-angular/
- [x] New services scaffolded: workflow, search, integration

### 5a — Keycloak Auth + Kafka Messaging
- [x] Keycloak 24 OAuth2 Resource Server (replaced custom jjwt)
- [x] KeycloakRoleConverter: `realm_access.roles` → `ROLE_ADMIN/ENGINEER/VIEWER`
- [x] realm-export.json with plm realm, 3 roles, test users
- [x] Kafka topics declared: `plm.conversion`, `plm.item-events`, `plm.workflow-events`, `plm.external-events`
- [x] **ItemEventPublisher**: fires `ITEM_CREATED`, `ITEM_UPDATED`, `ITEM_DELETED`, `LIFECYCLE_CHANGED` on every ItemService operation (wired into save/delete/transition calls)
- [x] **ExternalEventConsumer**: plm-core-service consumes `plm.external-events`

### 5b — Workflow Service (Camunda 8 / Zeebe, port 8082)
- [x] BPMN: revision-approval.bpmn — Manager Review → Quality Approval → Release/Reject
- [x] **Task Inbox pattern**: BPMN tasks converted from `userTask` to `serviceTask` with `autoComplete=false` and 24h timeout
- [x] `ManagerReviewWorker`: stores jobKey + sets step = MANAGER_REVIEW
- [x] `QualityReviewWorker`: stores jobKey + sets step = QUALITY_REVIEW
- [x] `PendingTask` DTO + `WorkflowInstanceStore` (in-memory task registry)
- [x] `GET /api/workflows/tasks` — list all pending tasks for task inbox UI
- [x] `POST /api/workflows/tasks/{jobKey}/complete` — approve or reject with optional comment
- [x] ReleaseRevisionWorker, NotifyRejectionWorker → publish to plm.workflow-events
- [x] `GET /api/workflows/revisions/{id}` — list workflow instances for a revision

### 5c — Search Service (Elasticsearch 8.13, port 8083)
- [x] IndexingConsumer: syncs `plm.item-events` and `plm.workflow-events` to Elasticsearch
- [x] `GET /api/search/items?q=` — full-text search (itemNumber, name, description)
- [x] `GET /api/search/items/by-state?state=` — filter by lifecycle state
- [x] Unit tests: `SearchControllerTest`

### 5d — Kubernetes + Terraform
- [x] K8s base: Kustomize manifests for all 10 services (including notification + reporting)
- [x] K8s prod overlay: 3 replicas, pinned image tags (`1.0.0`)
- [x] Ingress: AWS ALB (internet-facing, HTTPS redirect)
- [x] Terraform: `vpc` (3AZ, NAT), `eks` (managed nodes), `rds` (pg15, encrypted), `s3` (versioned)

### 5e — Angular Keycloak SSO
- [x] keycloak-angular@15 + keycloak-js@24
- [x] KeycloakAuthGuard, silent-check-sso.html, enableBearerInterceptor
- [x] Removed: jwt.interceptor, login component, register component

### 5f — Integration Service (port 8084)
- [x] Pluggable connector pattern (`@ConditionalOnProperty`, `ExternalSystemConnector` interface)
- [x] **OdooConnector** (Odoo JSON-RPC): `createOdooProduct`, `updateOdooProduct` (search+write), `releaseOdooProduct` — all fully implemented
- [x] MesConnector: MES REST API — REVISION_RELEASED notification
- [x] FreeCADConnector: FreeCAD REST API — register items, trigger STEP+PDF export
- [x] EventBridgeService: Kafka fan-out to all enabled connectors
- [x] `POST /api/integration/webhook/{source}` → **HMAC-SHA256 signature verification** (sha256= prefix, `X-Hub-Signature-256`)
- [x] Integration tests: `EventBridgeServiceTest`

## Phase 6 — Platform Completion

### Notification Service (port 8085)
- [x] Spring Boot + Spring Kafka + Spring WebSocket (STOMP) + Spring Data JPA + JavaMail (optional)
- [x] `PlmEventConsumer`: consumes `plm.item-events` (ITEM_CREATED, LIFECYCLE_CHANGED, ITEM_DELETED) and `plm.workflow-events` (REVISION_RELEASED, REVISION_REJECTED)
- [x] `Notification` entity persisted to PostgreSQL
- [x] Real-time WebSocket push to `/topic/notifications/{recipient}`
- [x] REST API: `GET /api/notifications`, `GET /api/notifications/unread-count`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`
- [x] Nginx WebSocket upgrade config (`/ws-notifications`)
- [x] Kubernetes manifest: `notification-service.yaml`

### Reporting Service (port 8086)
- [x] Spring Boot + Apache POI 5.2.5 (Excel) + RestTemplate
- [x] `GET /api/reports/summary` — item counts by lifecycleState, CR counts by status
- [x] `GET /api/reports/items/export?format=csv` — full item list as CSV download
- [x] `GET /api/reports/bom/{revisionId}/export` — BOM tree as `.xlsx` Excel file
- [x] Kubernetes manifest: `reporting-service.yaml`

### Monitoring
- [x] Prometheus (port 9090) added to docker-compose with scrape config for all Spring Boot services
- [x] Grafana (port 3000) added to docker-compose
- [x] `micrometer-registry-prometheus` dependency added to all Spring Boot services
- [x] `infrastructure/docker/prometheus.yml` — scrape targets for all 6 services

### Role-Based UI
- [x] `auth.isViewer()` guard implemented in `AuthService`
- [x] item-list: `New Item` button, `Edit` and `Delete` actions hidden from VIEWER
- [x] item-detail: lifecycle transition select, Next Revision button, revision status select, Start Approval button, BOM add form, Upload File, Delete document — all hidden from VIEWER

### Angular New Pages
- [x] **Task Inbox** (`/workflows/tasks`): table of pending approval tasks, inline APPROVE/REJECT form with optional comment field
- [x] **Reports** (`/reports`): summary cards (total items, CRs), tables by state/status, CSV + Excel export buttons
- [x] **Notifications** (`/notifications`): notification list with unread highlight, mark read / mark all read
- [x] **Shell**: notification bell with `[matBadge]` for unread count (polled every 30s), Task Inbox + Reports nav links

### CI/CD Expansion
- [x] `test-workflow` job: Maven tests for workflow-service
- [x] `test-search` job: Maven tests for search-service
- [x] `test-integration` job: Maven tests for integration-service
- [x] `build-and-push`: added notification-service and reporting-service image builds

## Remaining / Future Work
- [ ] Audit log service (event sourcing from all Kafka topics)
- [ ] HTTPS with Let's Encrypt (production)
- [ ] Performance testing (Gatling / k6)
- [ ] Architecture diagram (draw.io / Excalidraw)
- [ ] Live demo on AWS EKS
- [ ] Video walkthrough
- [ ] Lessons learned write-up

## Portfolio Deliverables
- [x] Full microservices backend (8 services)
- [x] Angular frontend with 3D CAD viewer, Task Inbox, Reports, Notifications
- [x] Kubernetes manifests (10 services) + Terraform IaC
- [x] GitHub Actions CI/CD pipeline (8 image builds)
- [x] Prometheus + Grafana monitoring
- [x] Role-based access control (VIEWER / ENGINEER / ADMIN)
- [ ] Architecture diagram
- [ ] Live demo
- [ ] Lessons learned write-up
