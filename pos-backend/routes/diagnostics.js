const express = require("express");
const router = express.Router();
const db = require("../db");

// authentication
router.use((req, res, next) => {
    const key = req.headers["x-api-key"];
    if (!key || key !== process.env.API_KEY) {
        return res.status(401).json({ error: "Unauthorized" });
    }
    next();
});

// POST /diagnostics
router.post("/", (req, res) => {
    const report = req.body;

    if (!report.terminal_id) {
        return res.status(400).json({ error: "terminal_id is required" });
    }

    const status = computeSummaryStatus(report);

    const sql = `
    INSERT INTO diagnostics (terminal_id, timestamp_utc, summary_status, payload_json)
    VALUES (?, ?, ?, ?)
  `;

    const params = [
        report.terminal_id,
        report.timestamp_utc,
        status,
        JSON.stringify(report)
    ];

    db.run(sql, params, function (err) {
        if (err) return res.status(500).json({ error: err.message });
        res.status(201).json({ status: "ok", id: this.lastID });
    });
});

// GET /diagnostics
router.get("/", (req, res) => {
  const { status, terminal_id, limit = 50 } = req.query;

  let sql = `
    SELECT id, terminal_id, timestamp_utc, summary_status, created_at
    FROM diagnostics
    WHERE 1=1
  `;

  const params = [];

  // strict filter on status
  if (typeof status === "string" && (status === "pass" || status === "fail" || status === "unknown")) {
    sql += " AND summary_status = ?";
    params.push(status);
  }

  // filter by terminal ID
  if (typeof terminal_id === "string" && terminal_id.trim() !== "") {
    sql += " AND terminal_id = ?";
    params.push(terminal_id.trim());
  }

  sql += " ORDER BY id DESC LIMIT ?";
  params.push(Number(limit) || 50);

  db.all(sql, params, (err, rows) => {
    if (err) {
      console.error("SQL ERROR:", err);
      return res.status(500).json({ error: err.message });
    }
    res.json(rows);
  });
});


// GET /diagnostics/:id
router.get("/:id", (req, res) => {
    const { id } = req.params;

    const sql = `
    SELECT *
    FROM diagnostics
    WHERE id = ?
  `;

    db.get(sql, [id], (err, row) => {
        if (err) return res.status(500).json({ error: err.message });
        if (!row) return res.status(404).json({ error: "Not found" });

        // Parse JSON before returning
        row.payload_json = JSON.parse(row.payload_json);

        res.json(row);
    });
});

/**
 * DELETE /diagnostics/:id
 * Remove a specific diagnostic (useful for tests)
 */
router.delete("/:id", (req, res) => {
    const id = Number(req.params.id);
    if (!id) return res.status(400).json({ error: "invalid id" });

    const sql = `DELETE FROM diagnostics WHERE id = ?`;
    db.run(sql, [id], function (err) {
        if (err) return res.status(500).json({ error: err.message });
        return res.json({ status: "ok", deleted: this.changes });
    });
});

/**
 * POST /diagnostics/reset
 * Clear all diagnostics (development only)
 */
router.post("/reset", (req, res) => {
    const sql = `DELETE FROM diagnostics`;
    db.run(sql, [], function (err) {
        if (err) return res.status(500).json({ error: err.message });
        return res.json({ status: "ok", deleted: this.changes });
    });
});

function computeSummaryStatus(r) {
    try {
        const hwFail = r.hardware && Object.values(r.hardware).some(t => t.status === "fail");
        const netFail = r.network && r.network.server_reachable === false;
        const txFail = r.transaction_test && r.transaction_test.status === "fail";

        if (hwFail || netFail || txFail) return "fail";
        return "pass";
    } catch {
        return "unknown";
    }
}

module.exports = router;
