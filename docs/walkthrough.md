# Walkthrough - Added System Audit Trail Logging & Search Scalability Optimizations

This walkthrough tracks the implementation of the database-backed audit trail logging, production profile configurations, custom error routing, and search performance optimizations.

---

## Verification Results & Evidence

### ⚡ Search Scalability (GIN Trigram Indexing)
To ensure the portal remains fast when searching through hundreds of thousands of files, we replaced the standard B-Tree index with **Generalized Inverted Index (GIN) Trigram indexing** on `title` and `filename` columns. 

We verified the database execution plan using `EXPLAIN` with sequential scans disabled (`SET enable_seqscan = off`) to simulate a high-volume dataset:
* **Title Search**: Confirmed it uses `Bitmap Index Scan on idx_documents_title_trgm`
* **Filename Search**: Confirmed it uses `Bitmap Index Scan on idx_documents_filename_trgm`

```
SET
                                       QUERY PLAN                                       
----------------------------------------------------------------------------------------
 Bitmap Heap Scan on documents  (cost=30.54..34.55 rows=1 width=1074)
   Recheck Cond: ((title)::text ~~* '%security%'::text)
   ->  Bitmap Index Scan on idx_documents_title_trgm  (cost=0.00..30.54 rows=1 width=0)
         Index Cond: ((title)::text ~~* '%security%'::text)
```

---

### 🔒 Access Control Verification (403 Page)
An employee user (`employeea`) attempting to access the administrator endpoint directly (`http://localhost:8080/admin/audit-logs`) is blocked. They are presented with the custom Access Denied screen instead of a default Spring error:

![Access Denied 403 Screen](access_denied_403_1785717648454.png)

---

### 📋 Audit Trail Log Verification (Admin View)
Once the employee user downloads a file ("Code review file", ID 5) and the admin user (`adminuser`) views the Audit Logs dashboard, the log entry for the operation appears successfully:

![Audit Log Download Entry](audit_logs_download_entry_1785717776223.png)

---

### 🎥 Browser Verification Session Recording
You can watch the full automated browser verification session here:

![Browser Manual Test Run](audit_logs_testing_1785717538032.webp)

---

## Changes Made

### 📂 Database Migrations
* **[V2__Create_Audit_Logs_Table.sql](src/main/resources/db/migration/V2__Create_Audit_Logs_Table.sql)**: Created the `audit_logs` table schema along with optimization indices.
* **[V3__Create_Trigram_Search_Indexes.sql](src/main/resources/db/migration/V3__Create_Trigram_Search_Indexes.sql)**: Enabled `pg_trgm` extension and created GIN trigram indexes on `title` and `filename` for scalable wildcard search matching.

### 📂 Models & JPA Layer
* **[AuditLog.java](src/main/java/com/enterprise/knowledgehub/model/AuditLog.java)**: Built the `AuditLog` database entity carrying compliance tracking properties.
* **[AuditLogRepository.java](src/main/java/com/enterprise/knowledgehub/repository/AuditLogRepository.java)**: Created repository queries to fetch all logs, filter by username, and retrieve recent entries.

### 📂 Service Layer & Business Integration
* **[AuditLogService.java](src/main/java/com/enterprise/knowledgehub/service/AuditLogService.java)** & **[AuditLogServiceImpl.java](src/main/java/com/enterprise/knowledgehub/service/impl/AuditLogServiceImpl.java)**: Implemented transactional audit logger service.
* **[DocumentServiceImpl.java](src/main/java/com/enterprise/knowledgehub/service/impl/DocumentServiceImpl.java)**: Injected `AuditLogService` and integrated log operations for Uploads, Downloads, and Deletions.

### 📂 Security & Profile Configurations
* **[SecurityConfig.java](src/main/java/com/enterprise/knowledgehub/config/SecurityConfig.java)**: Locked down `/admin/**` endpoints so that only users with the `ADMIN` role can access the audit logs.
* **[application-prod.yml](src/main/resources/application-prod.yml)**: Created production configuration profile enforcing environment secrets injection.
* **[docker-compose.yml](docker-compose.yml)**: Removed PostgreSQL fallback defaults and enabled production profile in Spring Boot container.

### 📂 Controllers & UI Views
* **[AdminController.java](src/main/java/com/enterprise/knowledgehub/controller/AdminController.java)**: Created controller mapped to `/admin/audit-logs` that retrieves paginated and filtered logs.
* **[CustomErrorController.java](src/main/java/com/enterprise/knowledgehub/controller/CustomErrorController.java)**: Custom global error routing logic resolving 403, 404, and 500 error views.
* **[audit-logs.html](src/main/resources/templates/admin/audit-logs.html)**: Designed Thymeleaf audit trail logs dashboard.
* **[layout.html](src/main/resources/templates/layout.html)**: Added restricted sidebar menu link.
* **[README.md](README.md)**: Updated architecture layer diagrams, security logs sections, and trigram indexing details.

### 📂 Automated Unit Testing
* **[AuditLogServiceImplTest.java](src/test/java/com/enterprise/knowledgehub/service/impl/AuditLogServiceImplTest.java)**: Unit tests confirming logging behavior and empty/whitespace filter edge cases.
