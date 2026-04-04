(function () {
    const form = document.getElementById('student-login-form');
    const hiddenKeyword = document.getElementById('keyword-hidden');
    const otpInputs = Array.from(document.querySelectorAll('.otp-input'));

    const syncKeyword = () => {
        hiddenKeyword.value = otpInputs.map((input) => input.value).join('');
    };

    const maybeAutoSubmit = () => {
        syncKeyword();
        if (hiddenKeyword.value.length === 5) {
            form.requestSubmit();
        }
    };

    otpInputs.forEach((input, index) => {
        input.addEventListener('input', (event) => {
            const normalized = event.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
            event.target.value = normalized.slice(-1);
            syncKeyword();

            if (event.target.value && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }

            if (index === otpInputs.length - 1) {
                maybeAutoSubmit();
            }
        });

        input.addEventListener('keydown', (event) => {
            if (event.key === 'Backspace' && !input.value && index > 0) {
                otpInputs[index - 1].focus();
            }
        });

        input.addEventListener('paste', (event) => {
            event.preventDefault();
            const chars = (event.clipboardData?.getData('text') || '')
                .replace(/[^a-zA-Z0-9]/g, '')
                .toUpperCase()
                .slice(0, otpInputs.length)
                .split('');

            otpInputs.forEach((otpInput, otpIndex) => {
                otpInput.value = chars[otpIndex] || '';
            });

            syncKeyword();

            const nextIndex = Math.min(chars.length, otpInputs.length - 1);
            otpInputs[nextIndex].focus();
            if (chars.length === otpInputs.length) {
                form.requestSubmit();
            }
        });
    });

    form?.addEventListener('submit', () => {
        hiddenKeyword.value = otpInputs.map((input) => input.value).join('');
    });

    otpInputs[0]?.focus();
})();
