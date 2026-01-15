function token() {
  return localStorage.getItem("lecturerToken");
}

function requireLogin() {
  // když není token, jdi na login
  if (!token()) location.href = "loginLec.html";
}

document.getElementById("logoutBtn")?.addEventListener("click", () => {
  localStorage.removeItem("lecturerToken");
  location.href = "index.html";
});

async function loadCourses() {
  const res = await fetch("api/courses");
  if (!res.ok) throw new Error("Nejde načíst kurzy.");
  return await res.json();
}

function renderCourses(items) {
  const el = document.getElementById("courses");
  el.innerHTML = items.map(c => `
    <div class="card">
      <strong>${c.title ?? ""}</strong><br>
      <span>${c.description ?? ""}</span><br>
      <small class="small">Lektor: ${c.lecturer ?? ""}</small><br><br>
      <a href="courseLec.html?id=${encodeURIComponent(c.id)}">Spravovat podklady</a>
    </div>
  `).join("");
}

(async () => {
  requireLogin();
  const items = await loadCourses();
  renderCourses(items);
})().catch(ex => {
  const err = document.getElementById("err");
  if (err) err.textContent = ex.message;
});
