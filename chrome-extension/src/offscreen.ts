chrome.runtime.onMessage.addListener((m, sender, respond) => {
  if (
    m.type !== "PLAY_SOUND" ||
    sender.id !== chrome.runtime.id ||
    sender.url?.split(/[?#]/)[0] !== chrome.runtime.getURL("background.js")
  )
    return;
  const audio = new Audio(chrome.runtime.getURL("warning.wav"));
  audio.volume = 0.55;
  void audio.play().then(
    () => respond({ ok: true }),
    () => respond({ ok: false }),
  );
  return true;
});
