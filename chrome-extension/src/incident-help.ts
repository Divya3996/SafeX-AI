import { el, button, card } from "./ui";
const INCIDENTS = [
  [
    "I opened a link",
    [
      "Close the page. Do not enter information, download files or grant permissions.",
      "If you only opened the page, that alone does not prove your device is compromised. Check for unexpected downloads and permission changes.",
      "If you shared information, installed an app or paid, choose that situation below for additional steps.",
    ],
  ],
  [
    "I shared a password",
    [
      "From a device you trust, open the service's official app or type its website address yourself. Change the exposed password.",
      "Change that password anywhere else you reused it. Review active sessions and sign out unknown devices.",
      "Enable multi-factor authentication and check recovery email, phone number and recent account activity.",
    ],
  ],
  [
    "I shared an OTP or PIN",
    [
      "Contact the affected bank or service immediately through its official app or a number you already trust.",
      "Ask about securing the account and stopping unauthorized transactions. Change an exposed PIN through the official service.",
      "Check recent transactions. If money was lost, use the payment incident steps and report promptly.",
    ],
  ],
  [
    "I installed an app",
    [
      "Stop using the suspicious app. If someone is remotely controlling your device, disconnect it from the internet.",
      "Use a different trusted device to contact your bank and secure affected accounts.",
      "Remove remote-access permissions and uninstall the suspicious application. Use the security tools supplied with your operating system.",
      "Preserve the app name and relevant evidence. SafeX AI cannot confirm or remove a device infection.",
    ],
  ],
  [
    "I sent money",
    [
      "Contact your bank or payment provider immediately using official contact details. Report the transaction and ask what actions are available.",
      "In India, call 1930 for financial cyber fraud and submit a complaint at cybercrime.gov.in.",
      "Keep the transaction reference, date, amount and relevant messages. Never include passwords, PINs or OTPs in shared evidence.",
      "Do not pay anyone who promises to recover your money. Reporting does not guarantee recovery.",
    ],
  ],
] as const;
let incident = 0,
  india = true;
const checked = new Set<string>();
export function renderIncidentHelp(
  main: HTMLElement,
  g: (value: string) => string,
) {
  const header = card(
    g("Take the next safe step"),
    g(
      "Choose what happened. These checklists work offline; calls and official websites open only when you choose them.",
    ),
  );
  const choice = el("select");
  choice.id = "incident-type";
  choice.setAttribute("aria-label", g("Choose what happened"));
  INCIDENTS.forEach((r, i) => {
    const o = el("option", "", g(r[0]));
    o.value = String(i);
    o.selected = i === incident;
    choice.append(o);
  });
  choice.onchange = () => {
    incident = Number(choice.value);
    main.replaceChildren();
    renderIncidentHelp(main, g);
  };
  const situation = el("div");
  situation.dataset.guide = "situation";
  situation.append(choice);
  header.append(situation);
  const steps = el("div", "incident-checklist");
  INCIDENTS[incident][1].forEach((text, i) => {
    const label = el("label", "incident-step"),
      check = el("input");
    check.type = "checkbox";
    check.checked = checked.has(`${incident}:${i}`);
    check.onchange = () => {
      if (check.checked) checked.add(`${incident}:${i}`);
      else checked.delete(`${incident}:${i}`);
    };
    label.append(check, el("span", "", g(text)));
    steps.append(label);
  });
  header.append(steps);
  main.append(header);
  const help = card(
    g(india ? "Official help in India" : "Use your local official services"),
  );
  help.dataset.guide = "contact";
  const region = el("select");
  region.id = "incident-country";
  region.setAttribute("aria-label", g("Where do you need help?"));
  for (const [value, title] of [
    ["IN", "India"],
    ["OTHER", "Another country"],
  ]) {
    const o = el("option", "", g(title));
    o.value = value;
    o.selected = india ? value === "IN" : value === "OTHER";
    region.append(o);
  }
  region.onchange = () => {
    india = region.value === "IN";
    main.replaceChildren();
    renderIncidentHelp(main, g);
  };
  help.append(region);
  if (india) {
    for (const [number, title] of [
      ["1930", "Financial cyber fraud • 1930"],
      ["112", "Immediate danger • 112"],
    ]) {
      const c = el("section", "official-contact");
      c.append(el("h3", "", g(title)));
      const actions = el("div", "actions");
      const copy = button(
        g("Copy number") + " · " + number,
        async () => {
          try {
            await navigator.clipboard.writeText(number);
            copy.querySelector("span")!.textContent = g(
              "Number copied. Dial it on your phone.",
            );
          } catch {
            copy.querySelector("span")!.textContent = g(
              "Copy unavailable. Dial the shown number on your phone.",
            );
          }
        },
        "button primary compact",
      );
      copy.dataset.phone = number;
      const dial = button(
        g("Open phone dialer") + " · " + number,
        () => {
          const d = el("dialog", "guide-intro");
          d.setAttribute("aria-label", g(title));
          const content = el("section", "guide-card");
          content.append(
            el("h2", "", g(title)),
            el(
              "p",
              "",
              g(
                "Review the number and make the call yourself. If no calling app opens, dial this number on your phone. These numbers are for India.",
              ),
            ),
          );
          const a = el("a", "button primary", g("Open calling app"));
          a.href = "tel:" + number;
          a.rel = "noreferrer";
          a.onclick = () => d.close();
          const close = button(g("Cancel"), () => d.close(), "button ghost");
          content.append(a, close);
          d.append(content);
          d.addEventListener("close", () => d.remove());
          document.body.append(d);
          d.showModal();
        },
        "button compact",
      );
      dial.dataset.dial = number;
      actions.append(copy, dial);
      c.append(actions);
      help.append(c);
    }
    const report = el("a", "button full", g("Open official reporting website"));
    report.href = "https://cybercrime.gov.in/";
    report.target = "_blank";
    report.rel = "noopener noreferrer";
    help.append(report);
    const sources = el(
      "p",
      "muted small",
      g(
        "Verified 10 October 2026: cybercrime.gov.in and 112.gov.in. These actions are not a completed complaint and do not guarantee recovery.",
      ),
    );
    help.append(sources);
  } else
    help.append(
      el(
        "p",
        "",
        g(
          "Contact your bank through its official app or a number printed on your card. For immediate danger, use your country's emergency service. India's 1930 and 112 actions are hidden for this selection.",
        ),
      ),
    );
  help.append(
    el(
      "p",
      "muted small",
      g(
        "SafeX AI does not submit reports or call anyone automatically. Website access needs an internet connection in your browser.",
      ),
    ),
  );
  main.append(help);
}
