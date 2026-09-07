const express = require('express');
const fs = require('fs');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

const DB_FILE = path.join(__dirname, 'database.json');

// Initialize database file if not exists
if (!fs.existsSync(DB_FILE)) {
    const initialData = {
        sites: [],
        transactions: [],
        staff: [],
        attendance: [],
        salary_records: [],
        users: []
    };
    fs.writeFileSync(DB_FILE, JSON.stringify(initialData, null, 2));
}

function readDb() {
    try {
        const data = fs.readFileSync(DB_FILE, 'utf8');
        return JSON.parse(data);
    } catch (e) {
        return { sites: [], transactions: [], staff: [], attendance: [], salary_records: [], users: [] };
    }
}

function writeDb(data) {
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
}

// API: Get all data
app.get('/api/data', (req, res) => {
    const db = readDb();
    res.json(db);
});

// API: Sync / Update data from Mobile App
app.post('/api/sync', (req, res) => {
    try {
        const incoming = req.body;
        const db = readDb();
        
        // Merge or replace incoming collections
        if (incoming.sites) db.sites = incoming.sites;
        if (incoming.transactions) db.transactions = incoming.transactions;
        if (incoming.staff) db.staff = incoming.staff;
        if (incoming.attendance) db.attendance = incoming.attendance;
        if (incoming.salary_records) db.salary_records = incoming.salary_records;
        if (incoming.users) db.users = incoming.users;

        writeDb(db);
        console.log(`[SYNC] Data updated at ${new Date().toLocaleString()}`);
        res.json({ status: 'success', message: 'Data synced successfully to PC!' });
    } catch (e) {
        console.error('Sync error:', e);
        res.status(500).json({ status: 'error', message: e.message });
    }
});

// Graphical Admin Dashboard HTML
app.get('/', (req, res) => {
    const db = readDb();
    res.send(`
    <!DOCTYPE html>
    <html lang="hi">
    <head>
        <meta charset="UTF-8">
        <title>RPVC PC Cloud Server Dashboard</title>
        <style>
            body { font-family: Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 20px; color: #333; }
            .container { max-width: 1000px; margin: auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
            h1 { color: #1e293b; margin-top: 0; display: flex; align-items: center; gap: 10px; }
            .badge { background: #22c55e; color: white; padding: 4px 12px; border-radius: 20px; font-size: 14px; }
            .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 20px 0; }
            .card { background: #f8fafc; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; text-align: center; }
            .card h3 { margin: 0; font-size: 28px; color: #0f172a; }
            .card p { margin: 5px 0 0; color: #64748b; font-size: 14px; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: left; }
            th { background: #f1f5f9; color: #475569; font-weight: 600; }
            .footer { margin-top: 30px; text-align: center; color: #94a3b8; font-size: 13px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🏗️ RPVC Construction PC Server <span class="badge">ONLINE</span></h1>
            <p>Aapka PC ab successfully Cloud Storage Server ban chuka hai. Mobile phones se saara data yahan live save ho raha hai.</p>
            
            <div class="stats">
                <div class="card">
                    <h3>${db.sites.length}</h3>
                    <p>Total Sites / Projects</p>
                </div>
                <div class="card">
                    <h3>${db.transactions.length}</h3>
                    <p>Transactions / Expenses</p>
                </div>
                <div class="card">
                    <h3>${db.staff.length}</h3>
                    <p>Staff / Workers</p>
                </div>
                <div class="card">
                    <h3>${db.attendance.length}</h3>
                    <p>Attendance Records</p>
                </div>
            </div>

            <h2>📋 Recent Transactions</h2>
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Type</th>
                        <th>Category</th>
                        <th>Amount</th>
                        <th>Added By</th>
                    </tr>
                </thead>
                <tbody>
                    ${db.transactions.slice(-10).reverse().map(t => `
                        <tr>
                            <td>${t.date || '-'}</td>
                            <td><b style="color: ${t.type === 'INCOME' ? 'green' : 'red'}">${t.type}</b></td>
                            <td>${t.category}</td>
                            <td>₹${t.amount}</td>
                            <td>${t.addedBy}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="5" style="text-align:center; color:#888;">Koi transaction nahi mili abhi tak.</td></tr>'}
                </tbody>
            </table>

            <div class="footer">
                RPVC Pvt Ltd Construction Management System &bull; Local Storage Folder: <code>database.json</code>
            </div>
        </div>
    </body>
    </html>
    `);
});

app.listen(PORT, '0.0.0.0', () => {
    console.log('==================================================');
    console.log(`🚀 RPVC PC Server is running successfully!`);
    console.log(`🌐 Local Dashboard: http://localhost:${PORT}`);
    console.log(`📱 Mobile Sync Endpoint: http://<YOUR_PC_IP>:${PORT}/api/sync`);
    console.log('==================================================');
});
