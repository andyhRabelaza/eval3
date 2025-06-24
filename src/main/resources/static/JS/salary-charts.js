function prepareChartData(dataList) {
  const grouped = {};

  // Grouper les montants par composant et par mois
  dataList.forEach(entry => {
    const key = entry.composant;
    if (!grouped[key]) grouped[key] = {};
    grouped[key][entry.mois] = (grouped[key][entry.mois] || 0) + entry.montant;
  });

  // Extraire tous les mois uniques et les trier
  const allMonths = [...new Set(dataList.map(d => d.mois))].sort();

  // Définir des couleurs pour chaque série
  const colors = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40', '#8E44AD', '#2ECC71'];

  // Construire les datasets
  const datasets = Object.entries(grouped).map(([comp, moisMontants], index) => {
    const color = colors[index % colors.length];
    return {
      label: comp,
      data: allMonths.map(m => moisMontants[m] || 0),
      fill: false,
      borderWidth: 2,
      tension: 0.3,
      borderColor: color,
      backgroundColor: color
    };
  });

  return {
    labels: allMonths,
    datasets: datasets
  };
}


function renderChart(canvasId, chartData, title) {
  const ctx = document.getElementById(canvasId);
  if (!ctx) {
    console.error(`Canvas with id "${canvasId}" not found.`);
    return;
  }

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: chartData.labels,
      datasets: chartData.datasets
    },
    options: {
      responsive: true,
      plugins: {
        title: {
          display: true,
          text: title
        },
        legend: {
          display: true
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
}


// Génération des graphiques si les données existent
if (typeof earningsData !== 'undefined' && earningsData.length > 0) {
  const earningsChartPrepared = prepareChartData(earningsData);
  renderChart('earningsChart', earningsChartPrepared, 'Revenus mensuels par composant');
}

if (typeof deductionsData !== 'undefined' && deductionsData.length > 0) {
  const deductionsChartPrepared = prepareChartData(deductionsData);
  renderChart('deductionsChart', deductionsChartPrepared, 'Déductions mensuelles par composant');
}
