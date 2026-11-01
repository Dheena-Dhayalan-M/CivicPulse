const $=s=>document.querySelector(s), app=$('#app');
let token=localStorage.getItem('civicpulse_token')||'', me=null, meta=null, data=[];

async function api(path,opt={}){opt.headers=Object.assign({'Content-Type':'application/x-www-form-urlencoded'},opt.headers||{});if(token)opt.headers.Authorization='Bearer '+token;const r=await fetch(path,opt);const t=await r.text();let j={};try{j=JSON.parse(t)}catch{}if(!r.ok)throw new Error(j.error||t||'Request failed');return j}
function esc(s){return String(s??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]))}
function form(obj){return Object.entries(obj).map(([k,v])=>encodeURIComponent(k)+'='+encodeURIComponent(v??'')).join('&')}
function shell(content){app.innerHTML=`<header class="top"><div class="brand">Civic<span>Pulse</span></div><div class="topright">${me?`<span class="userchip"><b>${esc(me.name)}</b><small>${esc(roleLabel(me.role))}${me.department?' · '+esc(me.department):''}</small></span><button class="btn secondary" onclick="logout()">Logout</button>`:'<small>Government Grievance Management & Priority Resolution</small>'}</div></header>${content}`}
function roleLabel(r){return r==='ADMIN'?'System Administrator':r==='HOD'?'Head of Department':r==='EMPLOYEE'?'Government Employee':'Citizen'}
async function boot(){try{meta=await api('/api/meta');if(token){const r=await api('/api/me');me=r.user;renderDashboard()}else renderHome()}catch(e){renderHome()}}

function renderHome(){shell(`<main class="wrap hero"><section class="hero-copy"><div class="eyebrow">SMART CIVIC SERVICE PORTAL</div><h1>Government grievances,<br><span>organized by priority.</span></h1><p>CivicPulse gives citizens a clean complaint portal, employees a department work queue, HODs a team-control dashboard and administrators a complete government-wide view.</p><div class="hero-actions"><button class="btn primary big" onclick="loginPage('CITIZEN')">Login to Portal</button><button class="btn secondary big" onclick="registerPage()">Citizen Registration</button></div><div class="feature-strip"><div><b>5</b><span>Fixed locations</span></div><div><b>4</b><span>Departments</span></div><div><b>30</b><span>Records</span></div><div><b>4</b><span>Access roles</span></div></div></section><aside class="roles"><div class="role-head"><div><span class="eyebrow">SECURE ACCESS</span><h2>Choose your workspace</h2></div><span class="shield">✓</span></div>${roleCard('Citizen','Raise and track your own grievances.','CITIZEN','User')}${roleCard('Government Employee','Work on HOD-assigned complaints and move cases to resolution.','EMPLOYEE','Employee')}${roleCard('Head of Department','Monitor your department, team workload and assignments.','HOD','HOD')}${roleCard('System Administrator','Global monitoring, accounts, capacity and reports.','ADMIN','Admin')}</aside></main>`)}
function roleCard(title,desc,role,label){return `<div class="role"><div class="role-icon">${label[0]}</div><div class="role-text"><b>${title}</b><small>${desc}</small></div><button class="btn secondary" onclick="loginPage('${role}')">Login</button></div>`}

function loginPage(role){shell(`<main class="auth-pro"><section class="login-visual"><div class="eyebrow">CIVICPULSE SECURE LOGIN</div><h1>${roleLabel(role)}<br><span>Workspace</span></h1><p>Sign in with your authorized CivicPulse account. Your dashboard and data access are selected from your account role.</p><div class="login-points"><span>✓ Server-side role validation</span><span>✓ Department-level privacy</span><span>✓ Complaint audit history</span></div></section><section class="card login-card"><div class="login-logo">C<span>P</span></div><h2>Welcome back</h2><p class="muted">${esc(roleLabel(role))} login</p><div id="msg"></div><div class="field"><label>Username</label><input id="username" autocomplete="username" autofocus></div><div class="field"><label>Password</label><input id="password" type="password" autocomplete="current-password" onkeydown="if(event.key==='Enter')login('${role}')"></div><button class="btn primary wide" onclick="login('${role}')">Sign in securely</button><button class="btn ghost wide" onclick="renderHome()">← Back to role selection</button></section></main>`)}
async function login(role){try{const r=await api('/api/login',{method:'POST',body:form({username:$('#username').value,password:$('#password').value,role})});token=r.token;localStorage.setItem('civicpulse_token',token);me=r.user;renderDashboard()}catch(e){$('#msg').innerHTML=`<div class="error">${esc(e.message)}</div>`}}
function registerPage(){shell(`<main class="auth"><section class="card register-card"><div class="login-logo">C<span>P</span></div><h1>Create Citizen Account</h1><p class="muted">Register once, then use your account to raise and track grievances.</p><div id="msg"></div><div class="grid2"><div class="field"><label>Full name</label><input id="name"></div><div class="field"><label>Phone number</label><input id="phone"></div></div><div class="field"><label>Username</label><input id="username"></div><div class="grid2"><div class="field"><label>Password</label><input id="password" type="password"></div><div class="field"><label>Confirm password</label><input id="confirm" type="password"></div></div><button class="btn primary wide" onclick="register()">Create Account</button><p class="muted center"><a href="#" onclick="loginPage('CITIZEN');return false">Already registered? Login</a></p></section></main>`)}
async function register(){try{const r=await api('/api/register',{method:'POST',body:form({name:$('#name').value,phone:$('#phone').value,username:$('#username').value,password:$('#password').value,confirm:$('#confirm').value})});$('#msg').innerHTML=`<div class="success">${esc(r.message)}</div>`;setTimeout(()=>loginPage('CITIZEN'),900)}catch(e){$('#msg').innerHTML=`<div class="error">${esc(e.message)}</div>`}}
async function logout(){try{await api('/api/logout',{method:'POST'})}catch{}token='';me=null;localStorage.removeItem('civicpulse_token');renderHome()}
async function renderDashboard(){if(me.role==='ADMIN')return adminDash();if(me.role==='HOD')return hodDash();if(me.role==='EMPLOYEE')return employeeDash();return citizenDash()}
function head(title,sub,actions=''){return `<main class="wrap dash"><div class="dashhead"><div><div class="eyebrow">${esc(roleLabel(me.role)).toUpperCase()}</div><h1>${title}</h1><p>${sub}</p></div><div class="head-actions">${actions}</div></div>`}
function stat(label,value,extra=''){return `<div class="stat ${extra}"><small>${label}</small><b>${value}</b></div>`}

async function citizenDash(){data=await api('/api/complaints');const total=data.length,res=data.filter(x=>x.status==='RESOLVED').length,open=total-res;shell(head('Citizen Dashboard','Raise a grievance and track only the complaints registered from your account.')+`<div class="stats">${stat('My complaints',total)}${stat('Open',open)}${stat('Resolved',res)}${stat('Fixed locations',5)}</div><div class="cols"><section class="card"><div class="section-title"><h2>Raise Complaint</h2><span class="pill">Citizen service</span></div><div id="msg"></div>${complaintForm()}</section><section class="card"><div class="section-title"><h2>My Complaints</h2><button class="btn small secondary" onclick="citizenDash()">Refresh</button></div><div class="search"><input id="search" placeholder="Search ID, department, location..." oninput="filterCitizen()"></div><div id="complaintList" class="list">${renderItems(data,'citizen')}</div></section></div></main>`)}
function complaintForm(){return `<div class="field"><label>Department</label><select id="dept"><option value="">Select department</option>${meta.departments.map(x=>`<option>${esc(x)}</option>`).join('')}</select></div><div class="field"><label>Location</label><select id="location"><option value="">Select one of the five fixed locations</option>${meta.locations.map(x=>`<option>${esc(x)}</option>`).join('')}</select><small class="field-help">Available across all departments: Makkal Nagar · Anna Nagar · Gandhi Nagar · Nehru Nagar · Periyar Nagar</small></div><div class="grid2"><div class="field"><label>Problem started date</label><input id="startedDate" type="date" value="${new Date().toISOString().slice(0,10)}"></div><div class="field"><label>Problem started time</label><input id="startedTime" type="time" value="09:00"></div></div><div class="grid2"><div class="field"><label>Severity (1-5)</label><select id="severity"><option>1</option><option>2</option><option>3</option><option>4</option><option selected>5</option></select></div><div class="field"><label>Affected people</label><input id="affected" type="number" min="1" value="10"></div></div><div class="field"><label>Description</label><textarea id="description" placeholder="Describe the issue clearly..."></textarea></div><button class="btn primary wide" onclick="raiseComplaint()">Submit Complaint</button><div class="section info"><b>Automatic routing:</b> you choose the department; the system validates it and sends the complaint to that department queue. Only one employee can lock a complaint.</div>`}
async function raiseComplaint(){try{const r=await api('/api/complaints',{method:'POST',body:form({department:$('#dept').value,location:$('#location').value,startedDate:$('#startedDate').value,startedTime:$('#startedTime').value,description:$('#description').value,severity:$('#severity').value,affectedPeople:$('#affected').value})});$('#msg').innerHTML=`<div class="success">${esc(r.message)}</div>`;setTimeout(citizenDash,600)}catch(e){$('#msg').innerHTML=`<div class="error">${esc(e.message)}</div>`}}
function filterCitizen(){const q=$('#search').value.toLowerCase();$('#complaintList').innerHTML=renderItems(data.filter(c=>JSON.stringify(c).toLowerCase().includes(q)),'citizen')}
function reportMyRecords(){showReportList(data,'My CivicPulse Complaint Report')}

async function employeeDash(){
  const all=await api('/api/complaints');
  const cap=await api('/api/capacity');
  data=all.filter(x=>x.assignedUsername===me.username);
  const active=data.filter(x=>['ASSIGNED','IN_PROGRESS'].includes(x.status)).length;
  const resolved=data.filter(x=>x.status==='RESOLVED').length;
  shell(head('Employee Dashboard',`Department: ${esc(me.department)} · You can see only complaints assigned to you.`)+
  `<div class="stats">${stat('My assigned',data.length)}${stat('Active',active)}${stat('Resolved',resolved)}${stat('Department capacity',`${cap[0].active}/10`)}</div>
  <div class="actionbar">
    <button class="btn secondary" onclick="filterEmployeeView('ALL')">My Complaints</button>
    <button class="btn secondary" onclick="filterEmployeeView('ACTIVE')">Active Work</button>
    <button class="btn secondary" onclick="filterEmployeeView('ASSIGNED')">Assigned</button>
    <button class="btn secondary" onclick="filterEmployeeView('IN_PROGRESS')">In Progress</button>
    <button class="btn secondary" onclick="filterEmployeeView('RESOLVED')">Resolved</button>
    <button class="btn secondary" onclick="document.querySelector('.caprow')?.scrollIntoView({behavior:'smooth'})">Department Capacity</button>
  </div>
  <div class="dashboard-grid">
    <section class="card">
      <div class="section-title"><div><h2>My Work</h2><span class="muted">Only HOD-assigned complaints appear here.</span></div><button class="btn small secondary" onclick="employeeDash()">Refresh</button></div>
      <div class="info"><b>Workflow:</b> HOD assigns → ASSIGNED → employee starts → IN_PROGRESS → employee resolves → RESOLVED.</div>
      <h3>Department Capacity</h3>${capacityRows(cap)}
    </section>
    <section class="card">
      <div class="section-title"><h2>Assigned Complaints</h2><span class="pill blue">${data.length} assigned</span></div>
      <div class="search"><input id="search" placeholder="Search complaint ID, location, description..." oninput="filterEmployee()"></div>
      <div id="complaintList" class="list">${renderItems(data,'employee')}</div>
    </section>
  </div>
  </main>`)
}
let employeeViewFilter='ALL';
function filterEmployeeView(view){employeeViewFilter=view;filterEmployee();document.querySelector('#complaintList')?.scrollIntoView({behavior:'smooth',block:'start'})}
function filterEmployee(){const q=($('#search')?.value||'').toLowerCase();const list=data.filter(c=>{const statusOk=employeeViewFilter==='ALL'||(employeeViewFilter==='ACTIVE'&&['ASSIGNED','IN_PROGRESS'].includes(c.status))||c.status===employeeViewFilter;return statusOk&&JSON.stringify(c).toLowerCase().includes(q)});$('#complaintList').innerHTML=renderItems(list,'employee')}

async function hodDash(){
  data=await api('/api/complaints');
  const [statsData,team,cap,db,history]=await Promise.all([api('/api/hod/stats'),api('/api/hod/team'),api('/api/capacity'),api('/api/hod/database'),api('/api/hod/employee-history')]);
  const unassigned=data.filter(c=>!c.assignedUsername&&c.status!=='RESOLVED');
  const active=data.filter(c=>['ASSIGNED','IN_PROGRESS'].includes(c.status));
  shell(head(`${esc(me.department)} HOD Dashboard`,`Lead ${esc(me.department)} operations, receive complaints, assign work and monitor employee performance.`)+
  `<div class="stats">${stat('Department complaints',statsData.total)}${stat('New HOD inbox',unassigned.length)}${stat('Active',active.length)}${stat('Resolved',statsData.resolved)}</div>
  <div class="actionbar">
    <button class="btn secondary" onclick="filterHod('ALL')">View All Complaints</button>
    <button class="btn secondary" onclick="filterHod('UNASSIGNED')">New HOD Inbox</button>
    <button class="btn secondary" onclick="filterHod('ACTIVE')">Active Work</button>
    <button class="btn secondary" onclick="openHodAssign()">Assign Complaint</button>
    <button class="btn secondary" onclick="downloadHodComplaintReport()">Download Complaint Report</button>
    <button class="btn secondary" onclick="downloadHodEmployeeReport()">Download Employee Work Report</button>
  </div>
  <div class="dashboard-grid">
    <section class="card">
      <div class="section-title"><div><h2>New HOD Inbox</h2><span class="muted">Citizen complaints waiting for your assignment</span></div><span class="pill blue">${unassigned.length} waiting</span></div>
      <div class="list inbox-list">${unassigned.length?unassigned.slice(0,10).map(c=>`<div class="item"><div class="itemtop"><b>${esc(c.id)} · ${esc(c.department)}</b><span class="badge registered">NEW</span></div><h3>${esc(c.location)} · Severity ${c.severity}</h3><p>${esc(c.description)}</p><div class="meta">Citizen: ${esc(c.citizen)} · Affected: ${c.affectedPeople}</div><div class="actions"><button class="btn secondary" onclick="openHodAssignFor('${esc(c.id)}')">Assign to Employee</button><button class="btn secondary" onclick="showHistory('${esc(c.id)}')">Audit</button></div></div>`).join(''):'<div class="empty-state">No new complaints are waiting for assignment.</div>'}</div>
    </section>
    <section class="card">
      <div class="section-title"><div><h2>Department Team</h2><span class="muted">Only ${esc(me.department)} employees</span></div><button class="btn secondary" onclick="openCreateEmployee()">+ Add Employee</button></div>
      <div class="team-cards">${team.length?team.map(t=>`<div class="team-card"><div><b>${esc(t.name)}</b><small>${esc(t.username)}</small></div><div class="team-counts"><span><b>${t.total}</b>Total</span><span><b>${t.active}</b>Active</span><span><b>${t.resolved}</b>Resolved</span></div><div class="actions"><button class="btn small secondary" onclick="filterHodEmployee('${esc(t.username)}')">View Work</button><button class="btn small secondary" onclick="showEmployeeHistory('${esc(t.username)}')">History</button></div></div>`).join(''):'<div class="empty-state">No employees yet. Add the first employee for this department.</div>'}</div>
    </section>
  </div>
  <div class="dashboard-grid">
    <section class="card"><div class="section-title"><div><h2>Employee Work History</h2><span class="muted">Assignment and status history for this department</span></div></div>${renderEmployeeHistoryCards(history,'hod')}</section>
    <section class="card"><div class="section-title"><div><h2>Department Operations</h2><span class="muted">Capacity and database status</span></div></div><h3>Department Capacity</h3>${capacityRows(cap)}<div class="section"><h3>Database Connectivity</h3><div class="db-status"><span class="dot"></span><div><b>Local storage active · JDBC-ready</b><small>${esc(db.message)}</small></div></div></div></section>
  </div>
  <section class="card section-card"><div class="section-title"><div><h2>Department Complaints</h2><span class="muted">Priority ordered · ${esc(me.department)} only</span></div><div class="search compact"><input id="hodSearch" placeholder="Search complaint, citizen, location..." oninput="filterHod('SEARCH')"><button class="btn small secondary" onclick="hodDash()">Refresh</button></div></div><div id="hodList" class="list">${renderItems(data,'hod')}</div></section>
  </main>`)
}
function downloadHodComplaintReport(){showReportList(data,`${me.department} HOD Complaint Report`)}
function downloadHodEmployeeReport(){const groups=window.__employeeHistoryGroups||[];const records=flattenEmployeeHistory(groups);showReportList(records,`${me.department} HOD Employee Work Report`)}

function openCreateEmployee(){openModal(`<h2>Add Department Employee</h2><p class="muted">Create an employee account for ${esc(me.department)}. HODs can create employees only for their own department.</p><div id="hodEmpMsg"></div><div class="grid2"><div class="field"><label>Employee name</label><input id="hename"></div><div class="field"><label>Username</label><input id="heuser"></div></div><div class="grid2"><div class="field"><label>Password</label><input id="hepw" type="password"></div><div class="field"><label>Confirm password</label><input id="hecpw" type="password"></div></div><div class="field"><label>Department</label><input value="${esc(me.department)}" disabled></div><button class="btn secondary" onclick="createHodEmployee()">Create Employee Account</button>`)}
async function createHodEmployee(){try{const r=await api('/api/hod/employee',{method:'POST',body:form({name:$('#hename').value,username:$('#heuser').value,password:$('#hepw').value,confirm:$('#hecpw').value})});alert(r.message);document.querySelector('.modal')?.remove();hodDash()}catch(e){const m=$('#hodEmpMsg');if(m)m.innerHTML=`<div class="error">${esc(e.message)}</div>`;else alert(e.message)}}

let hodFilter='ALL';
function filterHod(v){hodFilter=v;const q=($('#hodSearch')?.value||'').toLowerCase();let list=data.filter(c=>(v==='ALL'||(v==='UNASSIGNED'&&!c.assignedUsername)||(v==='ACTIVE'&&['ASSIGNED','IN_PROGRESS'].includes(c.status))||(v==='SEARCH'))&&(!q||JSON.stringify(c).toLowerCase().includes(q)));if(v==='EMPLOYEE'){}$('#hodList').innerHTML=renderItems(list,'hod')}
function filterHodEmployee(username){hodFilter='SEARCH';const el=$('#hodSearch');if(el)el.value=username;filterHod('SEARCH');document.querySelector('#hodList')?.scrollIntoView({behavior:'smooth',block:'start'})}
async function openHodAssign(){const unassigned=data.filter(c=>!c.assignedUsername&&c.status!=='RESOLVED');const team=await api('/api/hod/team');if(!unassigned.length){openModal(`<h2>No unassigned complaints</h2><p class="muted">All current department complaints are already locked or resolved.</p>`);return}openModal(`<h2>Assign Complaint</h2><p class="muted">HOD can assign only an unassigned complaint to an employee in ${esc(me.department)}.</p><div class="field"><label>Complaint</label><select id="hodComplaint">${unassigned.map(c=>`<option value="${esc(c.id)}">${esc(c.id)} · Sev ${c.severity} · ${esc(c.location)}</option>`).join('')}</select></div><div class="field"><label>Employee</label><select id="hodEmployee">${team.map(t=>`<option value="${esc(t.username)}">${esc(t.name)} (${esc(t.username)})</option>`).join('')}</select></div><button class="btn primary" onclick="hodAssign()">Assign & Lock Complaint</button>`)}
async function hodAssign(){try{const r=await api('/api/hod/assign',{method:'POST',body:form({id:$('#hodComplaint').value,employee:$('#hodEmployee').value})});alert(r.message);document.querySelector('.modal')?.remove();hodDash()}catch(e){alert(e.message)}}
async function showHodAuditPicker(){const options=data.map(c=>`<option value="${esc(c.id)}">${esc(c.id)} · ${esc(c.location)} · ${esc(c.status)}</option>`).join('');openModal(`<h2>Audit / Escalation History</h2><p class="muted">Select a department complaint to inspect its full history.</p><div class="field"><label>Complaint</label><select id="auditComplaint">${options}</select></div><button class="btn primary" onclick="showHodAudit($('#auditComplaint').value)">View History</button>`)}
async function showHodAudit(id){try{const r=await api('/api/hod/audit?id='+encodeURIComponent(id));openModal(`<h2>Audit History · ${esc(id)}</h2>${r.history?.length?`<pre>${esc(r.history.join('\n'))}</pre>`:'<p class="muted">No audit events recorded.</p>'}`)}catch(e){alert(e.message)}}

async function adminDash(){
  const [s,c,cap,a,db,history]=await Promise.all([api('/api/admin/stats'),api('/api/complaints'),api('/api/capacity'),api('/api/admin/accounts'),api('/api/admin/database'),api('/api/admin/employee-history')]);
  data=c;
  shell(head('Administrator Dashboard','Government-wide monitoring, all departments, employee history, HOD routing, capacity, audit history and reports.',`<button class="btn secondary" onclick="showReportList(data,'CivicPulse Government-Wide Report')">Print / PDF</button>`)+
  `<div class="stats">${stat('All complaints',s.total)}${stat('Registered',s.registered)}${stat('Active',Number(s.assigned)+Number(s.inProgress))}${stat('Resolved',s.resolved)}</div>
  <div class="actionbar">
    <button class="btn secondary" onclick="filterAdminStatus('ALL')">All Complaints</button>
    <button class="btn secondary" onclick="filterAdminStatus('REGISTERED')">Priority Queue</button>
    <button class="btn secondary" onclick="filterAdminStatus('ACTIVE')">Active</button>
    <button class="btn secondary" onclick="filterAdminStatus('RESOLVED')">Resolved</button>
    <button class="btn secondary" onclick="showAdminAuditPicker()">Audit History</button>
    <button class="btn secondary" onclick="showReportList(data,'CivicPulse Government-Wide Report')">Government Report</button>
    <button class="btn secondary" onclick="showReportList(flattenEmployeeHistory(history),'CivicPulse All Department Employee History')">Employee History PDF</button>
    <button class="btn secondary" onclick="showPngReport(flattenEmployeeHistory(history),'CivicPulse All Department Employee History')">Employee History PNG</button>
  </div>
  <div class="dashboard-grid">
    <section class="card" id="adminComplaints"><div class="section-title"><div><h2 id="adminListTitle">Global Priority Queue / All Complaints</h2><span class="muted">All departments · HOD assignment controlled</span></div><button class="btn small secondary" onclick="adminDash()">Refresh</button></div><div class="info">Citizen complaints are routed to the HOD of their selected department before an employee is assigned.</div><div class="search"><input id="search" placeholder="Search complaint ID, citizen, department..." oninput="filterAdmin()"></div><div id="complaintList" class="list">${renderAdminItems(data)}</div></section>
    <section class="card"><div class="section-title"><div><h2>Government Operations</h2><span class="muted">Accounts, capacity and database</span></div></div><h3>Account Summary</h3><div class="mini-stats"><span>Citizens <b>${a.citizens}</b></span><span>Employees <b>${a.employees}</b></span><span>HODs <b>${a.hods}</b></span><span>Admins <b>${a.admins}</b></span></div><div class="section"><h3>Department Capacity</h3>${capacityRows(cap)}</div><div class="section"><h3>Database Connectivity</h3><div class="db-status"><span class="dot"></span><div><b>Local storage active · JDBC-ready</b><small>${esc(db.message)}</small></div></div></div><div class="info section"><b>Account rule:</b> one HOD is fixed for each department. HODs create and manage their own department employees.</div></section>
  </div>
  <section class="card section-card"><div class="section-title"><div><h2>All Department Employee History</h2><span class="muted">Complete work history across Water, Road, Electricity and Sanitation.</span></div><div class="actions"><button class="btn small secondary" onclick="showReportList(flattenEmployeeHistory(history),'CivicPulse All Department Employee History')">Print / PDF</button><button class="btn small secondary" onclick="showPngReport(flattenEmployeeHistory(history),'CivicPulse All Department Employee History')">PNG</button></div></div>${renderEmployeeHistoryCards(history,'admin')}</section>
  </main>`)
}

let adminStatusFilter='ALL';
function filterAdminStatus(status){adminStatusFilter=status;const names={ALL:'Global Priority Queue / All Complaints',REGISTERED:'Registered Priority Queue',ACTIVE:'Active Complaints',RESOLVED:'Resolved Complaints'};$('#adminListTitle').textContent=names[status]||names.ALL;filterAdmin();document.querySelector('#adminComplaints')?.scrollIntoView({behavior:'smooth',block:'start'})}
function filterAdmin(){const q=($('#search')?.value||'').toLowerCase();const list=data.filter(c=>(adminStatusFilter==='ALL'||(adminStatusFilter==='ACTIVE'?['ASSIGNED','IN_PROGRESS'].includes(c.status):c.status===adminStatusFilter))&&JSON.stringify(c).toLowerCase().includes(q));$('#complaintList').innerHTML=renderAdminItems(list)}
function renderAdminItems(list){return list.length?list.map(c=>`<div class="item"><div class="itemtop"><b>${esc(c.id)} · ${esc(c.department)} · Sev ${c.severity}</b><span class="badge ${c.status.toLowerCase()}">${esc(c.status)}</span></div><h3>${esc(c.location)} · Affected ${c.affectedPeople}</h3><p>${esc(c.description)}</p><div class="meta">Citizen: ${esc(c.citizen)} · Employee: ${esc(c.employee)} · Arrival: ${c.arrival}</div><div class="actions"><button class="btn secondary" onclick="showHistory('${c.id}')">Audit History</button><button class="btn secondary" onclick="showSingleReport('${c.id}')">Print / PDF</button><button class="btn secondary" onclick="showPngReport([getComplaint('${c.id}')],'Complaint ${esc(c.id)}')">PNG</button></div></div>`).join(''):'<div class="muted">No complaints match this filter.</div>'}
function getComplaint(id){return data.find(c=>c.id===id)}
function adminGo(id){const el=$('#'+id);if(el)el.scrollIntoView({behavior:'smooth',block:'start'})}
async function showAdminAuditPicker(){const options=data.map(c=>`<option value="${esc(c.id)}">${esc(c.id)} · ${esc(c.department)} · ${esc(c.location)}</option>`).join('');openModal(`<h2>Statistics & Audit History</h2><p class="muted">Select a complaint to view its complete audit history.</p><div class="field"><label>Complaint</label><select id="auditComplaint">${options}</select></div><button class="btn primary" onclick="showAdminAudit($('#auditComplaint').value)">View Audit History</button>`)}
async function showAdminAudit(id){try{const r=await api('/api/admin/audit?id='+encodeURIComponent(id));openModal(`<h2>Audit History · ${esc(id)}</h2>${r.history?.length?`<pre>${esc(r.history.join('\n'))}</pre>`:'<p class="muted">No audit events recorded yet.</p>'}`)}catch(e){alert(e.message)}}

function renderItems(list,role){if(!list.length)return `<div class="muted">No complaints found.</div>`;return list.map(c=>`<div class="item"><div class="itemtop"><b>${esc(c.id)} · ${esc(c.department)}</b><span class="badge ${c.status.toLowerCase()}">${esc(c.status)}</span></div><h3>${esc(c.location)} · Severity ${c.severity}</h3><p>${esc(c.description)}</p><div class="meta">Affected: ${c.affectedPeople} · Started: ${esc(c.date)} ${esc(c.time)}<br>Employee: ${esc(c.employee)}</div><div class="actions">${role==='employee'?employeeButtons(c):''}<button class="btn secondary" onclick="showHistory('${c.id}')">History</button>${role==='hod'?`<button class="btn secondary" onclick="openHodAssignFor('${c.id}')">Assign</button>`:''}</div></div>`).join('')}
function employeeButtons(c){let b='';if(!c.assignedUsername&&c.status!=='RESOLVED')b+=`<span class="pill">Waiting for HOD assignment</span>`;if(c.assignedUsername===me.username&&c.status==='ASSIGNED')b+=`<button class="btn secondary" onclick="statusUpdate('${c.id}','IN_PROGRESS')">Start / IN_PROGRESS</button>`;if(c.assignedUsername===me.username&&c.status==='IN_PROGRESS')b+=`<button class="btn primary" onclick="statusUpdate('${c.id}','RESOLVED')">Mark RESOLVED</button>`;b+=`<button class="btn secondary" onclick="showDuplicate('${c.id}')">Duplicate Check</button>`;return b}
async function claim(id){try{await api('/api/claim',{method:'POST',body:form({id})});employeeDash()}catch(e){alert(e.message)}}
async function statusUpdate(id,status){try{await api('/api/status',{method:'POST',body:form({id,status})});employeeDash()}catch(e){alert(e.message)}}
async function showHistory(id){try{const r=await api('/api/history?id='+encodeURIComponent(id));openModal(`<h2>Audit History · ${esc(id)}</h2>${r.history?.length?`<pre>${esc(r.history.join('\n'))}</pre>`:'<p class="muted">No audit events recorded yet.</p>'}`)}catch(e){alert(e.message)}}
async function showDuplicate(id){try{const r=await api('/api/duplicate?id='+encodeURIComponent(id));openModal(`<h2>Duplicate Candidates · ${esc(id)}</h2>${r.length?r.map(c=>`<p><b>${esc(c.id)}</b> · ${esc(c.location)} · ${esc(c.description)}</p>`).join(''):'<p class="muted">No registered same-department/same-location candidate found.</p>'}`)}catch(e){alert(e.message)}}
function capacityRows(cap){return cap.map(x=>`<div class="caprow"><b>${esc(x.department)}</b><div class="bar"><i style="width:${Math.min(100,(x.active/x.capacity)*100)}%"></i></div><span>${x.active}/${x.capacity}</span></div>`).join('')}
function openHodAssignFor(id){openHodAssign().then(()=>{const sel=$('#hodComplaint');if(sel)sel.value=id})}

function flattenEmployeeHistory(groups){
  const out=[];
  (groups||[]).forEach(g=>{
    (g.complaints||[]).forEach(c=>out.push(Object.assign({},c,{employee:c.employee||(`${g.name} (${g.username})`),employeeDepartment:g.department,employeeName:g.name,employeeUsername:g.username})));
  });
  return out;
}
function renderEmployeeHistoryCards(groups,scope){
  window.__employeeHistoryGroups=groups||[];
  if(!groups?.length)return '<p class="muted">No employees found.</p>';
  return groups.map(g=>`<div class="employee-history-card"><div class="employee-history-head"><div><b>${esc(g.name)}</b><small>${esc(g.username)} · ${esc(g.department)}</small></div><div class="mini-stats"><span>Total <b>${g.total}</b></span><span>Active <b>${g.active}</b></span><span>Resolved <b>${g.resolved}</b></span></div></div><div class="employee-history-list">${g.complaints?.length?g.complaints.map(c=>`<div class="history-row"><span><b>${esc(c.id)}</b> · ${esc(c.location)}</span><span>${esc(c.status)}</span><span>${esc(c.date)} ${esc(c.time)}</span><button class="btn small secondary" onclick="showHistory('${esc(c.id)}')">Audit</button></div>`).join(''):'<div class="muted">No complaints have been assigned to this employee yet.</div>'}</div><div class="actions"><button class="btn small secondary" onclick="showEmployeeHistory('${esc(g.username)}')">View / Print History</button></div></div>`).join('');
}
function showEmployeeHistory(username){
  const groups=window.__employeeHistoryGroups||[];
  const g=groups.find(x=>x.username===username);
  if(!g){alert('Employee history not found.');return}
  openModal(`<h2>${esc(g.name)} · Employee History</h2><p class="muted">${esc(g.department)} · ${esc(g.username)}</p><div class="mini-stats"><span>Total <b>${g.total}</b></span><span>Active <b>${g.active}</b></span><span>Resolved <b>${g.resolved}</b></span></div>${g.complaints?.length?`<div class="list">${g.complaints.map(c=>`<div class="item"><div class="itemtop"><b>${esc(c.id)} · ${esc(c.department)}</b><span class="badge ${c.status.toLowerCase()}">${esc(c.status)}</span></div><p>${esc(c.description)}</p><div class="meta">${esc(c.location)} · ${esc(c.date)} ${esc(c.time)}</div></div>`).join('')}</div>`:'<p class="muted">No assigned complaints yet.</p>'}<div class="actions"><button class="btn primary" onclick="showReportList(flattenEmployeeHistory([window.__employeeHistoryGroups.find(x=>x.username==='${esc(username)}')]),'${esc(g.name)} Employee History')">Print / PDF</button><button class="btn secondary" onclick="showPngReport(flattenEmployeeHistory([window.__employeeHistoryGroups.find(x=>x.username==='${esc(username)}')]),'${esc(g.name)} Employee History')">PNG</button></div>`)
}

function showSingleReport(id){const c=getComplaint(id);if(c)showReportList([c],`CivicPulse Complaint Report · ${c.id}`)}
function showReportList(records,title){
  if(!records?.length){alert('No records to report.');return}
  window.__reportRecords=records;window.__reportTitle=title;
  const html=reportHtml(records,title);
  openModal(`<div class="report-modal"><div class="report-toolbar"><button class="btn primary" onclick="downloadPdfReport(window.__reportTitle)">Download PDF</button><button class="btn secondary" onclick="printReport()">Print</button><button class="btn secondary" onclick="downloadPngReport(window.__reportRecords,window.__reportTitle)">Download PNG</button></div><div id="printReportArea">${html}</div></div>`)
}
function reportHtml(records,title){const total=records.length,res=records.filter(x=>x.status==='RESOLVED').length,active=records.filter(x=>['ASSIGNED','IN_PROGRESS'].includes(x.status)).length;return `<div class="report-page"><div class="report-brand"><div><b>CivicPulse</b><span>Government Grievance Management</span></div><div class="report-date">Generated ${new Date().toLocaleString()}</div></div><h2>${esc(title)}</h2><div class="report-summary"><span>Total <b>${total}</b></span><span>Active <b>${active}</b></span><span>Resolved <b>${res}</b></span></div><table class="report-table"><thead><tr><th>ID</th><th>Department</th><th>Location</th><th>Severity</th><th>Affected</th><th>Status</th><th>Employee</th><th>Employee Department</th><th>Description</th><th>Status History</th></tr></thead><tbody>${records.map(c=>`<tr><td>${esc(c.id)}</td><td>${esc(c.department)}</td><td>${esc(c.location)}</td><td>${c.severity??''}</td><td>${c.affectedPeople??''}</td><td>${esc(c.status||'')}</td><td>${esc(c.employee||c.employeeName||'UNASSIGNED')}</td><td>${esc(c.employeeDepartment||c.department||'')}</td><td>${esc(c.description||'')}</td><td>${esc((c.history||[]).join(' | '))}</td></tr>`).join('')}</tbody></table><div class="report-footer">CivicPulse · Assignment and status audit record · Generated from the current dashboard</div></div>`}
async function downloadPdfReport(title){
  try{
    const r=await fetch('/api/report/pdf?title='+encodeURIComponent(title||'CivicPulse Report'),{headers:{Authorization:token?'Bearer '+token:''}});
    if(!r.ok){const t=await r.text();throw new Error(t||'PDF generation failed.')}
    const blob=await r.blob();
    const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=(title.replace(/[^a-z0-9]+/gi,'_')||'civicpulse_report')+'.pdf';document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1500);
  }catch(e){alert('PDF download failed: '+e.message)}
}
function printReport(){
  const area=$('#printReportArea')?.innerHTML;if(!area){alert('Report preview is not available.');return}
  const frame=document.createElement('iframe');frame.style.position='fixed';frame.style.right='0';frame.style.bottom='0';frame.style.width='0';frame.style.height='0';frame.style.border='0';document.body.appendChild(frame);
  const d=frame.contentWindow.document;d.open();d.write(`<!doctype html><html><head><title>${esc(window.__reportTitle||'CivicPulse Report')}</title><style>body{font-family:Arial,sans-serif;margin:30px;color:#18263a}.report-brand{display:flex;justify-content:space-between;border-bottom:2px solid #2563eb;padding-bottom:12px}.report-brand b{font-size:28px}.report-brand span{display:block;color:#64748b}.report-summary{display:flex;gap:25px;background:#f1f5f9;padding:12px;margin:18px 0}.report-table{width:100%;border-collapse:collapse;font-size:10px}.report-table th,.report-table td{border:1px solid #dbe3ec;padding:7px;text-align:left;vertical-align:top}.report-table th{background:#eef4ff}.report-footer{margin-top:25px;color:#64748b;font-size:10px}@media print{body{margin:12mm}}</style></head><body>${area}</body></html>`);d.close();frame.contentWindow.focus();setTimeout(()=>{frame.contentWindow.print();setTimeout(()=>frame.remove(),1200)},350)
}
function downloadPngReport(records,title){
  try{
    if(!records?.length){alert('No records to export.');return}
    const canvas=document.createElement('canvas'),ctx=canvas.getContext('2d');
    const width=1800,rowH=82,headH=220,rows=Math.max(1,records.length),height=headH+rows*rowH+100;canvas.width=width;canvas.height=height;
    ctx.fillStyle='#fff';ctx.fillRect(0,0,width,height);ctx.fillStyle='#102e50';ctx.font='bold 40px Arial';ctx.fillText('CivicPulse',50,62);ctx.font='bold 26px Arial';ctx.fillText(String(title||'CivicPulse Report').slice(0,90),50,108);ctx.font='16px Arial';ctx.fillStyle='#64748b';ctx.fillText('Generated '+new Date().toLocaleString(),50,140);ctx.fillStyle='#dbe7ff';ctx.fillRect(40,165,width-80,2);
    let y=headH;ctx.font='bold 14px Arial';ctx.fillStyle='#475569';[['ID',50],['Department',150],['Location',330],['Severity',510],['Status',610],['Employee',740],['Emp. Dept.',1000],['Description',1160]].forEach(([t,x])=>ctx.fillText(t,x,y));y+=22;
    ctx.font='14px Arial';records.forEach((c,i)=>{ctx.fillStyle=i%2?'#fff':'#f8fafc';ctx.fillRect(40,y,width-80,rowH-8);ctx.fillStyle='#18263a';ctx.fillText(String(c.id||'').slice(0,14),50,y+28);ctx.fillText(String(c.department||'').slice(0,22),150,y+28);ctx.fillText(String(c.location||'').slice(0,24),330,y+28);ctx.fillText(String(c.severity??''),520,y+28);ctx.fillText(String(c.status||'').slice(0,15),610,y+28);ctx.fillText(String(c.employee||c.employeeName||'UNASSIGNED').slice(0,28),740,y+28);ctx.fillText(String(c.employeeDepartment||c.department||'').slice(0,20),1000,y+28);ctx.fillText(String(c.description||'').slice(0,58),1160,y+22);ctx.font='12px Arial';ctx.fillStyle='#64748b';ctx.fillText(String((c.history||[]).join(' | ')).slice(0,92),1160,y+44);ctx.font='14px Arial';y+=rowH});
    const a=document.createElement('a');a.href=canvas.toDataURL('image/png');a.download=(String(title||'civicpulse_report').replace(/[^a-z0-9]+/gi,'_')||'civicpulse_report')+'.png';document.body.appendChild(a);a.click();a.remove();
  }catch(e){alert('PNG download failed: '+e.message)}
}
function showPngReport(records,title){if(!records?.length){alert('No records to export.');return}window.__reportRecords=records;window.__reportTitle=title;openModal(`<h2>PNG Report Ready</h2><p class="muted">${esc(title)}</p><div class="info">The report is generated from the current dashboard data.</div><div class="actions"><button class="btn primary" onclick="downloadPngReport(window.__reportRecords,window.__reportTitle)">Download PNG</button><button class="btn secondary" onclick="showReportList(window.__reportRecords,window.__reportTitle)">Open PDF / Print</button></div>`)}

function openModal(html){const d=document.createElement('div');d.className='modal show';d.innerHTML=`<div class="modalbox">${html}<div class="modal-close"><button class="btn secondary" onclick="this.closest('.modal').remove()">Close</button></div></div>`;document.body.appendChild(d)}
boot();
