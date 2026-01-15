// Offline login – ukládá "session" do localStorage.
// Později to nahradíte voláním backendu (POST /api/login).

function setLoggedIn(value) {
    localStorage.setItem("tda_logged_in", value ? "1" : "0");
}

function isLoggedIn() {
    return localStorage.getItem("tda_logged_in") === "1";
}

// Pokud je to login stránka, připojíme logiku formuláře:
const form = document.getElementById("loginForm");

if (form) {
    form.addEventListener("submit", (e) => {
        e.preventDefault();

        const u = document.getElementById("username").value.trim();
        const p = document.getElementById("password").value;

        const msg = document.getElementById("msg");
        msg.textContent = "";

        if (u === "lecturer" && p === "TdA26!") {
            setLoggedIn(true);
            window.location.href = "dashboard.html";
        } else {
            setLoggedIn(false);
            msg.textContent = "Špatné přihlašovací údaje.";
        }
    });
}

// Export pro jiné skripty (dashboard):
window.TDA_AUTH = { isLoggedIn, setLoggedIn };
