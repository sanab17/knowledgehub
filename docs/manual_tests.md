# KnowledgeHub Manual Test Suite

This document contains manual test cases for verifying the core features, access control, and edge cases of the KnowledgeHub Enterprise Document Management Portal.

---

## 👥 Test Personas & Setup

To perform these manual tests, ensure the local server or Docker container stack is running and the database is migrated:
* **Host URL**: `http://localhost:8080`
* **Default Database Roles**:
  * **Admin User**: Set up an account with `ADMIN` role.
  * **Employee User A**: Set up an account with `EMPLOYEE` role.
  * **Employee User B**: Set up a second account with `EMPLOYEE` role.

---

## 🔒 Test Suite 1: Authentication & Authorization

| Test ID | Description | Pre-conditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AUTH-01** | Register New User (Valid) | Not logged in, on registration page | 1. Fill in all fields with valid details.<br/>2. Select role `EMPLOYEE`.<br/>3. Click register. | Registration is successful. Redirection to Login screen with success message. |
| **TC-AUTH-02** | Registration Passwords Mismatch | On registration page | 1. Enter valid details.<br/>2. Input "password123" in Password field and "password999" in Confirm Password.<br/>3. Click register. | Registration fails. Form shows inline validation error: "Passwords do not match". |
| **TC-AUTH-03** | Registration Duplicate Username/Email | User "testuser" already exists | 1. Try to register with username "testuser" or existing email address.<br/>2. Click register. | Registration fails. Displays user-friendly error message indicating that username or email is already taken. |
| **TC-AUTH-04** | Login with Valid Credentials | Valid user exists | 1. Go to `/login`.<br/>2. Enter valid username and password.<br/>3. Click sign in. | Redirected to `/dashboard` successfully. |
| **TC-AUTH-05** | Unauthorized Route Interception | Not logged in | 1. Navigate directly to `http://localhost:8080/dashboard` or `/documents`. | User is intercepted by Spring Security and redirected to the login page (`/login`). |

---

## 📂 Test Suite 2: Document Uploads & Validations

> [!IMPORTANT]
> The system supports PDF and DOCX files. Verify both sizes and file extensions during verification.

| Test ID | Description | Pre-conditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-DOC-01** | Upload Valid PDF Document | Logged in as any user | 1. Go to upload section.<br/>2. Select a PDF file under size limit.<br/>3. Select department (e.g. IT).<br/>4. Click upload. | Document is uploaded successfully, saved to local `uploads/` directory, and metadata displays in dashboard. |
| **TC-DOC-02** | Upload Valid DOCX Document | Logged in as any user | 1. Repeat same steps with a valid DOCX file. | Document is uploaded successfully. |
| **TC-DOC-03** | Upload Invalid File Format (e.g. TXT or EXE) | Logged in as any user | 1. Select a `.txt` or `.exe` file.<br/>2. Attempt to upload. | Application rejects file. Displays error: "Invalid file format. Only PDF and DOCX files are allowed." |
| **TC-DOC-04** | Upload File Exceeding Size Limit | Max limit configured in `application.yml` | 1. Select a file larger than the maximum allowed limit.<br/>2. Attempt to upload. | Application rejects file. Displays message indicating the file exceeds the maximum size limit. |
| **TC-DOC-05** | Form Validation Missing Metadata | Logged in as any user | 1. Upload a valid file but leave Title or Department blank.<br/>2. Click upload. | Browser HTML5 validation or backend validation halts process, displaying required field warning. |

---

## 🔍 Test Suite 3: Search, Filter, and Browsing

| Test ID | Description | Pre-conditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-SRCH-01** | Filter by Department | Documents exist in different departments | 1. On dashboard/document list, select "HR" in the department filter.<br/>2. Submit. | Only documents belonging to the "HR" department are displayed in the results list. |
| **TC-SRCH-02** | Search by Title Keyword | Documents with "Security Policy" in title exist | 1. Enter "Security" in search bar.<br/>2. Click Search. | Results filter down to show documents containing the word "Security" in the title. |
| **TC-SRCH-03** | Document Download Flow | Active documents exist | 1. Click download button on a listed document. | Browser downloads the correct file. File size and content-type match the uploaded source. |

---

## 🛡️ Test Suite 4: Access Control & Delete Permissions

> [!CAUTION]
> Deletion rules are enforced both on the UI and programmatically at the service layer.

| Test ID | Description | Pre-conditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-ACL-01** | Employee Delete Owned Document | Logged in as Employee User A. Existing document was uploaded by Employee User A. | 1. Find document in list.<br/>2. Click delete button. | Document is successfully deleted and file is removed from `uploads/`. |
| **TC-ACL-02** | Employee Delete Unowned Document | Logged in as Employee User A. Existing document was uploaded by Employee User B. | 1. Attempt to trigger delete command for Employee User B's document (check UI button and raw POST/DELETE endpoint call). | Delete button is hidden in UI. Direct endpoint calls trigger `AccessDeniedException` or service-level failure. |
| **TC-ACL-03** | Admin Delete Any Document | Logged in as Admin. Documents uploaded by User A and User B exist. | 1. Navigate to dashboard.<br/>2. Delete User A's document.<br/>3. Delete User B's document. | Both operations succeed. Admin possesses global deletion rights. |

---

## 📈 Test Suite 5: Monitoring & Health Checks

| Test ID | Description | Pre-conditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-ACT-01** | Actuator Health Endpoint Check | Application is running | 1. Navigate directly to `http://localhost:8080/actuator/health` in browser. | JSON response is returned containing status: `{"status":"UP"}`. |
