const API_KEY = "supersecret123";
const BASE_URL = (() => {
    const saved = localStorage.getItem('apiBase');
    if (saved && typeof saved === 'string') return saved.replace(/\/$/, '');
    if (window.location.protocol === 'file:') return 'http://localhost:4000';
    if (window.location.port && window.location.port !== '4000') return 'http://localhost:4000';
    return '';
})();

async function readErrorText(res) {
    try {
        const ct = (res.headers.get('content-type') || '').toLowerCase();
        if (ct.includes('application/json')) {
            const data = await res.json();
            return data && data.error ? String(data.error) : JSON.stringify(data);
        }
        return await res.text();
    } catch {
        return 'Request failed';
    }
}

async function fetchDiagnostics() {
    const res = await fetch(`${BASE_URL}/diagnostics?api_key=${API_KEY}`);
    if (!res.ok) throw new Error(await readErrorText(res));
    return await res.json();
}

async function fetchTerminals() {
    const res = await fetch(`${BASE_URL}/terminals?api_key=${API_KEY}`);
    if (!res.ok) throw new Error(await readErrorText(res));
    return await res.json();
}

async function fetchUsers() {
    const res = await fetch(`${BASE_URL}/auth/users`);
    if (!res.ok) throw new Error(await readErrorText(res));
    return await res.json();
}
