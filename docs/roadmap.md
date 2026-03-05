# Project Roadmap

## Phase 1 — Core PLM Backend
- [x] Spring Boot project setup
- [x] PostgreSQL schema + JPA entities (Item, Revision, BomLink, Document, ChangeRequest)
- [x] Item CRUD endpoints
- [x] Revisioning logic (A → B → C auto-increment)
- [x] Lifecycle state machine (DRAFT → IN_REVIEW → RELEASED → OBSOLETE)
- [x] Multi-level BOM API
- [x] Change Request API
- [x] JWT authentication (custom jjwt — replaced in v2)
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

## Phase 4 — DevOps
- [x] Dockerfile for all services (multi-stage, eclipse-temurin:17-alpine)
- [x] docker-compose.yml (full local stack)
- [x] GitHub Actions CI: Maven tests + Angular build on every push/PR
- [x] GitHub Actions CD: build + push images to GHCR on push to main
- [x] SSH deploy step to EC2
- [x] Nginx API Gateway
- [x] Layer caching via GitHub Actions cache

## Phase 5 — V2 Microservices Architecture
- [x] **5-prep**: Monorepo restructure (backend/ → services/plm-core-service/, frontend/ → frontend-angular/)
- [x] **5a**: Keycloak 24 OAuth2 auth (replaced custom JWT); Kafka messaging (replaced RabbitMQ)
  - KeycloakRoleConverter, realm-export.json, plm realm with ADMIN/ENGINEER/VIEWER roles
  - Kafka topics: plm.conversion, plm.item-events
- [x] **5b**: Workflow Service (Camunda 8 / Zeebe, port 8082)
  - BPMN: revision-approval.bpmn — Manager review → Quality approval → Release/Reject
  - ReleaseRevisionWorker, NotifyRejectionWorker → publish to plm.workflow-events
- [x] **5c**: Search Service (Elasticsearch 8.13, port 8083)
  - IndexingConsumer: syncs item-events + workflow-events to ES
  - GET /api/search/items?q= (full-text), /api/search/items/by-state?state=
- [x] **5d**: Kubernetes + Terraform
  - K8s base: Kustomize manifests, StatefulSets, Deployments, ALB Ingress
  - K8s prod overlay: 3 replicas, pinned image tags
  - Terraform: vpc (3AZ, NAT), eks (managed nodes), rds (pg15, encrypted), s3 (versioned)
- [x] **5e**: Angular — Keycloak SSO integration
  - keycloak-angular@15 + keycloak-js@24
  - KeycloakAuthGuard, silent-check-sso.html, enableBearerInterceptor
  - Removed: jwt.interceptor, login component, register component
- [x] **5f**: Integration Service (port 8084)
  - Pluggable connector pattern (`@ConditionalOnProperty`, `ExternalSystemConnector` interface)
  - OdooConnector: Odoo JSON-RPC — ITEM_CREATED/UPDATED/REVISION_RELEASED
  - MesConnector: MES REST API — REVISION_RELEASED notification
  - FreeCADConnector: FreeCAD REST API — register items, trigger STEP+PDF export
  - EventBridgeService: Kafka fan-out to all enabled connectors
  - Webhook endpoint: POST /api/integration/webhook/{source} → plm.external-events

## Planned / Next Steps
- [ ] Prometheus + Grafana monitoring stack
- [ ] Audit log service (event sourcing from Kafka)
- [ ] Real-time notifications (WebSocket / SSE)
- [ ] HTTPS with Let's Encrypt (production)
- [ ] Performance testing (Gatling / k6)
- [ ] Architecture diagram (draw.io)
- [ ] Live demo on AWS
- [ ] Video walkthrough

## Portfolio Deliverables
- [x] Full microservices backend (6 services)
- [x] Angular frontend with 3D CAD viewer
- [x] Kubernetes manifests + Terraform IaC
- [x] GitHub Actions CI/CD pipeline
- [ ] Architecture diagram
- [ ] Live demo
- [ ] Lessons learned write-up
