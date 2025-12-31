const sqlite3 = require('sqlite3').verbose();
require("dotenv").config();

const db = new sqlite3.Database(process.env.DB_FILE || "./diagnostics.db");

function init(callback) {
    db.serialize(() => {
        // Create tables
        db.run(`
        CREATE TABLE IF NOT EXISTS diagnostics (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          terminal_id TEXT,
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

        // Migration helper: Ensure terminal_id exists in diagnostics
        db.run("ALTER TABLE diagnostics ADD COLUMN terminal_id TEXT", (err) => {
            // Ignore "duplicate column" errors
        });

        // Final check: If the code still tries to use device_id somewhere, let's add it too
        db.run("ALTER TABLE diagnostics ADD COLUMN device_id TEXT", (err) => {
            // Ignore "duplicate column" errors
        });

        console.log("Database initialized and migrations checked.");
        if (callback) callback();
    });
}

module.exports = { db, init };
