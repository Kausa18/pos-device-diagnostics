# POS Device Diagnostics — Project Report (Boss Presentation Version)

**Repository:** `pos-device-diagnostics`

**Scope of this report:** Entire repository, including:

- `pos-backend` (Node.js/Express backend + web dashboard)
- `pos-mobile-app` (Android mobile app for running diagnostics and uploading results)
- Root artifacts (CI workflow, keystore, diagrams)

**Prepared for:** _[INSERT RECIPIENT NAME / TITLE]_  
**Prepared by:** _[INSERT YOUR NAME]_  
**Date:** _[INSERT DATE]_  
**Version:** 1.0

> Notes on accuracy
>
>- This report is written to match the *current* source code found in the repository at the time of generation.
>- Where information cannot be verified from the repository (e.g., business process owners, exact production infrastructure), the report uses **TBD** placeholders.

\pagebreak

## Table of Contents

1. Executive Summary
2. Business Context & Problem Statement
3. Project Objectives and Scope
4. Stakeholders
5. System Overview
6. Architecture (High-Level)
7. Backend: `pos-backend`
8. Dashboard UI: `pos-backend/public`
9. Mobile App: `pos-mobile-app`
10. API Specification
11. Database Design (SQLite)
12. Data Flow & Sequence Diagrams (Placeholders)
13. Security Review and Hardening Plan
14. Testing & Quality Assurance
15. CI/CD and Automation
16. Deployment & Operations
17. Risks, Constraints, and Assumptions
18. Maintenance & Roadmap
19. Appendices (Screenshots and UML Placeholders)

\pagebreak

# 1. Executive Summary

## 1.1 Summary

The **POS Device Diagnostics** project provides an end-to-end solution to:

- Collect **hardware and system diagnostics** from POS terminals via an Android application.
- Upload diagnostic results to a central backend.
- Provide a **web-based dashboard** for monitoring device health, diagnostics history, and administrative user management.

The repository is structured into:

- **Android mobile app** (`pos-mobile-app`): runs tests such as battery health, network connectivity, signal strength, storage/security health (root detection), touchscreen interaction, and hardware peripherals (NFC, IC card reader, printer, LED/buzzer, charging port). The app generates a JSON payload and uploads it to the backend.
- **Node.js backend** (`pos-backend`): receives diagnostic payloads, stores them in **SQLite**, auto-registers terminals, and serves a static dashboard for visualization and management.

## 1.2 Key Deliverables

- Diagnostic mobile app (Android, Java, Feitian hardware integration)
- Express REST API backend
- Dashboard UI (static HTML/JS pages served via Express)
- SQLite database storing diagnostics, terminals, and user accounts
- CI workflow for basic backend sanity check

## 1.3 High-Level Benefits

- Faster troubleshooting and support turnaround time
- Centralized visibility into device fleet health
- Enables proactive maintenance planning

## 1.4 Screenshots and UML Diagrams

This report includes **placeholders** for:

- Dashboard screenshots
- Mobile application screenshots
- UML diagrams (use-case, component, class, sequence, deployment)

\pagebreak

# 2. Business Context & Problem Statement

## 2.1 Context

POS terminals are business-critical endpoints. Failures in connectivity, peripherals (printer/NFC/card reader), battery/charging, or touchscreen can disrupt transactions.

## 2.2 Problem Statement

- Field teams need an **easy, repeatable diagnostic procedure**.
- Management needs **centralized reporting** and history to support operational decisions.

## 2.3 Proposed Solution

- A mobile diagnostics app that runs standardized tests and uploads results.
- A backend that stores the results and provides a dashboard to review and manage devices and users.

\pagebreak

# 3. Project Objectives and Scope

## 3.1 Objectives

- Provide automated/interactive diagnostics on POS devices.
- Collect results centrally.
- Offer a dashboard for monitoring and reporting.

## 3.2 In-Scope

- Android application (diagnostics tests + payload upload)
- Backend API + SQLite persistence
- Web dashboard UI
- Admin user management screens

## 3.3 Out-of-Scope (Current)

- Enterprise-grade authentication/authorization (planned but not fully implemented as secure auth)
- Centralized device provisioning and certificate-based authentication
- Cloud-scale database/storage (project uses SQLite)

\pagebreak

# 4. Stakeholders

## 4.1 Stakeholder List (TBD)

- **Project Sponsor:** _[TBD]_  
- **Product Owner:** _[TBD]_  
- **Engineering Team:** _[TBD]_  
- **Field Support / Technicians:** _[TBD]_  
- **IT / Security:** _[TBD]_  

## 4.2 Responsibilities

- Engineering: feature development, maintenance
- Support: run diagnostics on devices, interpret results
- Admin: approve users, manage access

\pagebreak

# 5. System Overview

## 5.1 Repository Structure

Top-level items observed:

- `.github/workflows/backend-ci.yml` — CI job for backend sanity check
- `Architecture.drawio` — architecture diagram file (currently minimal/empty diagram data)
- `keystore/feitian-release.jks` — Android signing keystore (sensitive)
- `pos-backend/` — Node.js/Express backend + dashboard
- `pos-mobile-app/` — Android app

## 5.2 Runtime Components

- **Mobile app** runs on the POS device (Android API 26+)
- **Backend** runs on a server/workstation (default port `4000`)
- **Dashboard** is served as static content from the backend

\pagebreak

# 6. Architecture (High-Level)

## 6.1 Conceptual Architecture

- **POS Device / Android App**
  - Executes diagnostics tests
  - Creates diagnostics payload JSON
  - Uploads payload to backend via HTTP

- **Backend (Express + SQLite)**
  - Validates incoming requests using API key header for device uploads
  - Stores diagnostics results and terminal metadata
  - Provides REST endpoints for dashboard queries
  - Serves static HTML dashboard

- **Dashboard UI**
  - Calls backend endpoints to list terminals, diagnostics, and users
  - Provides reporting and CSV export functionality

## 6.2 Placeholder for Architecture Diagram

[INSERT UML DIAGRAM A1: High-Level Component Diagram]

- Suggested location: created in Draw.io / PlantUML
- Recommended to include components:
  - Mobile App
  - Backend API
  - SQLite DB
  - Dashboard UI

\pagebreak

# 7. Backend: `pos-backend`

## 7.1 Technology Stack

- Node.js (CI uses Node 18)
- Express (`express` dependency)
- SQLite (`sqlite3` dependency)
- `dotenv` for configuration
- `cors` middleware

## 7.2 Entry Point

- `pos-backend/index.js`
  - Loads environment variables (`dotenv`)
  - Configures Express (`cors`, JSON body parsing)
  - Serves static files from `pos-backend/public`
  - Initializes SQLite schema via `init()` from `pos-backend/db.js`
  - Registers routes:
    - `/diagnostics` -> `routes/diagnostics.js`
    - `/terminals` -> `routes/terminal.js`
    - `/auth` -> `routes/auth.js`
  - Provides `/health` endpoint (JSON)

## 7.3 Configuration

- `pos-backend/.env` contains:
  - `API_KEY=supersecret123`
  - `PORT=4000`
  - `DB_FILE=diagnostics.db`

> **Note:** `API_KEY` is a shared secret used by the mobile app and some dashboard endpoints.

## 7.4 Backend Routes (Overview)

- `/health` (GET): returns `{ status, ts }`
- `/diagnostics`:
  - POST `/diagnostics`: accepts diagnostics JSON (requires header `x-api-key`)
  - GET `/diagnostics`: returns stored diagnostics; optional `terminal_id` query
- `/terminals`:
  - Middleware checks API key on all `/terminals` requests (`x-api-key` header OR `api_key` query string)
  - GET `/terminals`: returns terminals with last status and last diagnostic time
  - GET `/terminals/stats`: aggregated terminal stats
- `/auth`:
  - POST `/auth/signup`
  - POST `/auth/login`
  - GET `/auth/users`
  - PATCH `/auth/users/:id`
  - DELETE `/auth/users/:id`

\pagebreak

# 8. Dashboard UI: `pos-backend/public`

## 8.1 Architecture

The dashboard is a set of static HTML pages served by Express. It uses JavaScript `fetch()` to call backend endpoints.

**Pages observed:**

- `login.html` — login UI
- `signup.html` — create account (pending approval)
- `dashboard.html` — main overview
- `devices.html` — device list and state
- `daily_report.html` — reporting view
- `test_logs.html` — diagnostics listing + CSV download
- `users.html` — admin user management
- `recycle_bin.html` — deleted user restore/permanent delete
- `settings.html`, `profile.html`, `register.html`, `index.html`

## 8.2 Shared JavaScript Helper

- `public/js/data_fetcher.js`
  - Contains helper functions: `fetchDiagnostics()`, `fetchTerminals()`, `fetchUsers()`
  - Uses an API base URL detection strategy and a constant API key.

## 8.3 Screenshot Placeholders

[INSERT FIGURE D1: Login Page Screenshot]

[INSERT FIGURE D2: Dashboard Overview Screenshot]

[INSERT FIGURE D3: Devices Page Screenshot]

[INSERT FIGURE D4: Test Logs & CSV Download Screenshot]

[INSERT FIGURE D5: User Management Screenshot]

[INSERT FIGURE D6: Recycle Bin Screenshot]

\pagebreak

# 9. Mobile App: `pos-mobile-app`

## 9.1 Technology Stack

- Android application
- Language: Java
- AndroidX + Material components
- Feitian SDK JAR: `FTSDK_api_V1.0.1.11_20241029.jar`

## 9.2 Build & Project Configuration

- `pos-mobile-app/settings.gradle`: `rootProject.name = "FeitianDiagnostics"`, includes `:app`
- `pos-mobile-app/app/build.gradle`:
  - `namespace 'com.feitian.diagnostics'`
  - `compileSdk 36`, `minSdk 26`, `targetSdk 36`
  - `buildConfigField API_KEY = "supersecret123"`
  - Includes Feitian SDK JAR and any JAR/AAR in libs

## 9.3 Android Manifest

- `app/src/main/AndroidManifest.xml`
  - Permissions:
    - INTERNET
    - ACCESS_NETWORK_STATE
    - ACCESS_WIFI_STATE
    - READ_PHONE_STATE
    - NFC
    - ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
  - `android:usesCleartextTraffic="true"`
  - `android:networkSecurityConfig="@xml/network_security_config"`
  - Activities:
    - `MainActivity` (launcher)
    - `HistoryActivity`
    - `TestDetailActivity`
    - `DiagnosticsFlowActivity`
    - `TouchTestActivity`

## 9.4 Mobile App Functional Overview

### 9.4.1 Main UI (`MainActivity`)

- Displays test modules in a grid.
- Binds Feitian hardware services on start via `ServiceManager.bindPosServer`.
- Triggers diagnostics flow (`DiagnosticsFlowActivity`) or runs individual tests.

### 9.4.2 Diagnostics Flow (`DiagnosticsFlowActivity`)

- Runs all tests using `DiagnosticRunner.runAll()`.
- Presents overlays for interactive tests (NFC, IC card, charging, touch).
- Builds a JSON payload using `DiagnosticsPayloadBuilder.build()`.
- Appends the payload line to a local history file `diagnostics_history.json`.
- Uploads payload to backend using `DiagnosticsApiClient.send(payload)`.
- Can print a receipt using `PrinterHelper.printReceipt()`.

### 9.4.3 History & Export (`HistoryActivity`)

- Reads `diagnostics_history.json` from internal storage.
- Computes pass/fail stats.
- Allows filtering displayed entries.
- Exports history to CSV and shares it using `FileProvider`.

### 9.4.4 Touch Test (`TouchTestActivity` + `DrawingView`)

- Presents a canvas and countdown for touchscreen interaction.
- Registers touch via `TouchscreenTestV2.registerTouch()`.

\pagebreak

# 10. API Specification

> **Important:** This section is derived from the actual backend route handlers.

## 10.1 Health

### GET `/health`

- **Response 200 (JSON):**
  - `{ "status": "ok", "ts": "<iso timestamp>" }`

## 10.2 Diagnostics

### POST `/diagnostics`

- **Purpose:** Device uploads a diagnostics payload.
- **Auth:** requires header `x-api-key` matching `API_KEY`.
- **Request body:** JSON containing at minimum:
  - `terminal_id` (string)
  - `results` (array)

- **Response:**
  - `201 { "status": "stored" }`

### GET `/diagnostics`

- **Purpose:** Dashboard reads diagnostics history.
- **Query params:** `terminal_id` optional
- **Response:** array of diagnostics:
  - `id`
  - `receivedAt`
  - `summaryStatus`
  - `payload` (parsed JSON)

> **Note:** In current code, payload parsing uses `JSON.parse(r.payload)`.

## 10.3 Terminals

### GET `/terminals`

- **Auth:** API key required (`x-api-key` header OR `api_key` query param)
- **Response fields:** includes terminal metadata plus computed fields:
  - `last_status`
  - `last_diag_time`

### GET `/terminals/stats`

- **Auth:** API key required
- **Response:** `{ total, passed, failed, not_tested }`

## 10.4 Auth / Users

### POST `/auth/signup`

- **Request:** `{ username, password }`
- **Response 201:** pending account

### POST `/auth/login`

- **Request:** `{ username, password }`
- **Response 200:** `{ status: "ok", user: { username, role } }`

> **Important security note:** Passwords are currently matched in plaintext.

### GET `/auth/users`

- Returns array of users:
  - `id, username, role, status, created_at, updated_at`

### PATCH `/auth/users/:id`

- Updates `status` and/or `role`, sets `updated_at`.

### DELETE `/auth/users/:id`

- Permanently deletes a user row (blocked for `admin`).

\pagebreak

# 11. Database Design (SQLite)

## 11.1 Database File

- Default DB file: `pos-backend/diagnostics.db` (SQLite)

## 11.2 Tables

### 11.2.1 `diagnostics`

Columns:

- `id` INTEGER PRIMARY KEY AUTOINCREMENT
- `terminal_id` TEXT
- `device_id` TEXT
- `received_at` TEXT
- `summary_status` TEXT
- `payload` TEXT (JSON serialized)

### 11.2.2 `terminals`

Columns:

- `terminal_id` TEXT PRIMARY KEY
- `manufacturer` TEXT
- `model` TEXT
- `android_version` TEXT
- `sdk_level` INTEGER
- `last_seen` TEXT

### 11.2.3 `users`

Columns:

- `id` INTEGER PRIMARY KEY AUTOINCREMENT
- `username` TEXT UNIQUE
- `password` TEXT
- `role` TEXT DEFAULT 'technician'
- `status` TEXT DEFAULT 'pending'
- `created_at` TEXT
- `updated_at` TEXT

## 11.3 Default Admin Provisioning

- On init, the backend inserts (or ignores if exists):
  - username: `admin`
  - password: `admin123`
  - role: `admin`
  - status: `approved`

\pagebreak

# 12. Data Flow & UML (Placeholders)

## 12.1 Sequence: Mobile Upload

[INSERT UML DIAGRAM S1: Sequence Diagram — Mobile App Uploads Diagnostics]

Suggested participants:

- `DiagnosticsFlowActivity`
- `DiagnosticRunner`
- `DiagnosticsPayloadBuilder`
- `DiagnosticsApiClient`
- Backend `/diagnostics` route
- SQLite DB

## 12.2 Sequence: Dashboard Loads Data

[INSERT UML DIAGRAM S2: Sequence Diagram — Dashboard Fetches Terminals and Diagnostics]

Suggested participants:

- Dashboard page JS
- `/terminals` endpoint
- `/diagnostics` endpoint
- SQLite

## 12.3 Use Cases

[INSERT UML DIAGRAM U1: Use Case Diagram]

Suggested actors:

- Technician
- Admin
- POS Device (Mobile App)

## 12.4 Class Diagram (Mobile)

[INSERT UML DIAGRAM C1: Mobile App Class Diagram]

Include:

- `DiagnosticRunner`
- `DiagnosticTest` interface
- `DiagnosticResult`
- individual tests (Battery, Network, etc.)

\pagebreak

# 13. Security Review and Hardening Plan

## 13.1 Current Security Posture (As Implemented)

- API key shared secret (`supersecret123`) used for:
  - Mobile app uploads
  - Some dashboard reads via query string (`api_key`)
- Dashboard “auth” is primarily client-side (stores user in browser storage)
- Passwords stored and compared in plaintext

## 13.2 Risks

- Shared API key can be extracted and abused
- Client-side role claims can be spoofed
- No strong session handling for dashboard

## 13.3 Recommended Improvements (Roadmap)

- Hash passwords (`bcrypt`) and implement real sessions (httpOnly cookies)
- Enforce server-side RBAC for admin endpoints
- Replace global API key with per-device tokens or signed requests
- Serve over HTTPS

\pagebreak

# 14. Testing & Quality Assurance

## 14.1 Current State

- Backend: no automated test suite; CI runs `node index.js` as a sanity check.
- Mobile app: includes default Android test scaffolding.

## 14.2 Recommended Additions

- Backend integration tests for:
  - `/diagnostics` POST
  - `/terminals` GET
  - `/auth` flows
- Mobile instrumentation tests for:
  - Diagnostics payload generation

\pagebreak

# 15. CI/CD and Automation

## 15.1 GitHub Actions

- `.github/workflows/backend-ci.yml`
  - Triggers on push/PR to `master`
  - Installs backend dependencies
  - Runs `node index.js` and sleeps 5 seconds

## 15.2 Recommended CI Enhancements

- Add lint checks
- Add unit/integration test execution
- Build Android APK in CI

\pagebreak

# 16. Deployment & Operations

## 16.1 Backend Deployment

- Run from `pos-backend`:
  - Install dependencies: `npm install`
  - Start server: `npm run start`

## 16.2 Mobile Deployment

- Build APK from Android Studio / Gradle
- Sign using keystore (sensitive): `keystore/feitian-release.jks`

> **Security note:** Never commit keystore passwords in the repo.

## 16.3 Network Considerations

- Mobile app currently posts to a hardcoded endpoint:
  - `http://192.168.100.42:4000/diagnostics`
- Android allows cleartext traffic to selected LAN domains via `network_security_config.xml`.

\pagebreak

# 17. Risks, Constraints, and Assumptions

## 17.1 Known Constraints

- SQLite is single-file and best suited to small-to-medium loads.
- Hardcoded endpoint/IP in mobile client requires updating for new environments.

## 17.2 Assumptions

- Backend reachable on LAN by POS terminals.
- Device has required Feitian services for hardware tests.

\pagebreak

# 18. Maintenance & Roadmap

## 18.1 Short-Term

- Secure authentication (bcrypt + sessions)
- Remove shared API key from dashboard browser

## 18.2 Medium-Term

- Improved dashboards and trend visualizations
- Device enrollment and per-device credentials

## 18.3 Long-Term

- Cloud deployment option
- Centralized logging and monitoring

\pagebreak

# 19. Appendices

## Appendix A — Screenshot Index (Placeholders)

- [INSERT FIGURE A-1: Mobile App — Main Screen]
- [INSERT FIGURE A-2: Mobile App — Diagnostics Flow]
- [INSERT FIGURE A-3: Mobile App — Test Detail]
- [INSERT FIGURE A-4: Mobile App — History + CSV Export]
- [INSERT FIGURE A-5: Dashboard — Login]
- [INSERT FIGURE A-6: Dashboard — Overview]
- [INSERT FIGURE A-7: Dashboard — Devices]
- [INSERT FIGURE A-8: Dashboard — Reports]
- [INSERT FIGURE A-9: Dashboard — Test Logs]
- [INSERT FIGURE A-10: Dashboard — Users]

## Appendix B — UML Diagram Index (Placeholders)

- [INSERT UML B-1: Use Case Diagram]
- [INSERT UML B-2: Component Diagram]
- [INSERT UML B-3: Deployment Diagram]
- [INSERT UML B-4: Sequence Diagram — Upload]
- [INSERT UML B-5: Sequence Diagram — Dashboard View]
- [INSERT UML B-6: Class Diagram — Mobile]

## Appendix C — Glossary

- **POS:** Point of Sale
- **SDK:** Software Development Kit
- **RBAC:** Role-Based Access Control
- **SQLite:** Embedded relational database

\pagebreak

# Page Expansion Sections (to reach 60+ pages)

> The sections below provide additional depth and are designed to expand the document when exported to PDF.
> Add screenshots and UML diagrams in the indicated placeholders to further increase the report length and presentation quality.

## P1. Detailed Mobile Diagnostics Test Specifications

### P1.1 Battery Health (`BatteryTestV2`)

- Inputs: BatteryManager extras
- Outputs: Level %, temp, voltage, health label
- Status mapping: GOOD -> PASS; overheat/dead/over-voltage -> FAIL; else SKIPPED

### P1.2 Network Connectivity (`NetworkTestV2`)

- Uses ConnectivityManager + NetworkCapabilities
- Status PASS if NET_CAPABILITY_INTERNET present

### P1.3 Signal Strength (`SignalStrengthTestV2`)

- WiFi RSSI for WiFi
- SIM level for cellular (API 28+)
- Requires location permission for SIM signal

### P1.4 Storage & Security (`StorageTestV2`)

- Storage free space warning if < 1GB
- Basic root detection methods

### P1.5 Touchscreen (`TouchscreenTestV2`, `TouchTestActivity`, `DrawingView`)

- Latch wait up to 30s
- PASS if touchDetected

### P1.6 NFC (`NfcTestV2`)

- Uses Feitian NFC SDK
- PASS on card ATR
- FAIL on error/timeout

### P1.7 IC Card (`CardReaderTestV2.IcReaderTest`)

- Uses Feitian IC reader SDK
- PASS if ATR detected

### P1.8 Charging Port (`ChargingPortTestV2`)

- Polls battery plugged status up to 10s

### P1.9 LED/Buzzer (`LedBuzzerTestV2`)

- Attempts buzzer + LED on/off

### P1.10 Printer (`PrinterHelper`, `PrinterTestV2`)

- Prints receipt of results

\pagebreak

## P2. Detailed Backend Design Notes

### P2.1 Request Validation

- Device upload validates `terminal_id` and `results` array.

### P2.2 Terminal Auto-Registration

- On diagnostic POST, backend UPSERTs terminal metadata.

### P2.3 Summary Status Computation

- `computeSummaryStatus()` checks for FAIL in results; legacy fallbacks.

\pagebreak

## P3. Suggested Improvements (Backlog)

- Replace hard-coded LAN IP in mobile app with configurable environment.
- Replace API key with per-device tokens.
- Implement secure auth for dashboard.

\pagebreak

## P4. Placeholder Pages for Screenshots

[INSERT FULL-PAGE SCREENSHOT: Mobile Main Screen]

\pagebreak

[INSERT FULL-PAGE SCREENSHOT: Mobile Diagnostics Flow]

\pagebreak

[INSERT FULL-PAGE SCREENSHOT: Dashboard Overview]

\pagebreak

[INSERT FULL-PAGE UML DIAGRAM: Component Diagram]

\pagebreak

[INSERT FULL-PAGE UML DIAGRAM: Sequence Upload]

\pagebreak

[INSERT FULL-PAGE UML DIAGRAM: Deployment Diagram]

\pagebreak

## P5. Revision History

| Version | Date | Author | Changes |
|---|---|---|---|
| 1.0 | _[INSERT]_ | _[INSERT]_ | Initial draft |

\pagebreak

## P6. Sign-Off

| Role | Name | Signature | Date |
|---|---|---|---|
| Sponsor | _[TBD]_ | _[TBD]_ | _[TBD]_ |
| Engineering Lead | _[TBD]_ | _[TBD]_ | _[TBD]_ |
| Security | _[TBD]_ | _[TBD]_ | _[TBD]_ |

\pagebreak

**End of Report**
