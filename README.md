# PLM Upgrade — Cloud-Native PLM Platform

A lightweight, cloud-native **Product Lifecycle Management (PLM)** platform featuring:

- Item & BOM management with revisioning
- CAD file storage (STEP) and 3D in-browser rendering
- Containerized deployment on AWS
- Full CI/CD pipeline via GitHub Actions

---

## Architecture

```
Frontend (React + Three.js 3D Viewer)
        ↓
Backend API (Spring Boot)
        ↓
   PostgreSQL
        ↓
Object Storage (AWS S3 / MinIO)

Deployed via Docker + GitHub Actions → AWS EC2
```

---

## System Components

### A. PLM Backend
- Item creation
- Revisioning (A, B, C...)
- Lifecycle states
- Multi-level BOM
- Change Requests
- Document association

**Stack:** Spring Boot · PostgreSQL · JPA ORM · JWT Authentication

### B. 3D Web Viewer
- Load STEP files → convert to glTF → render in browser
- Orbit controls, zoom/pan
- Basic object tree
- Part highlighting
- Optional: section view, exploded view, measure tool

**Stack:** React · Three.js

### C. CAD File Handling
1. Upload STEP file
2. Store in S3 (AWS) or MinIO (local S3-compatible)
3. Convert STEP → glTF (via OpenCascade CLI or FreeCAD headless)
4. Render glTF in browser

---

## Data Model

```sql
items            -- id, item_number, name, description, lifecycle_state
revisions        -- id, item_id, revision_code, status
bom_links        -- parent_revision_id, child_revision_id, quantity
documents        -- id, revision_id, file_path, file_type
change_requests  -- id, title, status
```

---

## DevOps

### Docker
- `Dockerfile` for backend
- `Dockerfile` for frontend
- `docker-compose.yml` for local stack (backend + frontend + postgres + minio)

### CI/CD (GitHub Actions)
1. Install dependencies
2. Run tests
3. Build Docker images
4. Push to container registry
5. Deploy to AWS EC2

### Cloud (AWS Free Tier)
- EC2 for compute
- S3 for file storage
- Security groups
- Optional: Nginx reverse proxy + HTTPS (Let's Encrypt)

---

## Advanced Architecture (Senior Level)

### Event-Driven
- RabbitMQ or Kafka
- Item revised → event triggered
- File uploaded → conversion job triggered

### Microservices
| Service | Responsibility |
|---|---|
| Item Service | Item + revision CRUD |
| BOM Service | BOM link management |
| File Service | Upload + storage |
| 3D Processing Service | STEP → glTF conversion |

### Monitoring
- Prometheus
- Grafana
- Centralized logging

---

## Project Timeline

| Phase | Duration |
|---|---|
| Core PLM backend + DB | 4 weeks |
| 3D Viewer (Three.js) | 2–3 weeks |
| DevOps + Cloud (Docker, CI/CD, AWS) | 3 weeks |
| Advanced features (events, microservices) | ongoing |
| **Total** | **~10–12 weeks** |

---

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL (or use Docker)

### Run locally

```bash
# Start full stack
docker-compose up --build

# Backend only
cd backend && ./mvnw spring-boot:run

# Frontend only
cd frontend && npm install && npm start
```

---

## What This Project Demonstrates

- PLM data modeling
- CAD file lifecycle management
- 3D rendering pipelines in the browser
- Cloud deployment on AWS
- DevOps automation with Docker + GitHub Actions
- Microservice architecture patterns
