document.addEventListener('DOMContentLoaded', () => {
  // Sélectionne toutes les lignes du tbody
  const rows = document.querySelectorAll('tbody tr');

  rows.forEach(row => {
    row.addEventListener('click', () => {
      // Récupère le contenu textuel de la 1ère cellule <td> (Name)
      const name = row.querySelector('td').textContent.trim();
      console.log("Nom du salaire cliqué :", name);
    });
  });
});