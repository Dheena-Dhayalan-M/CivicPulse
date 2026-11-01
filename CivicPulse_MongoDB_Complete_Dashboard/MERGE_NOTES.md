# Merge notes

- Preserved the MongoDB edition's `MongoStore.java`, database name `CivicPulse`, collection names `accounts` and `complaints`, MongoDB document fields, startup migration/fallback logic, and local data files.
- Retained the complete dashboard frontend and role workflow from `CivicPulse_Complete_Dashboard_All_Options_Working.zip`.
- The dashboard frontend files in both supplied ZIPs were byte-for-byte identical; they are included from the working-dashboard ZIP in this merged package.
- Kept the MongoDB-enabled Java backend rather than replacing it with the file-only backend.
- Improved `compile.bat` so it downloads the required MongoDB Java driver automatically when absent.
