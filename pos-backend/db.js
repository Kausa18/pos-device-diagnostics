const sqlite3 = require('sqlite3').verbose();
require("dotenv").config();

const db = new sqlite3.Database(process.env.DB_FILE || "./diagnostics.db");

function init(callback) {
    db.serialize(() => {
        db.run(`
        CREATE TABLE IF NOT EXISTS diagnostics (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          terminal_id TEXT,
          device_id TEXT,
          received_at TEXT,
          summary_status TEXT,
          payload TEXT
        )
        `);

        db.run(`
        CREATE TABLE IF NOT EXISTS terminals (
            terminal_id TEXT PRIMARY KEY,
            manufacturer TEXT,
            model TEXT,
            android_version TEXT,
            sdk_level INTEGER,
            last_seen TEXT
        )
        `);

        db.run(`
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password TEXT,
            role TEXT DEFAULT 'technician',
            status TEXT DEFAULT 'pending',
            created_at TEXT
        )
        `);

        // Default Admin
        db.run(`
            INSERT OR IGNORE INTO users (username, password, role, status, created_at)
            VALUES ('admin', 'admin123', 'admin', 'approved', ?)
        `, [new Date().toISOString()]);

        console.log("Database initialized with Users table.");
        if (callback) callback();
    });
}

module.exports = { db, init };
