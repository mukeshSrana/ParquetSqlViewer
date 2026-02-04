Here is the **complete, self-contained `README.md`** with **clear, step-by-step instructions**, ready to copy-paste and use as your final README file:

````markdown
# Parquet SQL Viewer

An IntelliJ IDEA plugin for querying local Parquet files using SQL, powered by DuckDB.

---

## 🚀 Features
- **Direct Parquet Access:** Query Parquet files without conversion.
- **DuckDB Engine:** Fast, in-memory SQL processing.
- **IDE Integration:** Built-in tool window for a seamless developer workflow.

---

## 🛠️ Build & Installation

### Prerequisites
Make sure you have the following installed:

- **JDK 21**
- **Gradle 8.12+**
- **IntelliJ IDEA** (for installing and testing the plugin)

---

## 🔧 Compile & Build (Initial Build)

To compile the plugin and create an installable ZIP file, run the following command from the project root:

```bash
cd ParquetSqlViewer
./gradlew buildPlugin
````

This will:

* Compile the Kotlin source code
* Process `plugin.xml`
* Create a plugin ZIP file under the `build/` directory

---

## 🔁 How to Re-Compile and Build (Recommended)

When you make code changes, it is strongly recommended to perform a **clean build** to avoid stale code or cached artifacts.

Run this command in your terminal (WSL or local):

```bash
./gradlew clean buildPlugin
```

### What each task does

* **`clean`**
  Deletes the entire `build/` directory, including old compiled classes and previously generated ZIP files.

* **`buildPlugin`**

    * Re-compiles all Kotlin classes
    * Processes `plugin.xml`
    * Bundles all required dependencies
    * Packages the DuckDB JDBC driver into the plugin ZIP

### Build Output Location

After a successful build, the plugin ZIP will be available at:

```
build/distributions/ParquetSqlViewer-1.1.0.zip
```

---

## 📦 How to Install the Updated Plugin ZIP

IntelliJ IDEA does **not automatically reload** rebuilt plugin ZIP files.
You must manually install the updated ZIP each time you rebuild.

### Installation Steps

1. Open your **main IntelliJ IDEA** instance.
2. Go to **Settings** (`Ctrl+Alt+S`) → **Plugins**.
3. Click the **Cog icon (⚙️)** in the top-right corner.
4. Select **Install Plugin from Disk…**.
5. Navigate to:

   ```
   build/distributions/
   ```
6. Select `ParquetSqlViewer-1.1.0.zip`.
7. Restart IntelliJ IDEA when prompted to apply the changes.

---

## ✅ How to Verify the Build (Important)

Before installing the plugin, you can verify that the DuckDB JDBC driver is correctly packaged inside the ZIP file.
This confirms that your dependency configuration worked and helps prevent runtime errors such as **“No suitable driver”**.

Run the following command:

```bash
unzip -l build/distributions/*.zip
```

### Verify the Output

Look for the following file in the listing:

```
lib/duckdb_jdbc-1.1.3.jar
```

If this JAR is present, the DuckDB driver is bundled correctly and the plugin should work after installation.

---

## 🤝 Contributing
For Statnett internal developers:

Clone the repo.

Run ./gradlew runIde to test changes in a sandbox.

---

## 🔄 Summary: Typical Development Workflow

Every time you change the plugin code:

1. **Build the plugin**

   ```bash
   ./gradlew buildPlugin
   ```

   *(Use `clean buildPlugin` if you want a guaranteed fresh build)*

2. **Install the updated ZIP**

    * IntelliJ IDEA → Settings → Plugins
    * Install Plugin from Disk
    * Select the newly built ZIP

3. **Restart IntelliJ IDEA**

---

Happy querying Parquet files with SQL 🚀

