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
