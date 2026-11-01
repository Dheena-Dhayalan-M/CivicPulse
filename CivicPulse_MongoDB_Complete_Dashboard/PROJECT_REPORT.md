# CivicPulse Project Report — HOD Routing Edition

## Core flow
Citizen → Department → HOD Dashboard → HOD Assignment → One Employee → IN_PROGRESS → RESOLVED.

## Roles
- Citizen: registers and tracks own complaints.
- Employee: sees department complaints and works only on HOD-assigned complaints.
- HOD: receives department complaints, assigns employees, monitors team workload/history, audits and reports.
- Admin: monitors all departments, accounts, complaints, employee histories, capacity and reports.

## Employee history
HOD dashboards show the respective department's employee history, including total, active and resolved work plus complaint-level history. Admin shows the same information across all departments.

## Reports
Reports support browser Print / Save as PDF and PNG snapshots for complaint records, department reports and employee history.

## Database readiness
The application runs with local file storage for zero-dependency demonstration. JDBC/MySQL schema and configuration guidance are provided in `database/` for later database connectivity.

## Final Dashboard Revision
The final revision removes visible demo credentials from login, removes HOD creation from Admin, moves employee account creation to the respective HOD, keeps one fixed HOD per department, and uses a compact action bar instead of a large non-functional menu. HODs receive new citizen complaints in their department inbox and assign exactly one employee. Employee history remains department-private for HODs and global for Admin. Report actions were hardened for Print/Save PDF and PNG download.
