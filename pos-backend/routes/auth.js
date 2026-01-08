const express = require("express");
const router = express.Router();
const { db } = require("../db");

// POST /auth/signup
router.post("/signup", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: "Username and password required" });

    db.run(`
        INSERT INTO users (username, password, role, status, created_at)
        VALUES (?, ?, 'technician', 'pending', ?)
    `, [username, password, new Date().toISOString()], function(err) {
        if (err) {
            if (err.message.includes("UNIQUE constraint")) return res.status(400).json({ error: "Username already exists" });
            return res.status(500).json({ error: err.message });
        }
        res.status(201).json({ status: "created", message: "Account pending admin approval" });
    });
});

// POST /auth/login
router.post("/login", (req, res) => {
    const { username, password } = req.body;
    db.get("SELECT * FROM users WHERE username = ? AND password = ?", [username, password], (err, user) => {
        if (err) return res.status(500).json({ error: err.message });
        if (!user) return res.status(401).json({ error: "Invalid credentials" });
        if (user.status !== 'approved') return res.status(403).json({ error: "Your account is pending admin approval" });
        
        // Return user info (In real prod, use JWT)
        res.json({ 
            status: "ok", 
            user: { username: user.username, role: user.role } 
        });
    });
});

// GET /auth/users (Admin only)
router.get("/users", (req, res) => {
    db.all("SELECT id, username, role, status, created_at FROM users", [], (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(rows);
    });
});

// PATCH /auth/users/:id (Admin only - approve/role change)
router.patch("/users/:id", (req, res) => {
    const { status, role } = req.body;
    const { id } = req.params;
    
    db.run("UPDATE users SET status = COALESCE(?, status), role = COALESCE(?, role) WHERE id = ?", 
    [status, role, id], function(err) {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ status: "updated" });
    });
});

module.exports = router;
