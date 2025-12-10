require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");

const diagnosticsRoute = require("./routes/diagnostics");
const terminalsRoute = require("./routes/terminal");

const app = express();
app.use(cors());
app.use(express.json({ limit: "3mb" }));

// serve static dashboard
app.use(express.static(path.join(__dirname, "public")));

app.get("/", (req, res) => {
    res.send("PoS Backend Running");
});

// main API route
app.use("/diagnostics", diagnosticsRoute);
app.use("/terminals", terminalsRoute);

//good health check
app.get("/health", (req, res) => res.json({ status: "ok", ts: new Date().toISOString() }));

const port = process.env.PORT || 4000;
app.listen(process.env.PORT, () => {
    console.log("Backend running on port " + process.env.PORT);
});
