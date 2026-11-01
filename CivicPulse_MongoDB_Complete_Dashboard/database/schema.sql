CREATE DATABASE IF NOT EXISTS civicpulse;
USE civicpulse;

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(80) PRIMARY KEY,
    password_hash VARCHAR(128) NOT NULL,
    role ENUM('CITIZEN','EMPLOYEE','HOD','ADMIN') NOT NULL,
    department VARCHAR(40) NULL,
    phone VARCHAR(30) NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    name VARCHAR(40) PRIMARY KEY
);
INSERT IGNORE INTO departments(name) VALUES ('Water'),('Road'),('Electricity'),('Sanitation');

CREATE TABLE IF NOT EXISTS locations (
    name VARCHAR(80) PRIMARY KEY,
    allowed_department VARCHAR(40) NOT NULL
);
INSERT IGNORE INTO locations(name, allowed_department) VALUES
('Makkal Nagar','ALL'),
('Anna Nagar','ALL'),
('Gandhi Nagar','ALL'),
('Nehru Nagar','ALL'),
('Periyar Nagar','ALL');

CREATE TABLE IF NOT EXISTS complaints (
    id VARCHAR(20) PRIMARY KEY,
    citizen_username VARCHAR(80) NOT NULL,
    citizen_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    department VARCHAR(40) NOT NULL,
    location VARCHAR(80) NOT NULL,
    severity INT NOT NULL,
    affected_people INT NOT NULL,
    started_date VARCHAR(20) NOT NULL,
    started_time VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    arrival_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_username VARCHAR(80) NULL,
    assigned_name VARCHAR(120) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_citizen FOREIGN KEY (citizen_username) REFERENCES users(username)
);

CREATE TABLE IF NOT EXISTS complaint_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id VARCHAR(20) NOT NULL,
    event_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

CREATE INDEX idx_complaints_department_status ON complaints(department,status);
CREATE INDEX idx_complaints_priority ON complaints(severity,affected_people,arrival_order);
CREATE INDEX idx_history_complaint ON complaint_history(complaint_id);
