# Architecture Notes

## High-Level Architecture

```
Frontend (React + Three.js)
        ↓  REST/JSON
Backend API (Spring Boot)
        ↓
   PostgreSQL DB
        ↓
Object Storage (S3 / MinIO)
```

## CAD File Flow

```
User uploads STEP file
        ↓
Backend stores raw file → S3/MinIO (bucket: cad-files/raw/)
        ↓
Conversion job triggered (OpenCascade CLI or FreeCAD headless)
STEP → glTF
        ↓
glTF stored → S3/MinIO (bucket: cad-files/gltf/)
        ↓
Frontend fetches glTF URL → Three.js renders it in browser
```

## Event-Driven (Advanced)

```
Item revised → publish event to RabbitMQ/Kafka
File uploaded → publish event → conversion worker consumes it
```

## Microservices Split (Senior Level)

```
API Gateway
  ├── Item Service    (port 8081)
  ├── BOM Service     (port 8082)
  ├── File Service    (port 8083)
  └── 3D Service      (port 8084)
```

## Security

- JWT tokens issued at login
- Role-based access: ADMIN, ENGINEER, VIEWER
- All API endpoints protected except `/auth/**`

## Monitoring Stack

- **Prometheus** — metrics scraping from Spring Boot Actuator
- **Grafana** — dashboards for API latency, DB connections, storage usage
- **Loki** — log aggregation
