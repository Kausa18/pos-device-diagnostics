const express = require("express");
const router = express.Router();
const { db } = require("../db");

const API_KEY = process.env.API_KEY || "supersecret123";

// POST diagnostics
router.post("/", (req, res) => {
    const apiKey = req.header("x-api-key");

    if (apiKey !== API_KEY) {
        return res.status(401).json({ error: "Unauthorized" });
    }

    const payload = req.body;

    if (!payload || !payload.terminal_id || !payload.results) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    const status = computeSummaryStatus(payload);

    // Auto-register/update terminal
    db.run(`
        INSERT OR REPLACE INTO terminals (
            terminal_id,
            manufacturer,
            model,
            android_version,
            sdk_level,
            last_seen
        )
        VALUES (?, ?, ?, ?, ?, ?)
    `, [
        payload.terminal_id,
        payload.manufacturer || 'Unknown',
        payload.model || 'Unknown',
        payload.androidVersion || 'Unknown',
        payload.sdkLevel || null,
        new Date().toISOString()
    ], (err) => {
        if (err) {
            console.error("Error updating terminal:", err);
            // Continue anyway
        }

        // Insert diagnostic
        db.run(`
            INSERT INTO diagnostics (terminal_id, received_at, summary_status, payload)
            VALUES (?, ?, ?, ?)
        `, [payload.terminal_id, new Date().toISOString(), status, JSON.stringify(payload)], function(err) {
            if (err) return res.status(500).json({ error: err.message });
            res.status(201).json({ status: "stored" });
        });
    });
});

// GET diagnostics for dashboard
router.get("/", (req, res) => {
    const { terminal_id } = req.query;
    let sql = "SELECT * FROM diagnostics";
    let params = [];
    if (terminal_id) {
        sql += " WHERE terminal_id = ?";
        params.push(terminal_id);
    }
    sql += " ORDER BY id DESC";
    db.all(sql, params, (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        const result = rows.map(r => ({
            id: r.id,
            receivedAt: r.received_at,
            summaryStatus: r.summary_status,
            payload: JSON.parse(r.payload)
        }));
        res.json(result);
    });
});

module.exports = router;

function computeSummaryStatus(r) {
    try {
        // New format: results array
        if (r.results && Array.isArray(r.results)) {
            const hasFail = r.results.some(result => result.status === "FAIL");
            return hasFail ? "fail" : "pass";
        }
        
        // Legacy format support
        const hwFail = r.hardware && Object.values(r.hardware).some(t => t.status === "fail");
        const netFail = r.network && r.network.server_reachable === false;
        const txFail = r.transaction_test && r.transaction_test.status === "fail";

        if (hwFail || netFail || txFail) return "fail";
        return "pass";
    } catch {
        return "unknown";
    }
}
