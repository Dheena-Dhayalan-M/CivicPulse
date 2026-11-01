# CivicPulse + MongoDB Atlas

1. Keep `mongo-java-driver-3.12.14.jar` inside the `lib` folder.
2. In the same PowerShell window used to start CivicPulse, set the Atlas URI:

   ```powershell
   $env:CIVICPULSE_MONGODB_URI = Read-Host "Paste your MongoDB Atlas URI"
   ```

3. Paste your connection string in this format, replacing `<YOUR_PASSWORD>` privately with the database user's actual password:

   `mongodb+srv://dmdheenadhayalan_db_user:<YOUR_PASSWORD>@civicpulsedb.otlvpl2.mongodb.net/CivicPulse?retryWrites=true&w=majority&appName=CivicPulseDB`

4. Run `run.bat` from that same PowerShell window.
5. Open `http://localhost:8082`.

Database: `CivicPulse`. Collections: `accounts` and `complaints`. Complaint history is stored as an array inside each complaint document.

On first run, if an Atlas collection is empty, existing local `data/accounts.db` or `data/complaints.db` records are loaded and saved to Atlas. Local `.db` files are also maintained as backups. If Atlas cannot connect, CivicPulse falls back to local file storage and prints the reason in the console.

Do not share your database password. If it contains URI-reserved characters, URL-encode them in the connection string.
