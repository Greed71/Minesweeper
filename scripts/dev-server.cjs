/**
 * Avvio Maven wrapper da /server, compatibile con Windows (mvnw.cmd) e Unix.
 */
const { spawn } = require("child_process");
const path = require("path");

const serverDir = path.resolve(__dirname, "..", "server");
const mvnw =
  process.platform === "win32"
    ? path.join(serverDir, "mvnw.cmd")
    : path.join(serverDir, "mvnw");

const child = spawn(mvnw, ["spring-boot:run"], {
  cwd: serverDir,
  stdio: "inherit",
  shell: process.platform === "win32",
  env: process.env,
});

child.on("exit", (code) => {
  process.exit(typeof code === "number" ? code : 0);
});
child.on("error", (err) => {
  console.error("Impossibile avviare il server (Maven):", err.message);
  process.exit(1);
});
