function showPopup() {
    var hp = document.getElementById("claimHoverPopup");
    hp.style.display = "block";
}

function hidePopup() {
    var hp = document.getElementById("claimHoverPopup");
    hp.style.display = "none";
}

function display(error) {
    var reasonText = document.getElementById("errordesc");
    claimBuildAction.getReason(error, function(content) {
        reasonText.textContent = content.responseObject();
    });
}

Behaviour.specify("A#claim", "AbstractClaimBuildAction_claim", 0, function (element) {
    element.addEventListener("click", (event) => {
        event.preventDefault();
        showPopup();
    });
});

Behaviour.specify("A#reassign", "AbstractClaimBuildAction_reassign", 0, function (element) {
    element.addEventListener("click", (event) => {
        event.preventDefault();
        showPopup();
    });
});

Behaviour.specify(".claim-hide-popup", "AbstractClaimBuildAction_hide-popup", 0, function (element) {
    element.addEventListener("click", (event) => {
        hidePopup();
    });
});

Behaviour.specify(".claim-bfa-display-error", "AbstractClaimBuildAction_display-bfa-error", 0, function (element) {
    element.addEventListener("change", (event) => {
        display(event.target.value);
    });
});
