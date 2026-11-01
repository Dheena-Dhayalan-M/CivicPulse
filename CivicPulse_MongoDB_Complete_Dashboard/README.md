# CivicPulse — Complete Dashboards + MongoDB Atlas

This package combines the full working Admin, HOD, Employee and Citizen dashboards with the MongoDB Atlas persistence implementation from the MongoDB edition. The Atlas database name and collection names are preserved.

## Run on Windows

1. Install JDK 17 or later.
2. Open PowerShell in this project folder.
3. Set your MongoDB Atlas connection string in the current PowerShell window (do not share it):

   ```powershell
   $env:CIVICPULSE_MONGODB_URI = Read-Host "Paste your MongoDB Atlas URI"
   ```

   Use your own Atlas database user's password. Do not put the URI in source files or screenshots.
4. Run `run.bat`. The first run downloads the MongoDB Java driver into `lib/` if it is not already present, then compiles and starts the application. Internet access is needed for this one-time driver download.
5. Open `http://localhost:8082`. Keep the terminal open while using the website.

## MongoDB content — unchanged

- Database: `CivicPulse`
- Collections: `accounts`, `complaints`
- Complaint history remains an array inside each complaint document.
- Existing Atlas documents are loaded at startup and updated using upserts. The application does not drop or rename collections.
- Existing local `data/accounts.db` and `data/complaints.db` files are retained as migration/fallback copies. If an Atlas collection is empty, existing local records are imported into it.
- If Atlas cannot connect, the application falls back to the local `.db` files and prints the reason in the terminal. Check the Atlas database username/password and Network Access IP allowlist if authentication fails.

## Dashboards and permissions

- Citizen: register, submit complaints, view own complaint records and history.
- HOD: view department complaints, assign one employee, manage department employees and reports.
- Employee: view only complaints assigned to that employee and update ASSIGNED → IN_PROGRESS → RESOLVED.
- Admin: government-wide statistics, complaints, accounts, audit and employee history.

## Demo accounts

- Admin: `admin` / `admin123`
- HODs: `waterhod`, `roadhod`, `electricityhod`, `sanitationhod` / `hod123`
- Employees: `water1` / `water123`, `road1` / `road123`, `electricity1` / `electricity123`, `sanitation1` / `sanitation123`
- Citizen: `citizen1` / `citizen123`

Change demo passwords before any real deployment. This is a college demonstration project, not a production-hardened public service.

## Useful files

- `src/civicpulse/Main.java` — HTTP server, API, roles, dashboards' backend and workflow.
- `src/civicpulse/MongoStore.java` — MongoDB Atlas connection and persistence.
- `web/` — complete frontend dashboards.
- `MONGODB_SETUP.md` — Atlas setup steps.
- `data/` — local fallback/migration data.
