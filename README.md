# PLM Platform v2 — Cloud-Native Microservices PLM

A full **Product Lifecycle Management (PLM)** platform built on a microservices architecture.

> **v1 (monolith)** is preserved on the `PLM-Upgrade-v1` branch.

---

## Architecture

```
Angular Frontend (frontend-angular/)
        |
   API Gateway  (Nginx — api-gateway/)
        |
----------------------------------------------------
|           |          |          |         |      |
Auth     PLM Core    CAD      Workflow   Search  Integration
(Keycloak) (Spring)  (Flask)  (Temporal) (ES)   (Kafka)
----------------------------------------------------
        |
   Event Bus (Kafka)
        |
Shared Infrastructure
(PostgreSQL · MinIO · Redis · Elasticsearch)
```

---

## Repository Structure

```
plm-platform/
│
├── frontend-angular/          # Angular 17 SPA
├── api-gateway/               # Nginx routing + Dockerfile
│
├── services/
│   ├── auth-service/          # Keycloak config + realm export
│   ├── plm-core-service/      # Spring Boot — items, revisions, BOM, docs, CRs
│   ├── cad-service/           # Python Flask — STEP→GLB conversion
│   ├── workflow-service/      # Approval workflow engine (TODO: Temporal/Camunda)
│   ├── search-service/        # Elasticsearch-backed search (TODO)
│   └── integration-service/   # ERP/MES connectors (TODO)
│
├── infrastructure/
│   ├── docker/                # docker-compose.yml (full v2 stack)
│   ├── kubernetes/            # K8s manifests (TODO)
│   └── terraform/             # IaC (TODO)
│
└── docs/                      # Architecture docs
```

---

## Quick Start (Local)

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for PLM Core dev)
- Node.js 20+ (for frontend dev)
- Python 3.10+ with conda (for CAD service dev)

### Run full stack

```bash
docker compose -f infrastructure/docker/docker-compose.yml up --build
```

| Service         | URL                        |
|-----------------|----------------------------|
| Frontend        | http://localhost:4200       |
| API Gateway     | http://localhost:80         |
| PLM Core API    | http://localhost:8080       |
| Keycloak Admin  | http://localhost:8081       |
| MinIO Console   | http://localhost:9001       |
| Elasticsearch   | http://localhost:9200       |

### Run individual services (dev mode)

```bash
# PLM Core
cd services/plm-core-service && ./mvnw spring-boot:run

# CAD Service
cd services/cad-service && flask run

# Frontend
cd frontend-angular && npm install && npm start
```

---

## Services

| Service | Port | Tech | Status |
|---|---|---|---|
| auth-service | 8081 | Keycloak 24 | Ready |
| plm-core-service | 8080 | Spring Boot 3 | Ready |
| cad-service | 5000 | Python Flask | Ready |
| workflow-service | 8082 | TBD (Temporal) | Planned |
| search-service | 8083 | Elasticsearch | Planned |
| integration-service | — | Kafka | Planned |
| api-gateway | 80 | Nginx | Ready |
| frontend-angular | 4200 | Angular 17 | Ready |

---

## CI/CD

GitHub Actions (`.github/workflows/ci-cd.yml`):
1. Test PLM Core (Maven + H2)
2. Build Angular frontend
3. Build & push Docker images to GHCR
4. Deploy to EC2 (main branch only)
