const express = require("express");
const router = express.Router();
const { db } = require("../db");

const API_KEY = process.env.API_KEY || "supersecret123";

// Simple middleware (API key for all requests)
router.use((req, res, next) => {
    const key = req.header("x-api-key") || req.query.api_key;
    if (!key || key !== API_KEY) {
        return res.status(401).json({ error: "Unauthorized" });
    }
    next();
});

// GET /terminals - Reliable listing
router.get("/", (req, res) => {
    db.all(`
        SELECT t.*, 
        (SELECT summary_status FROM diagnostics WHERE terminal_id = t.terminal_id OR device_id = t.terminal_id ORDER BY id DESC LIMIT 1) as last_status,
        (SELECT received_at FROM diagnostics WHERE terminal_id = t.terminal_id OR device_id = t.terminal_id ORDER BY id DESC LIMIT 1) as last_diag_time
        FROM terminals t 
        ORDER BY last_seen DESC
    `, [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// GET /terminals/stats
router.get("/stats", (req, res) => {
    const sql = `
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN last_status = 'pass' THEN 1 ELSE 0 END) as passed,
            SUM(CASE WHEN last_status = 'fail' THEN 1 ELSE 0 END) as failed,
            SUM(CASE WHEN last_status IS NULL THEN 1 ELSE 0 END) as not_tested
        FROM (
            SELECT t.terminal_id,
            (SELECT summary_status FROM diagnostics WHERE terminal_id = t.terminal_id OR device_id = t.terminal_id ORDER BY id DESC LIMIT 1) as last_status
            FROM terminals t
        )
    `;
    db.get(sql, [], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(row);
    });
});

module.exports = router;
