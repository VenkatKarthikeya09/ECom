$(document).ready(function() {
    const usernameOrEmailInput = $('#usernameOrEmail');
    const passwordInput = $('#password');
    const eyeIcon = $('#eye-icon');
    const eyeSvgOpen = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle>';
    const eyeSvgClosed = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.94 3.06"></path><path d="M1.06 1.06l22.88 22.88"></path><path d="M15 15a3 3 0 1 0-3-3"></path>';

    // Function to validate email format
    function isEmailValid(email) {
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return regex.test(email);
    }

    // Function to validate username format (no spaces, minimum length)
    function isUsernameValid(username) {
        return username.length >= 3 && !/\s/.test(username);
    }

    // Function to validate usernameOrEmail field
    function validateUsernameOrEmail(value) {
        if (value.includes('@')) {
            return isEmailValid(value) ? '' : 'Please enter a valid email address.';
        } else {
            return isUsernameValid(value) ? '' : 'Username must be at least 3 characters and contain no spaces.';
        }
    }

    // Function to show/hide password
    function togglePasswordVisibility() {
        const passwordField = passwordInput;
        if (passwordField.attr('type') === 'password') {
            passwordField.attr('type', 'text');
            eyeIcon.html(eyeSvgOpen);
        } else {
            passwordField.attr('type', 'password');
            eyeIcon.html(eyeSvgClosed);
        }
    }

    // Password toggle event
    $('#password-toggle').on('click', togglePasswordVisibility);

    // Real-time validation for usernameOrEmail
    usernameOrEmailInput.on('input', function() {
        const value = $(this).val();
        const error = validateUsernameOrEmail(value);
        if (error) {
            $(this).removeClass('is-valid').addClass('is-invalid');
            let feedback = $('#usernameOrEmail-feedback');
            if (feedback.length === 0) {
                $(this).closest('.form-with-icon').after(`<div id="usernameOrEmail-feedback" class="invalid-feedback">${error}</div>`);
            } else {
                feedback.text(error);
            }
        } else {
            $(this).removeClass('is-invalid').addClass('is-valid');
            $('#usernameOrEmail-feedback').remove();
        }
    });

    // Real-time validation for password (basic check)
    passwordInput.on('input', function() {
        const value = $(this).val();
        if (value.length === 0) {
            $(this).removeClass('is-valid is-invalid');
            $('#password-feedback').remove();
        } else if (value.length < 6) {
            $(this).removeClass('is-valid').addClass('is-invalid');
            let feedback = $('#password-feedback');
            if (feedback.length === 0) {
                $(this).closest('.form-with-icon').after('<div id="password-feedback" class="invalid-feedback">Password must be at least 6 characters.</div>');
            }
        } else {
            $(this).removeClass('is-invalid').addClass('is-valid');
            $('#password-feedback').remove();
        }
    });
});