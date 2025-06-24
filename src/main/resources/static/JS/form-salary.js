document.addEventListener("DOMContentLoaded", function () {

    const employeeSelect = document.getElementById("ref");
    const companySelect = document.getElementById("company");
    const salaryStructureSelect = document.getElementById("salaryStructure");

    employeeSelect.addEventListener("change", function () {
        const employeeId = this.value;

        // Réinitialise les champs
        companySelect.value = "";
        salaryStructureSelect.value = "";

        if (employeeId) {
            fetch(`/employee/info/${employeeId}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error("Erreur lors de la récupération des informations.");
                    }
                    return response.json();
                })
                .then(data => {
                    if (data) {
                        if (data.company) {
                            companySelect.value = data.company;
                        }
                        if (data.salary_structure) {
                            salaryStructureSelect.value = data.salary_structure;
                        }
                    } else {
                        console.warn("Aucune information trouvée pour cet employé.");
                    }
                })
                .catch(error => {
                    console.error("❌ Erreur :", error);
                });
        }
    });

});
