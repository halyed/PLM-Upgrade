# Project Roadmap

## Phase 1 — Core PLM Backend (4 weeks)
- [ ] Spring Boot project setup
- [ ] PostgreSQL schema + JPA entities
- [ ] Item CRUD endpoints
- [ ] Revisioning logic (A → B → C)
- [ ] Lifecycle state machine (DRAFT → IN_REVIEW → RELEASED → OBSOLETE)
- [ ] Multi-level BOM API
- [ ] Change Request API
- [ ] JWT authentication
- [ ] Unit + integration tests

## Phase 2 — 3D Web Viewer (2–3 weeks)
- [ ] React app scaffold (Create React App or Vite)
- [ ] Three.js integration
- [ ] glTF loader
- [ ] Orbit controls (zoom, pan, rotate)
- [ ] Object tree panel
- [ ] Part highlight on click
- [ ] Optional: Section view
- [ ] Optional: Exploded view

## Phase 3 — CAD File Handling (part of Phase 2)
- [ ] File upload API (multipart)
- [ ] MinIO integration (local)
- [ ] STEP → glTF conversion pipeline
- [ ] S3 integration (AWS)

## Phase 4 — DevOps + Cloud (3 weeks)
- [ ] Dockerfile for backend
- [ ] Dockerfile for frontend
- [ ] docker-compose.yml (local stack)
- [ ] GitHub Actions: CI (test + build)
- [ ] GitHub Actions: CD (push image + deploy to EC2)
- [ ] AWS EC2 setup
- [ ] Nginx reverse proxy
- [ ] HTTPS with Let's Encrypt

## Phase 5 — Advanced Features (ongoing)
- [ ] RabbitMQ / Kafka event bus
- [ ] Microservices refactor
- [ ] Prometheus + Grafana monitoring
- [ ] API Gateway
- [ ] Performance testing

## Portfolio Deliverables
- [ ] Architecture diagram (draw.io)
- [ ] Live demo on AWS
- [ ] Screenshots + video walkthrough
- [ ] Lessons learned write-up
