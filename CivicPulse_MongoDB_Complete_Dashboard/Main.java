package civicpulse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    static final int PORT = 8082;
    static final Path DATA = Paths.get("data");
    static final Path ACCOUNTS = DATA.resolve("accounts.db");
    static final Path COMPLAINTS = DATA.resolve("complaints.db");
    static final List<String> DEPTS = List.of("Water", "Road", "Electricity", "Sanitation");
    static final List<String> LOCATIONS = List.of("Makkal Nagar", "Anna Nagar", "Gandhi Nagar", "Nehru Nagar", "Periyar Nagar");
    static final Map<String,String> LOCATION_DEPT = Map.of(
        "Makkal Nagar", "ALL", "Anna Nagar", "ALL", "Gandhi Nagar", "ALL", "Nehru Nagar", "ALL", "Periyar Nagar", "ALL"
    );
    static final Map<String,String> DEFAULT_EMPLOYEES = Map.of(
        "water1","water123", "road1","road123", "electricity1","electricity123", "sanitation1","sanitation123"
    );
    static final Map<String,String> EMP_DEPT = Map.of(
        "water1","Water", "road1","Road", "electricity1","Electricity", "sanitation1","Sanitation"
    );
    static final Map<String,String> DEFAULT_HODS = Map.of(
        "waterhod","hod123", "roadhod","hod123", "electricityhod","hod123", "sanitationhod","hod123"
    );
    static final Map<String,String> HOD_DEPT = Map.of(
        "waterhod","Water", "roadhod","Road", "electricityhod","Electricity", "sanitationhod","Sanitation"
    );
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    static final Map<String,User> users = new LinkedHashMap<>();
    static final Map<String,Complaint> complaints = new LinkedHashMap<>();
    static final Map<String,Session> sessions = new HashMap<>();
    static int nextArrival = 1;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATA);
        if (MongoStore.connect()) {
            MongoStore.loadUsers(users); MongoStore.loadComplaints(complaints);
            if (users.isEmpty()) loadAccountsFile();        // first run: migrate old data/accounts.db
            if (complaints.isEmpty()) loadComplaintsFile(); // first run: migrate old data/complaints.db
        } else {
            System.out.println("Falling back to local file storage (data/*.db).");
            loadAccountsFile(); loadComplaintsFile();
        }
        seedAccountsIfNeeded(); seedComplaintsIfNeeded();
        saveAccounts(); saveComplaints();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", Main::handle);
        server.setExecutor(Executors.newFixedThreadPool(12));
        System.out.println("==============================================");
        System.out.println(" CIVICPULSE GOVERNMENT GRIEVANCE WEB SYSTEM");
        System.out.println("==============================================");
        System.out.println("Web Portal : http://localhost:" + PORT);
        System.out.println("Admin      : admin / admin123");
        System.out.println("HOD        : waterhod/roadhod/electricityhod/sanitationhod / hod123");
        System.out.println("Employees  : water1/water123, road1/road123, electricity1/electricity123, sanitation1/sanitation123");
        System.out.println("Default complaints: " + complaints.size());
        System.out.println(MongoStore.ready ? "Database    : MongoDB Atlas connected (db: CivicPulse)" : "Database    : local files (MongoDB not connected)");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (MongoStore.client != null) { try { MongoStore.client.close(); } catch (Exception ignored) { } } }));
        System.out.println("No Maven required. Keep this window open while using the website.");
        server.start();
    }

    static void handle(HttpExchange ex) {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (method.equals("GET") && path.equals("/")) { serveFile(ex, "web/index.html", "text/html; charset=UTF-8"); return; }
            if (method.equals("GET") && path.equals("/app.js")) { serveFile(ex, "web/app.js", "application/javascript; charset=UTF-8"); return; }
            if (method.equals("GET") && path.equals("/style.css")) { serveFile(ex, "web/style.css", "text/css; charset=UTF-8"); return; }
            if (path.startsWith("/api/")) { api(ex, path, method); return; }
            send(ex, 404, json("error", "Not found"));
        } catch (Exception e) {
            e.printStackTrace();
            try { send(ex, 500, json("error", "Server error: " + e.getMessage())); } catch (Exception ignored) {}
        }
    }

    static void api(HttpExchange ex, String path, String method) throws Exception {
        Map<String,String> q = parseQuery(ex.getRequestURI().getRawQuery());
        Map<String,String> body = method.equals("POST") ? parseQuery(readBody(ex)) : Map.of();
        User u = current(ex);

        if (path.equals("/api/login") && method.equals("POST")) {
            String username = body.getOrDefault("username", "").trim();
            String password = body.getOrDefault("password", "");
            String role = body.getOrDefault("role", "").toUpperCase();
            User found = users.get(username);
            if (found == null || !found.role.equals(role) || !found.passwordHash.equals(hash(password))) {
                send(ex, 401, json("error", "Invalid username, password or account type.")); return;
            }
            String token = UUID.randomUUID().toString();
            sessions.put(token, new Session(username, LocalDateTime.now()));
            send(ex, 200, "{\"message\":"+q("Login successful")+",\"token\":"+q(token)+",\"user\":"+userJson(found)+"}"); return;
        }
        if (path.equals("/api/register") && method.equals("POST")) {
            String name=body.getOrDefault("name","").trim(), phone=body.getOrDefault("phone","").trim(), username=body.getOrDefault("username","").trim();
            String password=body.getOrDefault("password","");
            if(name.isBlank()||phone.isBlank()||username.isBlank()||password.length()<6||!password.equals(body.getOrDefault("confirm",""))){send(ex,400,json("error","Name, phone, username and matching 6+ character passwords are required."));return;}
            synchronized(Main.class){ if(users.containsKey(username)){send(ex,409,json("error","Username already exists."));return;} users.put(username,new User(username,hash(password),"CITIZEN","",phone,name)); saveAccounts(); }
            send(ex,200,json("message","Citizen account created. You can now log in.")); return;
        }
        if (path.equals("/api/logout") && method.equals("POST")) { String token=token(ex); if(token!=null)sessions.remove(token); send(ex,200,json("message","Logged out"));return; }
        if (path.equals("/api/me") && method.equals("GET")) { if(u==null){send(ex,401,json("error","Login required"));return;} send(ex,200,obj("user",userJson(u)));return; }
        if (path.equals("/api/meta") && method.equals("GET")) { send(ex,200,metaJson()); return; }
        if (u==null) { send(ex,401,json("error","Login required")); return; }

        if(path.equals("/api/complaints") && method.equals("GET")){ send(ex,200, complaintsJsonFor(u)); return; }
        if(path.equals("/api/complaints") && method.equals("POST")){ createComplaint(ex,u,body); return; }
        if(path.equals("/api/search") && method.equals("GET")){ search(ex,u,q.getOrDefault("q","")); return; }
        if(path.equals("/api/history") && method.equals("GET")){ history(ex,u,q.getOrDefault("id","")); return; }
        if(path.equals("/api/claim") && method.equals("POST")){ claim(ex,u,body.getOrDefault("id","")); return; }
        if(path.equals("/api/status") && method.equals("POST")){ updateStatus(ex,u,body.getOrDefault("id",""),body.getOrDefault("status","")); return; }
        if(path.equals("/api/duplicate") && method.equals("GET")){ duplicate(ex,u,q.getOrDefault("id","")); return; }
        if(path.equals("/api/capacity") && method.equals("GET")){ capacity(ex,u); return; }

        if(path.equals("/api/admin/stats") && method.equals("GET") && u.role.equals("ADMIN")){ adminStats(ex); return; }
        if(path.equals("/api/admin/accounts") && method.equals("GET") && u.role.equals("ADMIN")){ accountSummary(ex); return; }
        if(path.equals("/api/admin/audit") && method.equals("GET") && u.role.equals("ADMIN")){ audit(ex,q.getOrDefault("id","")); return; }
        if(path.equals("/api/admin/employee-history") && method.equals("GET") && u.role.equals("ADMIN")){ employeeHistory(ex,u,true); return; }

        if(path.equals("/api/hod/stats") && method.equals("GET") && u.role.equals("HOD")){ hodStats(ex,u); return; }
        if(path.equals("/api/hod/team") && method.equals("GET") && u.role.equals("HOD")){ hodTeam(ex,u); return; }
        if(path.equals("/api/hod/employee-history") && method.equals("GET") && u.role.equals("HOD")){ employeeHistory(ex,u,false); return; }
        if(path.equals("/api/hod/employee") && method.equals("POST") && u.role.equals("HOD")){ createEmployeeForHod(ex,u,body); return; }
        if(path.equals("/api/hod/assign") && method.equals("POST") && u.role.equals("HOD")){ hodAssign(ex,u,body); return; }
        if(path.equals("/api/hod/audit") && method.equals("GET") && u.role.equals("HOD")){ hodAudit(ex,u,q.getOrDefault("id","")); return; }
        if(path.equals("/api/hod/database") && method.equals("GET") && u.role.equals("HOD")){ databaseStatus(ex); return; }
        if(path.equals("/api/admin/database") && method.equals("GET") && u.role.equals("ADMIN")){ databaseStatus(ex); return; }
        if(path.equals("/api/report/pdf") && method.equals("GET")){ if(u==null){send(ex,401,json("error","Login required"));return;} downloadPdf(ex,u,q.getOrDefault("title","CivicPulse Report")); return; }

        send(ex,403,json("error","Access denied for this role."));
    }

    static void createComplaint(HttpExchange ex, User u, Map<String,String> b) throws Exception {
        if(!u.role.equals("CITIZEN")){send(ex,403,json("error","Only citizens can raise complaints."));return;}
        String dept=b.getOrDefault("department","").trim(), loc=b.getOrDefault("location","").trim();
        String date=b.getOrDefault("startedDate","").trim(), time=b.getOrDefault("startedTime","").trim(), desc=b.getOrDefault("description","").trim();
        int sev=parseInt(b.get("severity"),0), affected=parseInt(b.get("affectedPeople"),0);
        if(!DEPTS.contains(dept)||!LOCATION_DEPT.containsKey(loc)||!validLocation(loc,dept)||sev<1||sev>5||affected<1||date.isBlank()||time.isBlank()||desc.length()<8){send(ex,400,json("error","Check department, location, date/time, severity, affected people and description."));return;}
        synchronized(Main.class){ String id; do{id="R"+String.format("%03d",ThreadLocalRandom.current().nextInt(1,1000));}while(complaints.containsKey(id));
            Complaint c=new Complaint(id,u.username,u.name,u.phone,dept,loc,sev,affected,date,time,desc,nextArrival++,"REGISTERED","",""); c.history.add(now()+" - Complaint created with status REGISTERED");
            String hodUser = hodForDepartment(dept);
            c.history.add(now()+" - Routed to "+dept+" HOD dashboard ("+hodUser+")");
            complaints.put(id,c); saveComplaints();
            send(ex,200,"{\"message\":"+q("Complaint submitted successfully. Reference ID: "+id)+",\"complaint\":"+complaintJson(c)+"}"); }
    }

    static boolean validLocation(String loc,String dept){String allowed=LOCATION_DEPT.get(loc);return "ALL".equals(allowed)||allowed.equals(dept);}

    static void claim(HttpExchange ex,User u,String id)throws Exception{
        send(ex,403,json("error","Employees cannot self-claim complaints. The respective HOD must assign the complaint."));
    }

    static void updateStatus(HttpExchange ex,User u,String id,String newStatus)throws Exception{
        if(!u.role.equals("EMPLOYEE")){send(ex,403,json("error","Only employees can update complaint status."));return;}
        synchronized(Main.class){Complaint c=complaints.get(id);if(c==null){send(ex,404,json("error","Complaint not found."));return;}if(!c.department.equals(u.department)||!u.username.equals(c.assignedUsername)){send(ex,403,json("error","Only the assigned employee can update this complaint."));return;}String expected=c.status.equals("ASSIGNED")?"IN_PROGRESS":c.status.equals("IN_PROGRESS")?"RESOLVED":"";if(!newStatus.equals(expected)){send(ex,400,json("error","Allowed workflow is ASSIGNED -> IN_PROGRESS -> RESOLVED."));return;}c.status=newStatus;c.history.add(now()+" - Status changed to "+newStatus+" by "+u.name);saveComplaints();send(ex,200,"{\"message\":"+q("Status updated successfully: "+newStatus)+",\"complaint\":"+complaintJson(c)+"}");}
    }

    static void createEmployeeForHod(HttpExchange ex,User hod,Map<String,String>b)throws Exception{
        if(!hod.role.equals("HOD")){send(ex,403,json("error","Only the department HOD can create employees."));return;}
        String name=b.getOrDefault("name","").trim(),username=b.getOrDefault("username","").trim(),pw=b.getOrDefault("password","");
        String confirm=b.getOrDefault("confirm","");
        if(name.isBlank()||username.isBlank()||pw.length()<6||!pw.equals(confirm)){send(ex,400,json("error","Enter a name, unique username and matching password of at least 6 characters."));return;}
        synchronized(Main.class){
            if(users.containsKey(username)){send(ex,409,json("error","Username already exists."));return;}
            users.put(username,new User(username,hash(pw),"EMPLOYEE",hod.department,"",name));
            saveAccounts();
            send(ex,200,json("message","Employee account created for the "+hod.department+" department."));
        }
    }

    static void hodAssign(HttpExchange ex,User hod,Map<String,String>b)throws Exception{
        String id=b.getOrDefault("id","").trim(), employee=b.getOrDefault("employee","").trim();
        synchronized(Main.class){
            Complaint c=complaints.get(id); User emp=users.get(employee);
            if(c==null){send(ex,404,json("error","Complaint not found."));return;}
            if(!c.department.equals(hod.department)){send(ex,403,json("error","Complaint belongs to another department."));return;}
            if(emp==null||!emp.role.equals("EMPLOYEE")||!emp.department.equals(hod.department)){send(ex,400,json("error","Select an employee from the same department."));return;}
            if(!c.assignedUsername.isBlank()){send(ex,409,json("error","Complaint is already locked to "+c.assignedName+"."));return;}
            if(c.status.equals("RESOLVED")){send(ex,409,json("error","Resolved complaint cannot be assigned."));return;}
            if(activeCount(hod.department)>=10){send(ex,409,json("error","Department active workload capacity is full."));return;}
            c.assignedUsername=emp.username;c.assignedName=emp.name;c.status="ASSIGNED";c.history.add(now()+" - HOD "+hod.name+" assigned complaint to "+emp.name+" ("+emp.username+")");saveComplaints();
            send(ex,200,"{\"message\":"+q("Complaint assigned to "+emp.name)+",\"complaint\":"+complaintJson(c)+"}");
        }
    }

    static String hodForDepartment(String dept){
        for(Map.Entry<String,String> e:HOD_DEPT.entrySet()) if(e.getValue().equals(dept)) return e.getKey();
        return "Not configured";
    }

    static void employeeHistory(HttpExchange ex,User u,boolean allDepartments)throws Exception{
        List<String> out=new ArrayList<>();
        for(User emp:users.values()){
            if(!emp.role.equals("EMPLOYEE")) continue;
            if(!allDepartments && !emp.department.equals(u.department)) continue;
            List<Complaint> works=new ArrayList<>();
            for(Complaint c:complaints.values()) if(emp.username.equals(c.assignedUsername)) works.add(c);
            works.sort(Comparator.comparingInt(c->c.arrivalOrder));
            StringBuilder b=new StringBuilder();
            b.append("{\"username\":").append(q(emp.username)).append(",\"name\":").append(q(emp.name)).append(",\"department\":").append(q(emp.department)).append(",\"total\":").append(works.size()).append(",\"active\":").append(works.stream().filter(c->!c.status.equals("RESOLVED")).count()).append(",\"resolved\":").append(works.stream().filter(c->c.status.equals("RESOLVED")).count()).append(",\"complaints\":").append(listJson(works)).append("}");
            out.add(b.toString());
        }
        send(ex,200,"["+String.join(",",out)+"]");
    }

    static void search(HttpExchange ex,User u,String query)throws Exception{
        String q=query.trim().toLowerCase();List<Complaint> list=new ArrayList<>();for(Complaint c:complaints.values()) if(visible(u,c)&&(q.isBlank()||contains(c,q))) list.add(c);list.sort(priorityComparator());send(ex,200,listJson(list));
    }
    static boolean contains(Complaint c,String q){return c.id.toLowerCase().contains(q)||c.citizenName.toLowerCase().contains(q)||c.department.toLowerCase().contains(q)||c.location.toLowerCase().contains(q)||c.description.toLowerCase().contains(q);}
    static void history(HttpExchange ex,User u,String id)throws Exception{Complaint c=complaints.get(id);if(c==null||!visible(u,c)){send(ex,404,json("error","Complaint not found or access denied."));return;}send(ex,200,historyJson(c));}
    static void audit(HttpExchange ex,String id)throws Exception{Complaint c=complaints.get(id);if(c==null){send(ex,404,json("error","Complaint not found."));return;}send(ex,200,historyJson(c));}
    static void hodAudit(HttpExchange ex,User u,String id)throws Exception{Complaint c=complaints.get(id);if(c==null||!c.department.equals(u.department)){send(ex,404,json("error","Complaint not found or access denied."));return;}send(ex,200,historyJson(c));}
    static String historyJson(Complaint c){return "{"+q("id")+":"+q(c.id)+",\"history\":"+stringList(c.history)+"}";}
    static void duplicate(HttpExchange ex,User u,String id)throws Exception{Complaint base=complaints.get(id);if(base==null||!visible(u,base)){send(ex,404,json("error","Complaint not found or access denied."));return;}List<Complaint> matches=new ArrayList<>();for(Complaint c:complaints.values())if(!c.id.equals(base.id)&&c.department.equals(base.department)&&c.location.equals(base.location)&&!c.status.equals("RESOLVED"))matches.add(c);send(ex,200,listJson(matches));}

    static void capacity(HttpExchange ex,User u)throws Exception{if(u.role.equals("EMPLOYEE")||u.role.equals("HOD")){send(ex,200,capacityJson(List.of(u.department)));return;}if(u.role.equals("ADMIN")){send(ex,200,capacityJson(DEPTS));return;}send(ex,403,json("error","Employees, HODs and admins only."));}
    static String capacityJson(List<String>ds){List<String>items=new ArrayList<>();for(String d:ds){long a=activeCount(d);items.add("{\"department\":"+q(d)+",\"active\":"+a+",\"capacity\":10,\"available\":"+(a<10)+"}");}return "["+String.join(",",items)+"]";}
    static void adminStats(HttpExchange ex)throws Exception{int reg=countStatus("REGISTERED"),assigned=countStatus("ASSIGNED"),progress=countStatus("IN_PROGRESS"),resolved=countStatus("RESOLVED");send(ex,200,obj("total",Integer.toString(complaints.size()),"registered",Integer.toString(reg),"assigned",Integer.toString(assigned),"inProgress",Integer.toString(progress),"resolved",Integer.toString(resolved),"citizens",Integer.toString(countRole("CITIZEN")),"employees",Integer.toString(countRole("EMPLOYEE")),"hods",Integer.toString(countRole("HOD")),"admins",Integer.toString(countRole("ADMIN")),"priority",listJson(sortedComplaints())));}
    static void accountSummary(HttpExchange ex)throws Exception{
        List<String> employeeUsernames=new ArrayList<>(), hodUsernames=new ArrayList<>();
        for(User account:users.values()){if(account.role.equals("EMPLOYEE")) employeeUsernames.add(q(account.username));if(account.role.equals("HOD")) hodUsernames.add(q(account.username));}
        String response="{\"citizens\":"+countRole("CITIZEN")+",\"employees\":"+countRole("EMPLOYEE")+",\"hods\":"+countRole("HOD")+",\"admins\":"+countRole("ADMIN")+",\"employeeUsernames\":["+String.join(",",employeeUsernames)+"],\"hodUsernames\":["+String.join(",",hodUsernames)+"]}";
        send(ex,200,response);
    }
    static void hodStats(HttpExchange ex,User u)throws Exception{
        int total=0,reg=0,assigned=0,progress=0,resolved=0; for(Complaint c:complaints.values()) if(c.department.equals(u.department)){total++;if(c.status.equals("REGISTERED"))reg++;if(c.status.equals("ASSIGNED"))assigned++;if(c.status.equals("IN_PROGRESS"))progress++;if(c.status.equals("RESOLVED"))resolved++;}
        send(ex,200,obj("department",u.department,"total",Integer.toString(total),"registered",Integer.toString(reg),"assigned",Integer.toString(assigned),"inProgress",Integer.toString(progress),"resolved",Integer.toString(resolved),"employees",Integer.toString(countEmployees(u.department)),"active",Long.toString(activeCount(u.department))));
    }
    static void hodTeam(HttpExchange ex,User u)throws Exception{
        List<String> out=new ArrayList<>(); for(User emp:users.values()) if(emp.role.equals("EMPLOYEE")&&emp.department.equals(u.department)){int total=0,active=0,resolved=0;for(Complaint c:complaints.values())if(emp.username.equals(c.assignedUsername)){total++;if(c.status.equals("RESOLVED"))resolved++;else active++;}out.add("{\"username\":"+q(emp.username)+",\"name\":"+q(emp.name)+",\"total\":"+total+",\"active\":"+active+",\"resolved\":"+resolved+"}");}send(ex,200,"["+String.join(",",out)+"]");
    }
    static int countEmployees(String dept){return (int)users.values().stream().filter(u->u.role.equals("EMPLOYEE")&&u.department.equals(dept)).count();}
    static void databaseStatus(HttpExchange ex)throws Exception{
        if (MongoStore.ready) {
            send(ex, 200, "{\"mode\":\"MONGODB_ATLAS\",\"status\":\"CONNECTED\",\"database\":\"CivicPulse\",\"accounts\":" + MongoStore.accounts.countDocuments() + ",\"complaints\":" + MongoStore.complaints.countDocuments() + ",\"message\":\"Connected to MongoDB Atlas successfully.\"}");
        } else {
            send(ex, 200, "{\"mode\":\"FILE\",\"status\":\"FALLBACK_FILE\",\"database\":\"CivicPulse\",\"message\":\"MongoDB not connected; using local files.\"}");
        }
    }

    static String complaintsJsonFor(User u){List<Complaint> list=new ArrayList<>();for(Complaint c:complaints.values())if(visible(u,c))list.add(c);list.sort(priorityComparator());return listJson(list);}
    static boolean visible(User u,Complaint c){if(u.role.equals("ADMIN"))return true;if(u.role.equals("CITIZEN"))return c.citizenUsername.equals(u.username);if(u.role.equals("EMPLOYEE"))return c.assignedUsername.equals(u.username);return c.department.equals(u.department);}
    static List<Complaint> sortedComplaints(){List<Complaint>l=new ArrayList<>(complaints.values());l.sort(priorityComparator());return l;}
    static Comparator<Complaint> priorityComparator(){return Comparator.comparingInt((Complaint c)->c.severity).reversed().thenComparing(Comparator.comparingInt((Complaint c)->c.affectedPeople).reversed()).thenComparingInt(c->c.arrivalOrder);}
    static long activeCount(String dept){return complaints.values().stream().filter(c->c.department.equals(dept)&&(c.status.equals("ASSIGNED")||c.status.equals("IN_PROGRESS"))).count();}
    static int countStatus(String s){return (int)complaints.values().stream().filter(c->c.status.equals(s)).count();}
    static int countRole(String r){return (int)users.values().stream().filter(u->u.role.equals(r)).count();}

    static void seedAccountsIfNeeded(){
        if(!users.containsKey("admin"))users.put("admin",new User("admin",hash("admin123"),"ADMIN","ALL","","System Administrator"));
        for(String un:DEFAULT_EMPLOYEES.keySet()) if(!users.containsKey(un))users.put(un,new User(un,hash(DEFAULT_EMPLOYEES.get(un)),"EMPLOYEE",EMP_DEPT.get(un),"",EMP_DEPT.get(un)+" Department Employee"));
        // Exactly one fixed HOD account is maintained for each department.
        users.entrySet().removeIf(e -> e.getValue().role.equals("HOD") && !DEFAULT_HODS.containsKey(e.getKey()));
        for(String un:DEFAULT_HODS.keySet()){
            User fixed=users.get(un);
            if(fixed==null || !fixed.role.equals("HOD") || !fixed.department.equals(HOD_DEPT.get(un))){
                users.put(un,new User(un,hash(DEFAULT_HODS.get(un)),"HOD",HOD_DEPT.get(un),"",HOD_DEPT.get(un)+" Department HOD"));
            }
        }
        if(!users.containsKey("citizen1"))users.put("citizen1",new User("citizen1",hash("citizen123"),"CITIZEN","","9000000001","Sample Citizen 1"));
        if(!users.containsKey("citizen2"))users.put("citizen2",new User("citizen2",hash("citizen123"),"CITIZEN","","9000000002","Sample Citizen 2"));
    }
    static void seedComplaintsIfNeeded(){if(!complaints.isEmpty())return;String[]depts={"Water","Road","Electricity","Sanitation"};String[][]loc={{"Makkal Nagar","Periyar Nagar"},{"Anna Nagar","Periyar Nagar"},{"Gandhi Nagar","Periyar Nagar"},{"Nehru Nagar","Periyar Nagar"}};String[]issues={"Low water pressure and irregular supply","Large pothole on the main road","Streetlight is not working","Garbage collection is delayed","Leaking public water pipe","Damaged road surface","Exposed electrical wire","Overflowing public waste bin","Contaminated drinking water concern","Blocked roadside drain"};for(int i=0;i<30;i++){int d=i%4;String l=loc[d][(i/4)%2];int sev=1+(i%5);String id=String.format("R%03d",i+1);String citizen="Sample Citizen "+(i+1);String cu=(i%2==0?"citizen1":"citizen2");Complaint c=new Complaint(id,cu,citizen,i%2==0?"9000000001":"9000000002",depts[d],l,sev,10+(i%8)*5,"01-10-2026",String.format("%02d:%02d",8+(i%10),(i*7)%60),issues[i%issues.length]+" near "+l+".",nextArrival++,"REGISTERED","","");c.history.add(now()+" - Initial complaint loaded");complaints.put(id,c);}}

    static void saveAccounts(){
        saveAccountsFile();
        if(MongoStore.ready){
            try { MongoStore.saveUsers(users.values()); }
            catch(Exception e){ System.out.println("MongoDB account save warning: "+e.getMessage()); }
        }
    }
    static void saveComplaints(){
        saveComplaintsFile();
        if(MongoStore.ready){
            try { MongoStore.saveComplaints(complaints.values()); }
            catch(Exception e){ System.out.println("MongoDB complaint save warning: "+e.getMessage()); }
        }
    }
    static void loadAccountsFile(){if(!Files.exists(ACCOUNTS))return;try{for(String line:Files.readAllLines(ACCOUNTS)){if(line.isBlank()||line.startsWith("#"))continue;String[]p=line.split("\\|",-1);if(p.length>=6)users.put(p[0],new User(p[0],p[1],p[2],p[3],p[4],p[5]));}}catch(Exception e){System.out.println("Account load warning: "+e.getMessage());}}
    static void saveAccountsFile(){try{Files.createDirectories(DATA);List<String>out=new ArrayList<>();out.add("# username|passwordHash|role|department|phone|name");for(User u:users.values())out.add(String.join("|",safe(u.username),safe(u.passwordHash),safe(u.role),safe(u.department),safe(u.phone),safe(u.name)));Files.write(ACCOUNTS,out,StandardCharsets.UTF_8);}catch(IOException e){System.out.println("Account save warning: "+e.getMessage());}}
    static void loadComplaintsFile(){if(!Files.exists(COMPLAINTS))return;try{for(String line:Files.readAllLines(COMPLAINTS)){if(line.isBlank()||line.startsWith("#"))continue;String[]p=line.split("\\|",-1);if(p.length<16)continue;Complaint c=new Complaint(p[0],p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),Integer.parseInt(p[7]),p[8],p[9],p[10],Integer.parseInt(p[11]),p[12],p[13],p[14]);if(p.length>15)c.history.addAll(decodeList(p[15]));complaints.put(c.id,c);nextArrival=Math.max(nextArrival,c.arrivalOrder+1);}}catch(Exception e){System.out.println("Complaint load warning: "+e.getMessage());}}
    static void saveComplaintsFile(){try{Files.createDirectories(DATA);List<String>out=new ArrayList<>();out.add("# id|citizenUsername|citizenName|phone|department|location|severity|affected|date|time|description|arrival|status|assignedUsername|assignedName|history(base64-items)");for(Complaint c:complaints.values()){String h=Base64.getEncoder().encodeToString(String.join("\u001e",c.history).getBytes(StandardCharsets.UTF_8));out.add(String.join("|",safe(c.id),safe(c.citizenUsername),safe(c.citizenName),safe(c.phone),safe(c.department),safe(c.location),Integer.toString(c.severity),Integer.toString(c.affectedPeople),safe(c.date),safe(c.time),safe(c.description),Integer.toString(c.arrivalOrder),safe(c.status),safe(c.assignedUsername),safe(c.assignedName),h));}Files.write(COMPLAINTS,out,StandardCharsets.UTF_8);}catch(IOException e){System.out.println("Complaint save warning: "+e.getMessage());}}
    static String safe(String s){return s==null?"":s.replace("|","/").replace("\n"," ").replace("\r"," ");}
    static List<String> decodeList(String b){try{String s=new String(Base64.getDecoder().decode(b),StandardCharsets.UTF_8);return s.isBlank()?new ArrayList<>():new ArrayList<>(Arrays.asList(s.split("\\u001e",-1)));}catch(Exception e){return new ArrayList<>();}}

    static String token(HttpExchange ex){String h=ex.getRequestHeaders().getFirst("Authorization");return h!=null&&h.startsWith("Bearer ")?h.substring(7):null;}
    static User current(HttpExchange ex){String t=token(ex);if(t==null)return null;Session s=sessions.get(t);return s==null?null:users.get(s.username);}
    static String readBody(HttpExchange ex)throws IOException{return new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
    static Map<String,String> parseQuery(String raw){Map<String,String>m=new LinkedHashMap<>();if(raw==null||raw.isBlank())return m;for(String part:raw.split("&")){int i=part.indexOf('=');String k=i>=0?part.substring(0,i):part;String v=i>=0?part.substring(i+1):"";try{k=URLDecoder.decode(k,StandardCharsets.UTF_8);v=URLDecoder.decode(v,StandardCharsets.UTF_8);}catch(Exception ignored){}m.put(k,v);}return m;}
    static void serveFile(HttpExchange ex,String file,String type)throws IOException{Path p=Paths.get(file);if(!Files.exists(p)){send(ex,404,"Not found");return;}byte[]b=Files.readAllBytes(p);ex.getResponseHeaders().set("Content-Type",type);ex.sendResponseHeaders(200,b.length);try(OutputStream o=ex.getResponseBody()){o.write(b);}}
    static void send(HttpExchange ex,int code,String text)throws IOException{byte[]b=text.getBytes(StandardCharsets.UTF_8);Headers h=ex.getResponseHeaders();h.set("Content-Type","application/json; charset=UTF-8");h.set("Cache-Control","no-store");ex.sendResponseHeaders(code,b.length);try(OutputStream o=ex.getResponseBody()){o.write(b);}}
    static void downloadPdf(HttpExchange ex,User u,String title)throws IOException{
        List<Complaint> records=new ArrayList<>();
        for(Complaint c:complaints.values()) if(visible(u,c)) records.add(c);
        records.sort(priorityComparator());
        byte[] pdf=buildPdf(records,title);
        Headers h=ex.getResponseHeaders();
        h.set("Content-Type","application/pdf");
        h.set("Content-Disposition","attachment; filename=\""+safeFileName(title,"civicpulse_report")+".pdf\"");
        h.set("Cache-Control","no-store");
        ex.sendResponseHeaders(200,pdf.length);
        try(OutputStream o=ex.getResponseBody()){o.write(pdf);}
    }
    static String safeFileName(String title,String fallback){String x=title==null?fallback:title.replaceAll("[^A-Za-z0-9._-]+","_");if(x.isBlank())x=fallback;return x.length()>90?x.substring(0,90):x;}
    static byte[] buildPdf(List<Complaint> records,String title){
        List<String> lines=new ArrayList<>();
        lines.add("CivicPulse - Government Grievance Management");
        lines.add(title==null?"CivicPulse Report":title);
        lines.add("Generated: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        lines.add("Total records: "+records.size());
        lines.add("");
        if(records.isEmpty()) lines.add("No records available.");
        else for(Complaint c:records){
            String emp=c.assignedUsername.isBlank()?"UNASSIGNED":c.assignedName+" ("+c.assignedUsername+")";
            lines.add(c.id+" | "+c.department+" | "+c.location+" | Sev "+c.severity+" | "+c.status);
            lines.add("Employee: "+emp+" | Affected: "+c.affectedPeople+" | Date: "+c.date+" "+c.time);
            lines.add("Description: "+c.description);
            if(!c.history.isEmpty()){
                lines.add("Status / assignment history:");
                for(String event:c.history) lines.add("- "+event);
            }
            lines.add("");
        }
        List<List<String>> pages=new ArrayList<>();
        for(int i=0;i<lines.size();i+=42) pages.add(lines.subList(i,Math.min(lines.size(),i+42)));
        if(pages.isEmpty()) pages.add(List.of("No records available."));
        int fontObj=3+(pages.size()*2);
        int max=fontObj;
        List<byte[]> objs=new ArrayList<>();
        while(objs.size()<max) objs.add(null);
        objs.set(0,bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        List<Integer> pageObjNums=new ArrayList<>();
        for(int i=0;i<pages.size();i++) pageObjNums.add(3+(i*2));
        for(int i=0;i<pages.size();i++){
            int pnum=pageObjNums.get(i), cnum=pnum+1;
            objs.set(pnum-1,bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 "+fontObj+" 0 R >> >> /Contents "+cnum+" 0 R >>"));
            StringBuilder stream=new StringBuilder("BT /F1 10 Tf 42 750 Td 14 TL\n");
            boolean first=true; int count=0;
            for(String line:pages.get(i)){
                String clean=pdfText(line); if(clean.length()>115) clean=clean.substring(0,115);
                if(!first) stream.append("T*\n"); first=false;
                stream.append("(").append(pdfEscape(clean)).append(") Tj\n"); count++; if(count>=42) break;
            }
            stream.append("ET");
            byte[] sb=stream.toString().getBytes(StandardCharsets.ISO_8859_1);
            objs.set(cnum-1,bytes("<< /Length "+sb.length+" >>\nstream\n"+stream+"\nendstream"));
        }
        objs.set(1,bytes("<< /Type /Pages /Kids ["+pageObjNums.stream().map(n->n+" 0 R").reduce((a,b)->a+" "+b).orElse("")+"] /Count "+pages.size()+" >>"));
        objs.set(fontObj-1,bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try{out.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));}catch(IOException ignored){}
        long[] offsets=new long[objs.size()+1];
        for(int i=1;i<=objs.size();i++){
            offsets[i]=out.size();
            try{out.write((i+" 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));out.write(objs.get(i-1));out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));}catch(IOException ignored){}
        }
        long xref=out.size();
        try{out.write(("xref\n0 "+(objs.size()+1)+"\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));for(int i=1;i<=objs.size();i++)out.write(String.format("%010d 00000 n \n",offsets[i]).getBytes(StandardCharsets.ISO_8859_1));out.write(("trailer\n<< /Size "+(objs.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));}catch(IOException ignored){}
        return out.toByteArray();
    }
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.ISO_8859_1);}
    static String pdfText(String s){StringBuilder b=new StringBuilder();for(char c:(s==null?"":s).toCharArray())b.append(c>=32&&c<=126?c:'?');return b.toString();}
    static String pdfEscape(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}
    static String hash(String text){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    static String now(){return LocalDateTime.now().format(FMT);}
    static int parseInt(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}
    static String q(String s){if(s==null)return "null";return "\""+escape(s)+"\"";}
    static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");}
    static String json(String key,String value){return "{"+q(key)+":"+q(value)+"}";}
    static String obj(String...kv){StringBuilder b=new StringBuilder("{");for(int i=0;i<kv.length;i+=2){if(i>0)b.append(',');b.append(q(kv[i])).append(':').append(q(kv[i+1]));}return b.append('}').toString();}
    static String userJson(User u){return "{"+q("username")+":"+q(u.username)+","+q("name")+":"+q(u.name)+","+q("role")+":"+q(u.role)+","+q("department")+":"+q(u.department)+","+q("phone")+":"+q(u.phone)+"}";}
    static String metaJson(){return "{"+q("departments")+":"+arrayStrings(DEPTS)+","+q("locations")+":"+arrayStrings(LOCATIONS)+"}";}
    static String arrayStrings(Collection<String>s){List<String>l=new ArrayList<>();for(String x:s)l.add(q(x));return "["+String.join(",",l)+"]";}
    static String listJson(List<Complaint>l){List<String>x=new ArrayList<>();for(Complaint c:l)x.add(complaintJson(c));return "["+String.join(",",x)+"]";}
    static String stringList(List<String>l){return arrayStrings(l);}
    static String complaintJson(Complaint c){String emp=c.assignedUsername.isBlank()?"UNASSIGNED":c.assignedName+" ("+c.assignedUsername+")";return "{"+q("id")+":"+q(c.id)+","+q("citizen")+":"+q(c.citizenName)+","+q("username")+":"+q(c.citizenUsername)+","+q("phone")+":"+q(c.phone)+","+q("department")+":"+q(c.department)+","+q("location")+":"+q(c.location)+","+q("severity")+":"+c.severity+","+q("affectedPeople")+":"+c.affectedPeople+","+q("date")+":"+q(c.date)+","+q("time")+":"+q(c.time)+","+q("description")+":"+q(c.description)+","+q("arrival")+":"+c.arrivalOrder+","+q("status")+":"+q(c.status)+","+q("employee")+":"+q(emp)+","+q("assignedUsername")+":"+q(c.assignedUsername)+","+q("history")+":"+stringList(c.history)+"}";}

    static class User {final String username,passwordHash,role,department,phone,name;User(String u,String p,String r,String d,String ph,String n){username=u;passwordHash=p;role=r;department=d;phone=ph;name=n;}}
    static class Session {final String username;final LocalDateTime created;Session(String u,LocalDateTime c){username=u;created=c;}}
    static class Complaint {final String id;String citizenUsername,citizenName,phone,department,location;int severity,affectedPeople;String date,time,description;final int arrivalOrder;String status,assignedUsername,assignedName;List<String>history=new ArrayList<>();Complaint(String id,String cu,String cn,String ph,String d,String l,int s,int a,String date,String time,String desc,int arr,String st,String au,String an){this.id=id;citizenUsername=cu;citizenName=cn;phone=ph;department=d;location=l;severity=s;affectedPeople=a;this.date=date;this.time=time;description=desc;arrivalOrder=arr;status=st;assignedUsername=au;assignedName=an;}}
}
