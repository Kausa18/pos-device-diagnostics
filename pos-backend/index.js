require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");

const { init } = require("./db");

const app = express();
app.use(cors());
app.use(express.json({ limit: "10mb" }));

// serve static dashboard
app.use(express.static(path.join(__dirname, "public")));

app.get("/", (req, res) => {
    res.sendFile(path.join(__dirname, "public", "index.html"));
});

// Initialize database and then set up routes
init(() => {
    const diagnosticsRoute = require("./routes/diagnostics");
    const terminalsRoute = require("./routes/terminal");

    // main API routes
    app.use("/diagnostics", diagnosticsRoute);
    app.use("/terminals", terminalsRoute);

    // good health check
    app.get("/health", (req, res) => res.json({ status: "ok", ts: new Date().toISOString() }));

    const port = process.env.PORT || 4000;
    app.listen(port, () => {
        console.log("Backend running on port " + port);
    });
});
