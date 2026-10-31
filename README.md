# Content Tree Application

Application allows to create hierarchical tree structure where each node
contains name and content. The tree can be reorganized using drag and drop
functionality.

## Tech Stack

| Part | Versions |
|------|----------|
| Backend | Java 21, Spring Boot 4.1, Jackson 3, Maven 3.9 (wrapper) |
| Frontend | Angular 22 (standalone components, signals, zoneless), Angular CDK 22, TypeScript 6.0, RxJS 7.8 |
| Tests | JUnit 5 + Mockito + MockMvc, Vitest 5 + jsdom |
| Tooling | ESLint 10 with angular-eslint, Prettier config in `frontend/package.json` |

## Prerequisites

- Java 21+
- Node.js 22.22+ or 24+ (required by Angular CLI 22) with npm
- Maven (or use the included wrapper `./mvnw`)

## How to Run

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts on http://localhost:8080

### Frontend

```bash
cd frontend
npm install
npm start
```

The frontend starts on http://localhost:4200

### Configuration

The backend reads the following settings (environment variable or the
matching property in `application.properties`):

| Environment variable | Property | Default | Description |
|----------------------|----------|---------|-------------|
| `CONTENT_TREE_DATA_FILE` | `content-tree.data-file` | `data/tree.json` | JSON file the tree is persisted to |
| `CONTENT_TREE_CORS_ALLOWED_ORIGINS` | `content-tree.cors-allowed-origins` | `http://localhost:4200` | Comma separated origins allowed to call `/api/**` |
| `SERVER_PORT` | `server.port` | `8080` | HTTP port |

The frontend API URL is set in `frontend/src/environments/`.

### Running Tests

```bash
cd backend
./mvnw verify          # unit + integration tests

cd frontend
npm test -- --watch=false
npm run lint
npm run build          # production build into dist/
```

### CI

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and
on pull requests: backend `./mvnw verify`, frontend lint, unit tests,
production build and `npm audit` of the production dependencies.

## Architecture Overview

Backend uses layered architecture based in Spring Boot conventions:

```
com.ptc.contenttree/
├── config/          - CORS and application config
├── controller/      - REST endpoints
├── dto/             - Request/Response objects
├── exception/       - Error handling
├── model/           - TreeNode entity
├── repository/      - JSON file persistence
└── service/         - Business logic and tree operations
```

The tree data is persisted into JSON file for sake of simplicity during
development. The `TreeRepository` uses a `ConcurrentHashMap` for in-memory
storage and writes to `data/tree.json` on every change. The file is written
atomically (temp file + move), and if it cannot be parsed on startup the
application refuses to start instead of overwriting it with an empty tree.
The application starts with an empty tree as specified in the requirements.

### Key Design Decisions

- **JSON file storage** instead of database - the spec allows it, and for a
  tree structure JSON is actually a natural fit. In production would use a
  proper database with recursive CTEs.
- **Tree rebuild on every request** - the service rebuilds the full tree
  from a snapshot of the flat node map on each call (in linear time, without
  touching the stored nodes). With larger datasets I would add caching to
  avoid this overhead.
- **Structural rules** - there is exactly one root, the root cannot be
  deleted, and a node can never be moved (or edited) under itself or one of
  its descendants. Mutating operations are serialized so these checks and the
  write happen atomically.
- **Recursive delete** - when deleting a non-leaf node, all children are
  deleted recursively. This is handled in the repository layer.
- **Sample data available** - the app starts with empty tree per spec. If you
  want to quickly test with some data, copy the included seed file:
  `mkdir -p backend/data && cp backend/src/main/resources/sample-tree.json backend/data/tree.json`
  and restart (the path is relative to the directory the backend runs in).
- **Search with match flags** - the search endpoint returns the full tree
  structure with `isMatch` boolean on each node. The frontend uses this to
  show non-matching nodes in gray while keeping them visible for context.

### Frontend Architecture

```
frontend/src/app/
├── components/
│   ├── tree-view/       - Main layout with tree and content area
│   ├── tree-node/       - Recursive tree node component
│   ├── content-panel/   - Selected node content display
│   ├── node-dialog/     - Create/edit dialog
│   └── delete-confirm/  - Delete confirmation with affected nodes
├── models/              - TypeScript interfaces
└── services/            - HTTP service for backend communication
```

For the frontend we use Angular with CDK library for drag and drop support.
Components are standalone, use signal inputs/outputs and the default OnPush
change detection; the node dialog is a typed reactive form with the same
limits as the backend.
The application was tested with Chrome and Firefox browsers.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/tree | Get full tree structure |
| GET | /api/tree/{id} | Get node by ID |
| POST | /api/tree | Create new node |
| PUT | /api/tree/{id} | Update existing node |
| DELETE | /api/tree/{id} | Delete node (recursive) |
| POST | /api/tree/move | Move node to new parent |
| GET | /api/tree/search?query= | Search with match flags |

Validation: `name` and `content` are required and limited to 255 and 10000
characters.

### Error responses

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)
problem details. The message is in `detail`; validation errors also carry an
`errors` object keyed by field name:

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "Name is required",
  "errors": { "name": "Name is required" }
}
```

| Status | When |
|--------|------|
| 400 | Validation error, unknown parent, second root, move/edit that would create a cycle |
| 404 | Node not found |
| 409 | Deleting the root node |
