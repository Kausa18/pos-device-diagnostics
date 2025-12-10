async function loadData() {
    const res = await fetch('/diagnostics?limit=50', {
        headers: { "x-api-key": "supersecret123" }
    });

    const data = await res.json();
    const tbody = document.querySelector("#table tbody");

    tbody.innerHTML = "";

    data.forEach(d => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${d.id}</td>
      <td>${d.terminal_id}</td>
      <td>${d.summary_status}</td>
      <td>${d.timestamp_utc}</td>
    `;
        tbody.appendChild(tr);
    });
}

loadData();
