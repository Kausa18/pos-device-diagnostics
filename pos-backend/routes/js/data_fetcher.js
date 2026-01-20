const API_KEY = "supersecret123";
const BASE_URL = "http://localhost:4000";

async function fetchDiagnostics() {
    const res = await fetch(`${BASE_URL}/diagnostics?api_key=${API_KEY}`);
    if (!res.ok) throw new Error('Failed to fetch diagnostics');
    return await res.json();
}

async function fetchTerminals() {
    const res = await fetch(`${BASE_URL}/terminals?api_key=${API_KEY}`);
    if (!res.ok) throw new Error('Failed to fetch terminals');
    return await res.json();
}

async function fetchUsers() {
    const res = await fetch(`${BASE_URL}/auth/users`);
    if (!res.ok) throw new Error('Failed to fetch users');
    return await res.json();
}
