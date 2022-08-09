"use strict";

function changeMtpType(radio_name) {
    if (radio_name === "structured_instructions") {
        document.getElementById("structured-instructions-inputs").style.display = "block";
        document.getElementById("narrative-instructions-inputs").style.display = "none";

        setRequiredInput("narrative-instructions-inputs", false);
        setRequiredInput("structured-instructions-inputs", true);
    } else if (radio_name === "narrative_instructions") {
        document.getElementById("narrative-instructions-inputs").style.display = "block";
        document.getElementById("structured-instructions-inputs").style.display = "none";

        setRequiredInput("narrative-instructions-inputs", true);
        setRequiredInput("structured-instructions-inputs", false);
    }
}

function setRequiredInput(containerId, isRequired) {
    document.getElementById(containerId).querySelectorAll("input, textarea").forEach((input) => {
        if (!input.hasAttribute("data-optional"))
            input.toggleAttribute("required", isRequired);
    });
}