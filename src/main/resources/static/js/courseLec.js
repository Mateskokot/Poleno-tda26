function token() {
  return localStorage.getItem("lecturerToken");
}
function requireLogin() {
  if (!token()) location.href = "/loginLec.html";
}
function getId() {
  return new URLSearchParams(location.search).get("id");
}

async function loadCourse(id) {
  const res = await fetch(`/api/courses/${encodeURIComponent(id)}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error("Nejde načíst detail kurzu.");
  return await res.json();
}

async function loadMaterials(courseId) {
  const res = await fetch(`/api/courses/${encodeURIComponent(courseId)}/materials`);
  if (!res.ok) return [];
  return await res.json();
}

function renderMaterials(items) {
  const el = document.getElementById("materials");
  if (!items.length) {
    el.innerHTML = `<p><small>Zatím žádné podklady.</small></p>`;
    return;
  }

  el.innerHTML = items.map(m => {
    const common = `
      <div style="border:1px solid #ccc; padding:10px; margin:10px 0;">
        <strong>${m.title}</strong><br>
        <small>${m.description}</small><br><br>
    `;

    if (m.type === "FILE") {
      return common + `
        <a href="/api/materials/${encodeURIComponent(m.id)}/download">Stáhnout</a>
        <button data-del="${m.id}" type="button">Smazat</button>
      </div>`;
    }

    return common + `
      <a href="${m.url}" target="_blank" rel="noopener">Otevřít odkaz</a>
      <button data-del="${m.id}" type="button">Smazat</button>
    </div>`;
  }).join("");

  document.querySelectorAll("[data-del]").forEach(btn => {
    btn.addEventListener("click", async () => {
      const id = btn.getAttribute("data-del");
      await deleteMaterial(id);
      const mats = await loadMaterials(course.id);
      renderMaterials(mats);
    });
  });
}

async function addLink(courseId, title, description, url) {
  const res = await fetch(`/api/courses/${encodeURIComponent(courseId)}/materials/link`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Auth": token()
    },
    body: JSON.stringify({ title, description, url })
  });
  if (!res.ok) throw new Error(await res.text());
}

async function addFile(courseId, title, description, file) {
  const fd = new FormData();
  fd.append("title", title);
  fd.append("description", description);
  fd.append("file", file);

  const res = await fetch(`/api/courses/${encodeURIComponent(courseId)}/materials/file`, {
    method: "POST",
    headers: { "X-Auth": token() },
    body: fd
  });
  if (!res.ok) throw new Error(await res.text());
}

async function deleteMaterial(materialId) {
  const res = await fetch(`/api/materials/${encodeURIComponent(materialId)}`, {
    method: "DELETE",
    headers: { "X-Auth": token() }
  });
  if (!res.ok) throw new Error(await res.text());
}

// ===== UI (modal) =====
const dialog = document.getElementById("materialDialog");
const addBtn = document.getElementById("addMaterialBtn");
const cancelBtn = document.getElementById("cancelMaterial");
const form = document.getElementById("materialForm");
const typeSel = document.getElementById("mType");
const linkFields = document.getElementById("linkFields");
const fileFields = document.getElementById("fileFields");
const mErr = document.getElementById("mError");

typeSel.addEventListener("change", () => {
  const isFile = typeSel.value === "FILE";
  linkFields.style.display = isFile ? "none" : "block";
  fileFields.style.display = isFile ? "block" : "none";
});

addBtn.addEventListener("click", () => {
  mErr.textContent = "";
  dialog.showModal();
});
cancelBtn.addEventListener("click", () => dialog.close());

let course = null;

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  mErr.textContent = "";

  const title = document.getElementById("mTitle").value.trim();
  const description = document.getElementById("mDesc").value.trim();

  try {
    if (typeSel.value === "LINK") {
      const url = document.getElementById("mUrl").value.trim();
      if (!url) throw new Error("Chybí URL");
      await addLink(course.id, title, description, url);
    } else {
      const file = document.getElementById("mFile").files[0];
      if (!file) throw new Error("Vyber soubor");
      await addFile(course.id, title, description, file);
    }

    dialog.close();
    const mats = await loadMaterials(course.id);
    renderMaterials(mats);
  } catch (ex) {
    mErr.textContent = "Chyba: " + ex.message;
  }
});

// ===== start =====
(async () => {
  requireLogin();

  const id = getId();
  if (!id) throw new Error("Chybí id kurzu v URL.");

  course = await loadCourse(id);
  if (!course) throw new Error("Kurz nenalezen.");

  document.getElementById("title").textContent = course.title;
  document.getElementById("desc").textContent = course.description;
  document.getElementById("lecturer").textContent = `Lektor: ${course.lecturer}`;

  const mats = await loadMaterials(course.id);
  renderMaterials(mats);
})().catch(ex => {
  document.getElementById("err").textContent = ex.message;
});
