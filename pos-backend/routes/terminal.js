const express = require("express");
const router = express.Router();
const db = require("../db");

// middleware (reuse API key)
router.use((req, res, next) => {
    const key = req.headers["x-api-key"];
    if (!key || key !== process.env.API_KEY) {
        return res.status(401).json({ error: "Unauthorized" });
    }
    next();
});

// GET /terminals
router.get("/", (req, res) => {
    const sql = `
    SELECT terminal_id,
           COUNT(*) AS total_reports,
           MAX(created_at) AS last_report,
           (SELECT summary_status
            FROM diagnostics d2
            WHERE d2.terminal_id = d1.terminal_id
            ORDER BY id DESC LIMIT 1) AS last_status
    FROM diagnostics d1
    GROUP BY terminal_id
    ORDER BY last_report DESC
  `;

    db.all(sql, [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

module.exports = router;
