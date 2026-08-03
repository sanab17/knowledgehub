# KnowledgeHub - Enterprise Document Management Portal

KnowledgeHub is a production-quality, enterprise-grade internal knowledge management portal designed for companies to securely upload, organize, search, and download business documents (PDF and DOCX).

The application is built following clean architecture, Spring Boot 3 best practices, and is designed with future enterprise enhancements (e.g., AI document summaries, RAG, Redis caching, S3 storage) in mind.

---

## 🛠 Tech Stack
- **Backend:** Java 21, Spring Boot 3.3.x, Spring Security (Form Login, BCrypt hashing), Spring Data JPA, Hibernate, PostgreSQL, Flyway, Maven, Actuator
- **Frontend:** Thymeleaf, Bootstrap 5, FontAwesome (UI Icons)
- **Deployment:** Docker, Docker Compose

---

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- *Or* Java 21 & Maven 3.9+ & local PostgreSQL instance running

### Option 1: Run with Docker Compose (Recommended)
Starts the PostgreSQL database and the web application instantly:
1. Create a `.env` file from the example:
   ```bash
   cp .env.example .env
   ```
2. Build and start all services:
   ```bash
   docker compose up --build -d
   ```
3. The application will be running at [http://localhost:8080](http://localhost:8080).

### Option 2: Run Locally (Development)
Build and run the application locally (connects to database on `localhost:5432`):
1. Start the PostgreSQL database container only (or run your local PostgreSQL service):
   ```bash
   docker compose up -d db
   ```
2. Build the package:
   ```bash
   mvn clean package
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

---

## 🔑 Access Roles & Authorization
The system supports two core roles:
- **`ADMIN`**: Can view all documents, upload, download, and delete **any** document in the system.
- **`EMPLOYEE`**: Can search, view, and download all company documents, but can **only delete** documents that they personally uploaded.

---

## 📂 Project Architecture

The application is built following clean architecture and Spring Boot 3 best practices, separating concerns into strict layers.

### Visual Architecture Diagram

![KnowledgeHub Architecture Diagram](docs/architecture_diagram.png)

### Component & Data Flow Diagram (Interactive)

```mermaid
graph TD
    classDef layer fill:#e1f5fe,stroke:#0288d1,stroke-width:1px;
    classDef external fill:#efebe9,stroke:#5d4037,stroke-width:1px;
    classDef security fill:#e8f5e9,stroke:#2e7d32,stroke-width:1px;
    classDef client fill:#fff8e1,stroke:#f57f17,stroke-width:1px;

    Client["🌐 Client (Browser)<br/>Thymeleaf & Bootstrap 5"]:::client

    subgraph App ["KnowledgeHub Core Application"]
        Security["🔒 Spring Security Layer<br/>Session Management, BCrypt, CSRF"]:::security
        
        subgraph Controllers ["Controller Layer (MVC)"]
            AuthCtrl["AuthController"]:::layer
            DashCtrl["DashboardController"]:::layer
            DocCtrl["DocumentController"]:::layer
        end

        subgraph DTOs ["DTO & Exception Layer"]
            DTO["Request/Response DTOs<br/>(Validation Constraints)"]:::layer
            GlobalExc["GlobalExceptionHandler"]:::layer
        end

        subgraph Services ["Service Layer (Business Logic)"]
            UserServiceImpl["UserServiceImpl"]:::layer
            DocServiceImpl["DocumentServiceImpl"]:::layer
            LocalStorage["LocalStorageService"]:::layer
        end

        subgraph Repositories ["Repository Layer (JPA)"]
            UserRepo["UserRepository"]:::layer
            DocRepo["DocumentRepository"]:::layer
        end

        subgraph Models ["Model Layer (Entities)"]
            UserEnt["User Entity"]:::layer
            DocEnt["Document Entity"]:::layer
        end
    end

    subgraph Data ["Data & Storage Layer"]
        DB[("🛢️ PostgreSQL Database<br/>(Flyway Migrations)")]:::external
        FS[("📁 Local Storage<br/>(uploads/ directory)")]:::external
    end

    %% Interactions
    Client ==>|HTTP Requests| Security
    Security ==>|Dispatches to| Controllers
    Controllers -.->|Binds & Validates| DTO
    Controllers ==>|Invokes Business Services| Services
    DocServiceImpl ==>|Saves/Reads Binary Data| LocalStorage
    LocalStorage ==>|File Read/Write| FS
    DocServiceImpl ==>|Queries/Mutates Data| DocRepo
    UserServiceImpl ==>|Queries/Mutates Data| UserRepo
    UserRepo ==>|ORM Mapping| UserEnt
    DocRepo ==>|ORM Mapping| DocEnt
    UserEnt & DocEnt ==>|Read/Write| DB

    %% Direct links / error handling
    Controllers -.->|Intercepts exceptions| GlobalExc
```

### Layer Descriptions
The project is structured with strict layering:
- `com.enterprise.knowledgehub.model`: Entities (`User`, `Document`, `AuditLog`) and Enums (`Department`, `UserRole`)
- `com.enterprise.knowledgehub.repository`: Repositories (`UserRepository`, `DocumentRepository`, `AuditLogRepository` supporting JPA specifications)
- `com.enterprise.knowledgehub.dto`: Unified request/response data transfer objects with validation constraints
- `com.enterprise.knowledgehub.service`: Interfaces and implementations decoupling storage (`StorageService`), authorization, search filters, statistics, and auditing (`AuditLogService`)
- `com.enterprise.knowledgehub.controller`: Clean MVC controllers for Auth, Dashboard, Document management, Admin audit trails (`AdminController`), and global error handling (`CustomErrorController`)
- `com.enterprise.knowledgehub.exception`: Custom domain exceptions and a `GlobalExceptionHandler` mapping errors to beautiful views

---

## ⚡ Search Scalability & Indexing

To handle searches across hundreds of thousands of files efficiently, the repository utilizes **PostgreSQL Trigram (pg_trgm) indexing**:
- **Substring Wildcards**: Standard B-Tree indexes cannot accelerate queries with leading wildcards (such as `LIKE '%security%'`).
- **GIN Trigram Indexes**: We utilize GIN (Generalized Inverted Index) with `gin_trgm_ops` on the document's `title` and `filename` fields.
- **Performance**: This decreases search lookup times from linear table scans ($O(N)$) to index lookups ($O(\log N)$), ensuring fast searches over high-volume databases.

---



## 📈 Monitoring & Health Check
Spring Boot Actuator health check is available at:
[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🔒 Security Measures
- **Password Protection:** Encrypted at registration using BCrypt password hashing.
- **Session Protection:** All routes except login/registration and public assets require an active HTTP Session.
- **CSRF:** CSRF tokens are automatically managed and checked by Spring Security for post requests.
- **Delete Authorization:** Enforced programmatically at the service layer preventing employees from deleting documents they do not own.
- **Access Control Restriction:** `/admin/**` endpoints are restricted to the `ADMIN` role, returning HTTP 403 Forbidden to regular employees.
- **Audit Trail Logging:** All core document operations (upload, download, deletion) are audited in a persistent database table (`audit_logs`) for compliance. Deletion audits preserve document titles in log records after data is deleted.

---

## 🔮 Future Architecture & Modular Design
To support upcoming features without breaking the core codebase:
1. **Cloud File Storage:** Decoupled through the `StorageService` interface. Implement `S3StorageService` to transition from local file storage to AWS S3.
2. **AI, Virus Scanning & Email Services:** Hooked into Spring's Event System. An event listener can listen to `DocumentUploadedEvent` to process files in the background, run AI summary extractions, index vectors for RAG, execute virus scans, and trigger email alerts.

---

## 🛡️ Production Security & Deployment Configuration

For production deployments, the application enforces database credentials safety using the Spring Boot `prod` profile. 

### 1. Spring Profiles (`default` vs. `prod`)
- **Default Profile (`default`)**: Intended for local development. Standard database fallbacks (`postgres` password, localhost) are active, allowing easy onboarding.
- **Production Profile (`prod`)**: Enforces strict security by disabling fallback values. The application **will fail to start** if database connection parameters are missing from the host environment.

### 2. Running in Production

#### Via Docker Compose (Recommended)
Our production `docker-compose.yml` uses the `prod` profile and requires your environment to contain the DB secrets.
1. Populate your `.env` with secure database parameters (avoid using `postgres` as username/password):
   ```bash
   DB_HOST=db
   DB_PORT=5432
   DB_NAME=your_secure_db_name
   DB_USERNAME=your_secure_username
   DB_PASSWORD=your_super_secret_password
   ```
2. Start the services:
   ```bash
   docker compose up --build -d
   ```

#### Via Maven / Direct Execution
To run the production profile directly:
1. Define the system environment variables:
   ```bash
   export DB_HOST=your-prod-db-host
   export DB_PORT=5432
   export DB_NAME=your_prod_db_name
   export DB_USERNAME=your_prod_username
   export DB_PASSWORD=your_prod_password
   ```
2. Launch the jar with the `prod` profile:
   ```bash
   java -jar -Dspring.profiles.active=prod target/knowledgehub-0.0.1-SNAPSHOT.jar
   ```

---

## 🔧 Troubleshooting & Common Issues

### 1. Web server failed to start: Port 8080 was already in use
* **Cause:** Another process (often a running Docker container or another local server) is already listening on port `8080`.
* **Fix (Docker):** Stop the conflicting container or run `docker compose down` inside the project root to stop the compose stack.
* **Fix (Port Check):** Run `lsof -i :8080` to find the process ID (PID) and terminate it with `kill -9 <PID>`.
* **Fix (Custom Port):** Configure a different port in your environment:
  * In Docker: Change `SERVER_PORT` in your `.env` file.
  * In Maven: Run the application with a port override:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
    ```

### 2. Unable to obtain connection from database (Connection refused)
* **Cause:** The local Maven execution (`mvn spring-boot:run`) is trying to connect to the database on `localhost:5432`, but no database service is active on that port.
* **Fix (Docker Database):** If you want to run the application locally but use Docker for the database, start *only* the database service first:
  ```bash
  docker compose up -d db
  ```
* **Fix (Local PostgreSQL):** Ensure your local PostgreSQL service is running and has a database named `knowledgehub` matching the credentials in `application.yml`.

### 3. Cannot connect to the Docker daemon at unix:///var/run/docker.sock
* **Cause:** The Docker Desktop application (or the Docker daemon VM) is not running on your host machine.
* **Fix (Mac):** Open the `/Applications` directory and launch the **Docker** (or **Docker Desktop**) application, then wait for it to initialize.
* **Fix (Command Line):** Run the following command in your terminal to start Docker Desktop on macOS:
  ```bash
  open /Applications/Docker.app
  ```
