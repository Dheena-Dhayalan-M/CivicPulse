# CivicPulse Database Connectivity (JDBC-ready)

The supplied demo runs with **Java 17 + built-in HttpServer only**, so it works without Maven and without an external database server.

The project is also prepared for a later JDBC/MySQL connection. The current web screens already use a clean API layer, so the storage implementation can be switched without redesigning the frontend.

## Later: connect MySQL
1. Install MySQL 8+.
2. Run `schema.sql` in MySQL Workbench.
3. Put a MySQL JDBC driver JAR in a new `lib` folder, for example:
   `lib/mysql-connector-j-<version>.jar`
4. Copy `database.properties.example` to `database.properties` and set your database URL, username and password.
5. Replace the file repository calls in `Main.java` (`loadAccounts/saveAccounts/loadComplaints/saveComplaints`) with JDBC repository methods. The API endpoints and frontend do not need to change.

The demo deliberately keeps file persistence as the default so the project remains immediately runnable on a clean Windows PC with only JDK 17 installed.

## Suggested production tables
- `users` - citizen, employee, HOD and admin accounts
- `complaints` - complaint master record and current assignment/status
- `complaint_history` - audit trail for every action
- `departments` - Water, Road, Electricity, Sanitation
- `locations` - the five fixed service locations
