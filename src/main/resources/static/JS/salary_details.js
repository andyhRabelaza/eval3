document.addEventListener('DOMContentLoaded', function () {
    const detailLinks = document.querySelectorAll('.view-details-links');

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

            let rawMonthYear = monthCell.textContent.trim(); // ex: "Janvier 2024"
            console.log("rawMonthYear =", rawMonthYear);

            const parts = rawMonthYear.split(' ');
            if (parts.length !== 2) {
                console.warn("Format inattendu :", rawMonthYear);
                return;
            }

            const monthName = parts[0].toLowerCase(); // "janvier"
            const year = parts[1]; // "2024"

            const monthNum = moisMap[monthName];
            if (!monthNum) {
                console.warn("Mois non reconnu :", monthName);
                return;
            }

            const finalDate = `${year}-${monthNum}`; // ex: "2024-01"
            console.log("Date cliquée :", finalDate);

            window.location.href = `/salary-employe?monthYear=${finalDate}`;
        });
    });
});


document.addEventListener('DOMContentLoaded', function () {
    // Graphique ligne : évolution brut et net
    new Chart(document.getElementById('salaryNetChart'), {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Salaire Brut',
                    data: brutData,
                    borderColor: 'rgb(255, 99, 132)',
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    tension: 0.3,
                    fill: false
                },
                {
                    label: 'Salaire Net',
                    data: netData,
                    borderColor: 'rgb(54, 162, 235)',
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    tension: 0.3,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                title: {
                    display: true,
                    text: 'Évolution des Salaires Brut et Net par Mois'
                }
            }
        }
    });

    // Graphique barres : earning vs deduction
    new Chart(document.getElementById('salaryComponentsChart'), {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Earnings',
                    data: earningData,
                    backgroundColor: 'rgb(75, 192, 192)'
                },
                {
                    label: 'Deductions',
                    data: deductionData,
                    backgroundColor: 'rgb(255, 206, 86)'
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                title: {
                    display: true,
                    text: 'Éléments de Salaire par Mois'
                }
            }
        }
    });
});