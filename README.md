# Content Tree Application

Application allows to create hierarchical tree structure where each node
contains name and content. The tree can be reorganized using drag and drop
functionality.

## Prerequisites

- Java 21+
- Node.js 20+
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

### Running Tests

```bash
cd backend
./mvnw test
```

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
storage and writes to `data/tree.json` on every change. On startup, if no
data file exists, sample data is initialized automatically.

### Key Design Decisions

- **JSON file storage** instead of database - the spec allows it, and for a
  tree structure JSON is actually a natural fit. In production would use a
  proper database with recursive CTEs.
- **Recursive delete** - when deleting a non-leaf node, all children are
  deleted recursively. This is handled in the repository layer.
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
