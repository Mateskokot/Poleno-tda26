// tlačítko Zpět (musí existovat v login.html jako <button id="backBtn">)
const backBtn = document.getElementById("backBtn");
if (backBtn) {
  backBtn.addEventListener("click", () => {
    location.href = "/index.html";
  });
}

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const err = document.getElementById("err");
  err.textContent = "";

  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;

  try {
    const res = await fetch("/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      throw new Error("Špatné přihlašovací údaje.");
    }

    const data = await res.json();
    localStorage.setItem("lecturerToken", data.token);

    // jednodušší správa: jeden rozcestník, kde si lektor vybere kurz a typ obsahu
    location.href = "/manageLec.html";
  } catch (ex) {
    err.textContent = ex.message;
  }
});

