(function () {
    if (window.addEventListener) {
        window.addEventListener('DOMContentLoaded', domReady, false);
    } else {
        window.attachEvent('onload', domReady);
    }
} ());

var enabled;

function domReady() {
    var param = new URLSearchParams(window.location.search)
    enabled = param.get('enabled') != 'false';
    var toggle = document.getElementById("masterToggle");
    if (toggle != null) {
        toggle.checked = enabled;
    }
}

function toggleEnabled() {
    window.location.search = "enabled=" + !enabled;
}