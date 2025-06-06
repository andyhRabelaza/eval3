document.addEventListener("DOMContentLoaded", function () {
    const viewDetailLinks = document.querySelectorAll(".view-details-link");
    const modal = document.getElementById("detailsModal");
    const modalSlipName = document.getElementById("modalSlipName");
    const modalGrossPay = document.getElementById("modalGrossPay");
    const modalNetPay = document.getElementById("modalNetPay");
    const modalEarningsList = document.getElementById("modalEarningsList");
    const modalDeductionsList = document.getElementById("modalDeductionsList");
    const closeModal = document.getElementById("closeModal");

    viewDetailLinks.forEach(link => {
        link.addEventListener("click", function (event) {
            event.preventDefault();
            const idRef = this.getAttribute("data-name");

            fetch(`/salary-slip?idRef=${encodeURIComponent(idRef)}`)
                .then(response => {
                    if (!response.ok) throw new Error("Erreur réseau");
                    return response.json();
                })
                .then(data => {
                    modalSlipName.textContent = data.employee_name || "N/A";
                    modalGrossPay.textContent = data.gross_pay ? `${data.gross_pay} ` : "N/A";
                    modalNetPay.textContent = data.net_pay ? `${data.net_pay} ` : "N/A";

                    modalEarningsList.innerHTML = "";
                    modalDeductionsList.innerHTML = "";

                    if (Array.isArray(data.earnings) && data.earnings.length > 0) {
                        data.earnings.forEach(item => {
                            const li = document.createElement("li");
                            li.textContent = `${item.salary_component}: ${item.amount} `;
                            modalEarningsList.appendChild(li);
                        });
                    } else {
                        modalEarningsList.innerHTML = "<li>Aucun élément trouvé</li>";
                    }

                    if (Array.isArray(data.deductions) && data.deductions.length > 0) {
                        data.deductions.forEach(item => {
                            const li = document.createElement("li");
                            li.textContent = `${item.salary_component}: ${item.amount} `;
                            modalDeductionsList.appendChild(li);
                        });
                    } else {
                        modalDeductionsList.innerHTML = "<li>Aucune déduction trouvée</li>";
                    }

                    modal.style.display = "block";
                })
                .catch(error => {
                    alert("Erreur lors du chargement des détails: " + error.message);
                });
        });
    });

    closeModal.addEventListener("click", function () {
        modal.style.display = "none";
    });

    window.addEventListener("click", function (event) {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
});
