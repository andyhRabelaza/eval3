document.addEventListener('DOMContentLoaded', function () {
    const detailLinks = document.querySelectorAll('.view-details-link');

    const moisMap = {
        "janvier": "01",
        "février": "02",
        "mars": "03",
        "avril": "04",
        "mai": "05",
        "juin": "06",
        "juillet": "07",
        "août": "08",
        "septembre": "09",
        "octobre": "10",
        "novembre": "11",
        "décembre": "12"
    };

    detailLinks.forEach(link => {
        link.addEventListener('click', function (event) {
            event.preventDefault();

            const tr = this.closest('tr');
            if (!tr) return;

            const monthCell = tr.querySelector('.month-cell');
            if (!monthCell) return;

            let rawMonth = monthCell.textContent.trim();
            console.log("rawMonth =", rawMonth);

            const yearSelect = document.querySelector('select[name="year"]');
            let selectedYear = yearSelect ? yearSelect.value : null;

            if (!selectedYear) {
                console.log("Aucune année sélectionnée");
                return;
            }

            let yearPart, monthPart;

            if (rawMonth.includes('-')) {
                // Format "2025-juin" ou "2025-01"
                let parts = rawMonth.split('-');
                yearPart = parts[0];
                monthPart = parts[1];
            } else {
                // Format "juin" seulement
                yearPart = selectedYear; // on prend l'année sélectionnée
                monthPart = rawMonth;
            }

            // Convertir mois texte en numéro, sinon garder tel quel (ex: "01")
            let monthNum = moisMap[monthPart.toLowerCase()] || monthPart;

            const finalDate = `${yearPart}-${monthNum}`;
            console.log("Date cliquée :", finalDate);

            // Redirection vers le controller avec paramètre
            window.location.href = `/salary-employe?monthYear=${finalDate}`;
        });
    });
});
