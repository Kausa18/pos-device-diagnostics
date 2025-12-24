const sqlite3 = require('sqlite3').verbose();
require("dotenv").config();

const db = new sqlite3.Database(process.env.DB_FILE || "./diagnostics.db");

function init(callback) {
    db.serialize(() => {
        db.run(`
        CREATE TABLE IF NOT EXISTS diagnostics (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          terminal_id TEXT,
          received_at TEXT,
          summary_status TEXT,
          payload TEXT
        )
        `, (err) => {
            if (err) {
                console.error("Error creating diagnostics table:", err.message);
            } else {
                console.log("Diagnostics table ready.");
            }
        });

        db.run(`
        CREATE TABLE IF NOT EXISTS terminals (
            terminal_id TEXT PRIMARY KEY,
            manufacturer TEXT,
            model TEXT,
            android_version TEXT,
            sdk_level INTEGER,
            last_seen TEXT
        )
        `, (err) => {
            if (err) {
                console.error("Error creating terminals table:", err.message);
            } else {
                console.log("Terminals table ready.");
            }
            // Call callback after both tables are attempted
            callback();
        });
    });
}

module.exports = { db, init };
