import { createServer } from "node:http";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const routes = new Map([
  ["/", "demo/index.html"],
  ["/fake-login", "demo/fake-login.html"],
  ["/destination", "demo/destination.html"],
  ["/logo.svg", "dist/logo.svg"],
]);
const server = createServer(async (request, response) => {
  const route = routes.get(new URL(request.url, "http://localhost").pathname);
  if (!route || request.method !== "GET") {
    response.writeHead(404);
    response.end("Synthetic demo: no data collection endpoint.");
    return;
  }
  try {
    const content = await fs.readFile(path.join(root, route));
    response.writeHead(200, {
      "Content-Type": route.endsWith(".svg")
        ? "image/svg+xml"
        : "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    });
    response.end(content);
  } catch {
    response.writeHead(500);
    response.end("Run npm run build before starting the demo.");
  }
});
server.listen(8787, "127.0.0.1", () => {
  console.log("SafeX AI synthetic demo: http://127.0.0.1:8787");
  console.log(
    "Local-only fixtures. Stop with Ctrl+C. No submitted data is saved.",
  );
});
server.on("error", (error) => {
  console.error(error.message);
  process.exitCode = 1;
});
