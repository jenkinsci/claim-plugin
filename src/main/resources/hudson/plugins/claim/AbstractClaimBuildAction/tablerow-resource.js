function showPopup(hoveritem) {
    var row = hoveritem.closest('tr');
    var hp = row.querySelector(".claimHoverPopup");
    if (hp) {
        hp.style.display = "block";
    } else {
        console.error("Claim popup not found in the same row.");
    }
}

function hidePopup(hoveritem) {
    var row = hoveritem.closest('tr');
    var hp = row.querySelector(".claimHoverPopup");
    if (hp) {
        hp.style.display = "none";
    } else {
        console.error("Claim popup not found in the same row.");
    }
}

function display(hoveritem, error) {
    var row = hoveritem.closest('tr');
    var reasonText = row.querySelector('#errordesc');
    claimBuildAction.getReason(error, function(content) {
        reasonText.textContent = content.responseObject();
    });
}

Behaviour.specify("A#claim", "AbstractClaimBuildAction_claim", 0, function (element) {
    element.addEventListener("click", (event) => {
        event.preventDefault();
        showPopup(event.target.closest("a#claim"));
    });
});

Behaviour.specify("A#reassign", "AbstractClaimBuildAction_reassign", 0, function (element) {
    element.addEventListener("click", (event) => {
        event.preventDefault();
        showPopup(event.target.closest("a#reassign"));
    });
});

Behaviour.specify(".claim-hide-popup", "AbstractClaimBuildAction_hide-popup", 0, function (element) {
    element.addEventListener("click", (event) => {
        hidePopup(event.target);
    });
});

Behaviour.specify(".claim-bfa-display-error", "AbstractClaimBuildAction_display-bfa-error", 0, function (element) {
    element.addEventListener("change", (event) => {
        display(event.target, event.target.value);
    });
});