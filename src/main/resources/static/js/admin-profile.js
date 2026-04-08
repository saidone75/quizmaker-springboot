document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('themePreferenceForm');
    const select = document.getElementById('themePreference');
    if (!form || !select) return;

    select.addEventListener('change', () => form.submit());
});
