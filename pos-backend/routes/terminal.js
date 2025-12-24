const express = require("express");
const router = express.Router();
const { db } = require("../db");

// middleware (reuse API key)
router.use((req, res, next) => {
    const key = req.headers["x-api-key"];
    if (!key || key !== process.env.API_KEY) {
        return res.status(401).json({ error: "Unauthorized" });
    }
    next();
});

router.get("/", (req, res) => {
    db.all("SELECT * FROM terminals ORDER BY last_seen DESC", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

module.exports = router;
