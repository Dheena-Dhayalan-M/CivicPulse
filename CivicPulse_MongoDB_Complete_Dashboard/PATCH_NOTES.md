CivicPulse Complete Dashboard Fixes - 22-10-2026

1. Employee dashboard
- Removed Print / PDF from the employee dashboard.
- Removed HOD Pending Complaints and all unassigned complaint visibility.
- Employees now receive only complaints explicitly assigned to their own username.
- Employee action buttons filter My Complaints, Active, Assigned, In Progress and Resolved work.
- Status workflow remains ASSIGNED -> IN_PROGRESS -> RESOLVED.

2. HOD dashboard
- Removed the large non-functional menu.
- Replaced it with compact working dashboard actions.
- Added Download Complaint Report for all department complaints.
- Added Download Employee Work Report for assigned employee work.
- Reports include employee, current status and complete assignment/status history.
- HOD can still assign one complaint to one department employee and create department employees.

3. Admin dashboard
- Retained all-department complaint filtering and employee history.
- Employee history reports include employee details and complaint status history.
- PDF/PNG report actions remain available.

4. Backend security
- Employee API visibility is restricted to complaints assigned to that employee.
- HOD assignment remains department-restricted and locks the complaint to one employee.
- Status updates remain restricted to the assigned employee.

5. Validation
- Java source compiled successfully with JDK 17.
- JavaScript passed node --check.
- PDF endpoint returned a valid %PDF-1.4 document ending with %%EOF.
- Employee API was verified to return only assigned complaints.
- ASSIGNED -> IN_PROGRESS -> RESOLVED workflow was exercised and verified, then test data was restored.
