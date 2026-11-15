const form = document.querySelector("#upload-form");
const input = document.querySelector("#midi-file");
const fileLabel = document.querySelector("#file-label");
const notice = document.querySelector("#notice");
const filename = document.querySelector("#filename");
const statusDot = document.querySelector("#status-dot");
const playButton = form.querySelector('button[type="submit"]');
const stopButton = document.querySelector("#stop-button");
const csrfToken = document.body.dataset.csrfToken;
const vncToken = document.body.dataset.vncToken;

const displayUrl = new URL(window.location.href);
if (displayUrl.hostname.endsWith(".app.github.dev")) {
  displayUrl.hostname = displayUrl.hostname.replace(/-5000(?=\.app\.github\.dev$)/, "-6080");
  displayUrl.protocol = "https:";
  displayUrl.port = "";
} else {
  displayUrl.port = "6080";
}
displayUrl.pathname = "/vnc.html";
const socketQuery = new URLSearchParams({ token: vncToken });
displayUrl.search = new URLSearchParams({
  autoconnect: "true",
  resize: "scale",
  path: `websockify?${socketQuery}`,
});
document.querySelector("#display").src = displayUrl.toString();

input.addEventListener("change", () => {
  fileLabel.textContent = input.files[0]?.name ?? "Choose MIDI";
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!input.files.length) return;

  playButton.disabled = true;
  notice.textContent = "Uploading MIDI and starting the performance…";
  try {
    const response = await fetch("/upload", {
      method: "POST",
      headers: { "X-CSRF-Token": csrfToken },
      body: new FormData(form),
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error ?? "Could not start playback.");
    notice.textContent = result.message;
    await refreshStatus();
  } catch (error) {
    notice.textContent = error.message;
  } finally {
    playButton.disabled = false;
  }
});

stopButton.addEventListener("click", async () => {
  try {
    const response = await fetch("/stop", {
      method: "POST",
      headers: { "X-CSRF-Token": csrfToken },
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error ?? "Could not stop playback.");
    notice.textContent = result.message;
    await refreshStatus();
  } catch (error) {
    notice.textContent = error.message;
  }
});

async function refreshStatus() {
  try {
    const response = await fetch("/api/status", { cache: "no-store" });
    if (!response.ok) throw new Error("Player status is unavailable.");
    const state = await response.json();
    statusDot.classList.toggle("is-playing", state.running);
    filename.textContent = state.filename ?? "Waiting for a MIDI";
    if (state.exitCode !== null && state.exitCode !== undefined && state.exitCode !== 0) {
      notice.textContent = `The player exited with code ${state.exitCode}. Check the server log for details.`;
    }
  } catch (error) {
    notice.textContent = error.message;
  }
}

refreshStatus();
window.setInterval(refreshStatus, 3000);
