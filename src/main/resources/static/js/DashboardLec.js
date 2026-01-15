function token() {
  return localStorage.getItem("lecturerToken");
}

function requireLogin() {
  if (!token()) location.href = "/loginLec.html";
}

document.getElementById("logoutBtn").addEventListener("click", () => {
  localStorage.removeItem("lecturerToken");
  location.href = "/loginLec.html";
});

async function loadCourses() {
  const res = await fetch("/api/courses");
  if (!res.ok) throw new Error("Nejde načíst kurzy.");
  return await res.json();
}

function renderCourses(items) {
  const el = document.getElementById("courses");
  el.innerHTML = items.map(c => `
    <div style="border:1px solid #ccc; padding:10px; margin:10px 0;">
      <strong>${c.title}</strong><br>
      <span>${c.description}</span><br>
      <small>Lektor: ${c.lecturer}</small><br><br>
      <a href="/courseLec.html?id=${encodeURIComponent(c.id)}">Spravovat podklady</a>
    </div>
  `).join("");
}

(async () => {
  requireLogin();
  const items = await loadCourses();
  renderCourses(items);
})().catch(ex => {
  document.getElementById("err").textContent = ex.message;
});
