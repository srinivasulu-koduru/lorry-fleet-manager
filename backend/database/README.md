# MySQL 8.4 Database Setup Guide

This guide explains how to set up the **`lorry_fleet_db`** database for the Lorry Fleet Manager Spring Boot backend on Windows using **MySQL Server 8.4 LTS** (or compatible MySQL 8.0+).

---

## 1. Overview of Database Setup

In this foundational stage:
* The database schema **`lorry_fleet_db`** is created.
* Character set is set to **`utf8mb4`** (full Unicode support, including emojis and international text).
* Collation is set to **`utf8mb4_unicode_ci`** (standard multilingual, case-insensitive sorting).
* **No tables are created yet**: Tables will be introduced in future stages once the entity models (Owners, Drivers, Lorries, Trips) are finalized.
* The script is **idempotent and safe**: Running it multiple times will not drop, alter, or erase any existing databases or data (`CREATE DATABASE IF NOT EXISTS`).

---

## 2. How to Execute the SQL Script on Windows

Choose any one of the following methods that best fits your local setup:

### Method A: Using MySQL Command Line Client (Easiest for Beginners)

1. Open the Windows Start menu and search for **MySQL 8.4 Command Line Client** (or **MySQL Command Line Client**).
2. Enter your MySQL `root` password when prompted.
3. In the MySQL prompt, run the script using the `source` command (use forward slashes `/` in file paths):
   ```sql
   source D:/lorry-fleet-manager/backend/database/create-database.sql;
   ```
   *(Alternatively, open [`create-database.sql`](create-database.sql) in any text editor, copy the contents, paste them into the MySQL client window, and press Enter).*

---

### Method B: Using PowerShell or Windows Terminal

1. Open **PowerShell** as an everyday user.
2. If the MySQL binary directory (`C:\Program Files\MySQL\MySQL Server 8.4\bin`) is in your system `PATH`, run:
   ```powershell
   Get-Content "D:\lorry-fleet-manager\backend\database\create-database.sql" | mysql -u root -p
   ```
   Enter your MySQL `root` password when prompted.

3. Alternatively, you can log in to the MySQL client interactively:
   ```powershell
   mysql -u root -p
   ```
   Then run:
   ```sql
   source D:/lorry-fleet-manager/backend/database/create-database.sql;
   ```

---

### Method C: Using MySQL Workbench (GUI)

1. Launch **MySQL Workbench**.
2. Under **MySQL Connections**, click your local instance (e.g., `Local instance MySQL84`) and enter your password.
3. In the top menu, go to **File** > **Open SQL Script...** (or press `Ctrl + Shift + O`).
4. Browse to `D:\lorry-fleet-manager\backend\database\create-database.sql` and click **Open**.
5. Click the **Execute (lightning bolt)** icon on the toolbar to run the script.

---

### Method D: Using MySQL Shell (`mysqlsh`)

1. Open PowerShell or Command Prompt.
2. Start MySQL Shell in SQL mode:
   ```powershell
   mysqlsh --sql
   ```
3. Connect to your local server:
   ```sql
   \connect root@localhost:3306
   ```
   Enter your `root` password when prompted.
4. Execute the SQL script:
   ```sql
   \source D:/lorry-fleet-manager/backend/database/create-database.sql
   ```

---

## 3. How to Verify That the Database Exists

After executing the script, run the following verification commands in your MySQL client:

### Step 1: Check Database List
```sql
SHOW DATABASES;
```
You should see **`lorry_fleet_db`** listed in the output.

### Step 2: Verify Character Set and Collation
Run this query to confirm that the database was created with the correct encoding:
```sql
SELECT schema_name, default_character_set_name, default_collation_name
FROM information_schema.schemata
WHERE schema_name = 'lorry_fleet_db';
```

**Expected output:**
| schema_name | default_character_set_name | default_collation_name |
| :--- | :--- | :--- |
| **lorry_fleet_db** | **utf8mb4** | **utf8mb4_unicode_ci** |

---

## 4. Security Best Practice: Create a Dedicated Application User

> [!IMPORTANT]
> **Never use the MySQL `root` account for daily application operations or production deployments.**
> The `root` account possesses unrestricted administrative privileges across the entire MySQL server. If compromised, an attacker could drop all databases or alter server configurations.

Following the principle of **least privilege**, create a dedicated application user that only has access to `lorry_fleet_db`:

### SQL Commands to Create the Application User:

Run these commands in your MySQL client as `root`:

```sql
-- 1. Create a dedicated application user
-- Replace 'choose_a_strong_password_here' with your own secure password
CREATE USER IF NOT EXISTS 'lorry_app_user'@'localhost' IDENTIFIED BY 'choose_a_strong_password_here';

-- 2. Grant permissions ONLY to the lorry_fleet_db database
GRANT ALL PRIVILEGES ON lorry_fleet_db.* TO 'lorry_app_user'@'localhost';

-- 3. Flush privileges to apply changes immediately
FLUSH PRIVILEGES;
```

---

## 5. Configuring Spring Boot Environment Variables

The Spring Boot backend in [`backend/src/main/resources/application.yml`](../src/main/resources/application.yml) reads database credentials from environment variables:

| Variable | Recommended Value | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:mysql://localhost:3306/lorry_fleet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC connection URL pointing to the newly created database. |
| `DB_USERNAME` | `lorry_app_user` | Dedicated application user (or `root` for initial local testing). |
| `DB_PASSWORD` | *(Your user's password)* | Password for the designated database user. |

### Notes on the `DB_URL` Parameters:
* `useSSL=false`: Standard for local development connections over loopback (`localhost`). For remote production servers, SSL should be enabled (`useSSL=true`).
* `allowPublicKeyRetrieval=true`: Required for MySQL 8.0+ and 8.4 LTS when using the default `caching_sha2_password` authentication plugin without SSL.
* `serverTimezone=UTC`: Ensures consistent timezone handling between Java 21 and MySQL.

### Setting the Environment Variables in PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/lorry_fleet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "lorry_app_user"
$env:DB_PASSWORD = "choose_a_strong_password_here"
```

A template file is also provided at [`backend/.env.example`](../.env.example) for local reference.

---

## 6. Testing Backend Connectivity

Once you have executed `create-database.sql` and set your environment variables, you can test starting the Spring Boot backend:

```powershell
Set-Location -Path "D:\lorry-fleet-manager\backend"
.\mvnw.cmd spring-boot:run
```

When the application boots:
1. HikariCP will establish a connection to `lorry_fleet_db`.
2. Hibernate will validate the schema (`ddl-auto: validate`). Since no entities exist yet, validation completes immediately.
3. The server starts on port `8080`.
4. You can test the health endpoint in another PowerShell window:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get | ConvertTo-Json
   ```
   **Response:**
   ```json
   {
     "status": "UP",
     "application": "Lorry Owner Backend"
   }
   ```
