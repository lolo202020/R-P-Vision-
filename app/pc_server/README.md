# 🏗️ RPVC Construction App - Local PC Server

Yeh folder aapke PC ko **Private Cloud Storage Server** banane ke liye hai. Isse saara data (sites, expenses, staff, attendance) aapke PC par ek local folder mein secure rahega aur dono mobile phones (Admin & Site Incharge) isse sync kar sakenge.

---

### 🚀 PC par Server Chalane ke 3 Aasaan Steps:

#### Step 1: Node.js Install Karein (Agar pehle se nahi hai)
- [nodejs.org](https://nodejs.org/) par jayein aur **LTS version** download karke PC par install karein.

#### Step 2: Dependencies Install Karein
- Is `pc_server` folder ko apne PC par kisi jagah extract kar lein (jaise Desktop par).
- Us folder mein **Command Prompt (CMD)** ya **PowerShell** kholein aur yeh command run karein:
  ```cmd
  npm install
  ```

#### Step 3: Server Start Karein
- CMD mein yeh command chalayein:
  ```cmd
  npm start
  ```
- Server start ho jayega! Aap apne PC ke browser mein kholein: **`http://localhost:3000`**
- Yahan aapko ek **Graphical Web Dashboard** dikhega jisme saari sites, expenses aur staff ki live list dikhegi.

---

### 📱 Mobile App se Connect Kaise Karein?

1. Apne PC ka Local IP pata karein:
   - CMD mein likhein: `ipconfig`
   - **IPv4 Address** note karein (jaise `192.168.1.15`).
2. Mobile App kholein ➔ **Admin Login** karein.
3. Menu se **"PC Cloud Server Settings"** kholein.
4. Apna PC Server URL dalein:
   ```
   http://192.168.1.15:3000
   ```
5. **Save & Connect** par click karein! Ab mobile app direct aapke PC par data save aur sync karega.
