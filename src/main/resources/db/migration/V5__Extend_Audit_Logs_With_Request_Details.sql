-- Migration to add IP address, user-agent, and status result to audit logs

ALTER TABLE audit_logs ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(500);
ALTER TABLE audit_logs ADD COLUMN result VARCHAR(20) DEFAULT 'SUCCESS';
