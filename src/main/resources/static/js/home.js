"use strict";

function changeMtpType(radio_name) {
    if (radio_name === "structured_instructions") {
        document.getElementById("structured-instructions-inputs").style.display = "block";
        document.getElementById("narrative-instructions-inputs").style.display = "none";
    } else if (radio_name === "narrative_instructions") {
        document.getElementById("narrative-instructions-inputs").style.display = "block";
        document.getElementById("structured-instructions-inputs").style.display = "none";
    }
}