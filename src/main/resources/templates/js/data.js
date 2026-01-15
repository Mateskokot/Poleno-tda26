// Fake data pro školní/offline verzi.
// Později to nahradíte fetch() z backendu.

window.TDA = window.TDA || {};

window.TDA.courses = [
    {
        id: "11111111-1111-1111-1111-111111111111",
        title: "Základy HTML",
        description: "Základní tagy, struktura stránky, odkazy, formuláře.",
        lecturer: "Lektor A",
        materials: [
            { id: "m1", type: "file", title: "Prezentace - úvod (PDF)", createdAt: "2026-01-01" },
            { id: "m2", type: "link", title: "Video: HTML základy", url: "https://example.com", createdAt: "2026-01-03" }
        ]
    },
    {
        id: "22222222-2222-2222-2222-222222222222",
        title: "Úvod do JavaScriptu",
        description: "Proměnné, podmínky, funkce, práce s DOM.",
        lecturer: "Lektor B",
        materials: [
            { id: "m3", type: "link", title: "Dokumentace JS", url: "https://example.com", createdAt: "2026-01-02" }
        ]
    }
];
