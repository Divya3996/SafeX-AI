export function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className = "",
  text?: string,
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
export const ICONS: Record<string, string> = {
  shield: "M12 3 20 6v6c0 5-4 8-8 10-4-2-8-5-8-10V6Z",
  scan: "M4 8V4h4M16 4h4v4M20 16v4h-4M8 20H4v-4M7 12h10",
  link: "M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-2 2M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l2-2",
  image: "M4 4h16v16H4ZM4 16l5-5 4 4 3-3 4 4M9 8h.01",
  history: "M3 12a9 9 0 1 0 3-7M3 3v6h6M12 7v5l3 2",
  settings:
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4",
  check: "m5 12 4 4L19 6",
  alert: "M12 8v5M12 17h.01M12 3 2 21h20Z",
  lock: "M7 10V7a5 5 0 0 1 10 0v3M5 10h14v11H5ZM12 14v3",
  arrow: "M5 12h14m-5-5 5 5-5 5",
  close: "m6 6 12 12M6 18 18 6",
  file: "M6 3h8l4 4v14H6ZM14 3v5h4M9 12h6M9 16h6",
  sound: "M4 9h4l5-4v14l-5-4H4ZM17 8a6 6 0 0 1 0 8",
  globe:
    "M3 12h18M12 3a16 16 0 0 0 0 18 16 16 0 0 0 0-18M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0",
};
export function icon(name: string) {
  const s = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  s.setAttribute("viewBox", "0 0 24 24");
  s.setAttribute("fill", "none");
  s.setAttribute("stroke", "currentColor");
  s.setAttribute("stroke-width", "1.7");
  s.setAttribute("stroke-linecap", "round");
  s.setAttribute("stroke-linejoin", "round");
  s.setAttribute("aria-hidden", "true");
  const p = document.createElementNS("http://www.w3.org/2000/svg", "path");
  p.setAttribute("d", ICONS[name] ?? ICONS.shield);
  s.append(p);
  return s;
}
export function button(
  label: string,
  handler: () => void | Promise<void>,
  className = "button",
  name?: string,
) {
  const b = el("button", className);
  b.type = "button";
  if (name) b.append(icon(name));
  b.append(el("span", "", label));
  b.addEventListener("click", () => void handler());
  return b;
}
export function card(title: string, subtitle?: string) {
  const c = el("section", "card");
  c.append(el("h2", "", title));
  if (subtitle) c.append(el("p", "muted", subtitle));
  return c;
}
