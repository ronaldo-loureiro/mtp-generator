"use strict"

function copyToClipboard(elementId) {
    let element = document.getElementById(elementId);
    navigator.clipboard.writeText(element.innerText);
}

function downloadDoc(filename, content) {
    let pom = document.createElement('a');
    let bb = new Blob([content], {type: 'text/plain'});

    pom.setAttribute('href', window.URL.createObjectURL(bb));
    pom.setAttribute('download', filename);

    pom.dataset.downloadurl = ['text/plain', pom.download, pom.href].join(':');
    pom.click();
}

function btnCopyToClipboardClick(btn, elementId) {
    copyToClipboard(elementId);
}

function btnDownloadMtp(elementId) {
    let elementToDown = document.getElementById(elementId);
    let filename = document.getElementById("input-filename").value + ".xml";
    downloadDoc(filename, elementToDown.innerText);
}