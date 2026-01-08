require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");

const { init } = require("./db");

const app = express();
app.use(cors());
app.use(express.json({ limit: "10mb" }));

// serve static dashboard from public
app.use(express.static(path.join(__dirname, "public")));

// Default route redirects to login
app.get("/", (req, res) => {
    res.redirect("/login.html");
});

// Initialize database and then set up routes
init(() => {
    const diagnosticsRoute = require("./routes/diagnostics");
    const terminalsRoute = require("./routes/terminal");
    const authRoute = require("./routes/auth");

    // main API routes
    app.use("/diagnostics", diagnosticsRoute);
    app.use("/terminals", terminalsRoute);
    app.use("/auth", authRoute);

    // health check
    app.get("/health", (req, res) => res.json({ status: "ok", ts: new Date().toISOString() }));

    const port = process.env.PORT || 4000;
    app.listen(port, () => {
        console.log("Backend running on port " + port);
    });
});
