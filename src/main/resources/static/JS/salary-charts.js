function prepareChartData(dataList) {
  const grouped = {};

  dataList.forEach(entry => {
    const key = entry.composant;
    if (!grouped[key]) grouped[key] = {};
    grouped[key][entry.mois] = (grouped[key][entry.mois] || 0) + entry.montant;
  });

  const allMonths = [...new Set(dataList.map(d => d.mois))].sort();
  const datasets = Object.entries(grouped).map(([comp, moisMontants]) => ({
    label: comp,
    data: allMonths.map(m => moisMontants[m] || 0),
    fill: false,
    borderWidth: 2,
    tension: 0.3 // pour courbe lissée
  }));

  return { labels: allMonths, datasets };
}

function renderChart(canvasId, chartData, title) {
  new Chart(document.getElementById(canvasId), {
    type: 'line', // <-- Ici on change de 'bar' à 'line'
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

if (typeof earningsData !== 'undefined' && earningsData.length > 0) {
  const earningsChartPrepared = prepareChartData(earningsData);
  renderChart('earningsChart', earningsChartPrepared, 'Revenus mensuels par composant');
}

if (typeof deductionsData !== 'undefined' && deductionsData.length > 0) {
  const deductionsChartPrepared = prepareChartData(deductionsData);
  renderChart('deductionsChart', deductionsChartPrepared, 'Déductions mensuelles par composant');
}
