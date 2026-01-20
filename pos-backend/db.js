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
            created_at TEXT,
            updated_at TEXT
        )
        `);

        const finalize = () => {
            const now = new Date().toISOString();
            db.run(`
                INSERT OR IGNORE INTO users (username, password, role, status, created_at, updated_at)
                VALUES ('admin', 'admin123', 'admin', 'approved', ?, ?)
            `, [now, now], (insertErr) => {
                if (insertErr) {
                    if (callback) return callback(insertErr);
                    return;
                }
                console.log("Database initialized with Users table.");
                if (callback) callback();
            });
        };

        db.all("PRAGMA table_info(users)", [], (err, columns) => {
            if (err) {
                if (callback) return callback(err);
                return;
            }
            const hasUpdatedAt = Array.isArray(columns) && columns.some(c => c && c.name === 'updated_at');
            if (hasUpdatedAt) return finalize();

            db.run("ALTER TABLE users ADD COLUMN updated_at TEXT", [], (alterErr) => {
                if (alterErr) {
                    console.error("Failed to migrate users table:", alterErr);
                    if (callback) return callback(alterErr);
                    return;
                }
                db.run("UPDATE users SET updated_at = COALESCE(updated_at, created_at)", [], () => finalize());
            });
        });
    });
}

module.exports = { db, init };
