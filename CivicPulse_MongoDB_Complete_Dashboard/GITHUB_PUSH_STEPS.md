# Push CivicPulse to GitHub

## Before pushing
- Never commit `config.properties`, `.env`, MongoDB passwords, or local database files.
- `config.properties` and `data/*.db` are excluded by `.gitignore`.
- The app reads the MongoDB URI from `CIVICPULSE_MONGODB_URI` (or `MONGO_URI`) environment variable. The example below uses PowerShell.
- Rotate any MongoDB password that has already been shared or exposed.

## Push using Git (Windows)

1. Create a new **empty** repository on https://github.com/new. Do not add a README, `.gitignore`, or license there if you are pushing this existing project.
2. Extract this ZIP.
3. Open the extracted `CivicPulse_MongoDB_Complete_Dashboard` folder in VS Code.
4. Open **Terminal → New Terminal**.
5. Run these commands one at a time, replacing `YOUR_GITHUB_USERNAME` and `YOUR_REPOSITORY_NAME`:

```powershell
git init
git add .
git status
git commit -m "Initial CivicPulse project upload"
git branch -M main
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME.git
git push -u origin main
```

6. Refresh your GitHub repository page. Your project files should appear.

## Configure MongoDB locally after cloning

In PowerShell, from the project folder:

```powershell
$env:CIVICPULSE_MONGODB_URI = Read-Host "Paste your MongoDB Atlas URI"
.un.bat
```

Then open http://localhost:8082.

The environment variable is only set in that PowerShell session. It is not saved in the repository.

## If Git says `remote origin already exists`

Use:

```powershell
git remote set-url origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME.git
git push -u origin main
```

## If Git is not recognized

Install Git for Windows from https://git-scm.com/download/win, restart VS Code, and repeat the commands.

## Important
If a secret was committed, deleting the file in a later commit does not erase it from Git history. Rotate the MongoDB password immediately. For a first push, use this GitHub-ready ZIP so the local config and database files are not included.
