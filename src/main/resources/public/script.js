const out = document.getElementById('out');

document.getElementById('time').onclick = async () => {
    const r = await fetch('/api/time');
    const j = await r.json();
    document.getElementById('out').textContent = JSON.stringify(j, null, 2);
};

// POST: enviar mensaje al servidor
document.getElementById('echo').onclick = async () => {
    const msg = { message: "Hola servidor desde el cliente <3" };

    const r = await fetch('/api/echo', {
        method: 'POST',
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(msg)
    });

    const j = await r.json();
    out.textContent = JSON.stringify(j, null, 2);
};
