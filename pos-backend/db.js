const sqlite3 = require('sqlite3').verbose();
require("dotenv").config();

const db = new sqlite3.Database(process.env.DB_FILE || "./diagnostics.db");

db.serialize(() => {
    db.run(`
    CREATE TABLE IF NOT EXISTS diagnostics (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      terminal_id TEXT,
      timestamp_utc TEXT,
      summary_status TEXT,
      payload_json TEXT,
      created_at TEXT DEFAULT (datetime('now', 'localtime'))
    )
  `);
});

module.exports = db;
