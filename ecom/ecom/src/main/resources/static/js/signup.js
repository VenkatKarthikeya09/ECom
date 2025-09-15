$(document).ready(function() {
    const signupForm = $('#signup-form');
    const firstNameInput = $('#firstName');
    const lastNameInput = $('#lastName');
    const usernameInput = $('#username');
    const emailInput = $('#email');
    const passwordInput = $('#password');
    const confirmPasswordInput = $('#confirmPassword');
    const messageContainer = $('#message-container');

    const firstNameFeedback = $('#firstName-feedback');
    const lastNameFeedback = $('#lastName-feedback');
    const usernameFeedback = $('#username-feedback');
    const emailFeedback = $('#email-feedback');
    const passwordFeedback = $('#password-feedback');
    const confirmPasswordFeedback = $('#confirmPassword-feedback');

    // Password toggle SVG icons (inline)
    const eyeSvgOpen = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle>';
    const eyeSvgClosed = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.94 3.06"></path><path d="M1.06 1.06l22.88 22.88"></path><path d="M15 15a3 3 0 1 0-3-3"></path>';

    // Function to check for spaces
    function hasSpaces(str) {
        return /\s/.test(str);
    }

    // Function to validate name/username length
    function isLengthValid(str) {
        return str.length >= 3;
    }

    // Function to validate email format
    function isEmailValid(email) {
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return regex.test(email);
    }

    // Function to validate password complexity
    function validatePassword(password) {
        const feedback = [];
        if (password.length < 8) {
            feedback.push('Password must be at least 8 characters long.');
        }
        if (!/[A-Z]/.test(password)) {
            feedback.push('Password must contain at least one uppercase letter.');
        }
        if (!/[a-z]/.test(password)) {
            feedback.push('Password must contain at least one lowercase letter.');
        }
        if (!/[0-9]/.test(password)) {
            feedback.push('Password must contain at least one number.');
        }
        if (!/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password)) {
            feedback.push('Password must contain at least one special character.');
        }
        return feedback;
    }

    // Function to show/hide password
    function togglePasswordVisibility(passwordField, eyeIcon) {
        if (passwordField.attr('type') === 'password') {
            passwordField.attr('type', 'text');
            eyeIcon.html(eyeSvgOpen);
        } else {
            passwordField.attr('type', 'password');
            eyeIcon.html(eyeSvgClosed);
        }
    }

    // Password toggle events
    $('#password-toggle').on('click', function() {
        togglePasswordVisibility(passwordInput, $('#password-eye-icon'));
    });

    $('#confirmPassword-toggle').on('click', function() {
        togglePasswordVisibility(confirmPasswordInput, $('#confirmPassword-eye-icon'));
    });

    // Live validation for input fields (unchanged)
    firstNameInput.on('input', function() {
        const value = $(this).val();
        if (hasSpaces(value)) {
            firstNameFeedback.text('First name cannot contain spaces.');
        } else if (!isLengthValid(value)) {
            firstNameFeedback.text('First name must be at least 3 characters.');
        } else {
            firstNameFeedback.text('');
        }
    });

    lastNameInput.on('input', function() {
        const value = $(this).val();
        if (hasSpaces(value)) {
            lastNameFeedback.text('Last name cannot contain spaces.');
        } else if (!isLengthValid(value)) {
            lastNameFeedback.text('Last name must be at least 3 characters.');
        } else {
            lastNameFeedback.text('');
        }
    });

    usernameInput.on('input', function() {
        const value = $(this).val();
        if (hasSpaces(value)) {
            usernameFeedback.text('Username cannot contain spaces.');
        } else if (!isLengthValid(value)) {
            usernameFeedback.text('Username must be at least 3 characters.');
        } else {
            usernameFeedback.text('');
        }
    });

    emailInput.on('input', function() {
        const value = $(this).val();
        if (!isEmailValid(value)) {
            emailFeedback.text('Please enter a valid email address.');
        } else {
            emailFeedback.text('');
        }
    });

    passwordInput.on('input', function() {
        const password = $(this).val();
        const feedback = validatePassword(password);
        if (feedback.length > 0) {
            passwordFeedback.html(feedback.join('<br>'));
        } else {
            passwordFeedback.text('');
        }
    });

    confirmPasswordInput.on('input', function() {
        const password = passwordInput.val();
        const confirmPassword = $(this).val();
        if (password !== confirmPassword) {
            confirmPasswordFeedback.text('Passwords do not match.');
        } else {
            confirmPasswordFeedback.text('');
        }
    });

    // Form submission handler
    signupForm.on('submit', function(event) {
        event.preventDefault();
        let isValid = true;
        messageContainer.html('');

        // Re-run all validations on submit (unchanged)
        const firstName = firstNameInput.val();
        const lastName = lastNameInput.val();
        const username = usernameInput.val();
        const email = emailInput.val();
        const password = passwordInput.val();
        const confirmPassword = confirmPasswordInput.val();

        if (firstName === '') {
            firstNameFeedback.text('First name is required.');
            isValid = false;
        } else if (hasSpaces(firstName) || !isLengthValid(firstName)) {
            firstNameFeedback.text(hasSpaces(firstName) ? 'First name cannot contain spaces.' : 'First name must be at least 3 characters.');
            isValid = false;
        } else {
            firstNameFeedback.text('');
        }

        if (lastName === '') {
            lastNameFeedback.text('Last name is required.');
            isValid = false;
        } else if (hasSpaces(lastName) || !isLengthValid(lastName)) {
            lastNameFeedback.text(hasSpaces(lastName) ? 'Last name cannot contain spaces.' : 'Last name must be at least 3 characters.');
            isValid = false;
        } else {
            lastNameFeedback.text('');
        }

        if (username === '') {
            usernameFeedback.text('Username is required.');
            isValid = false;
        } else if (hasSpaces(username) || !isLengthValid(username)) {
            usernameFeedback.text(hasSpaces(username) ? 'Username cannot contain spaces.' : 'Username must be at least 3 characters.');
            isValid = false;
        } else {
            usernameFeedback.text('');
        }

        if (email === '') {
            emailFeedback.text('Email is required.');
            isValid = false;
        } else if (!isEmailValid(email)) {
            emailFeedback.text('Please enter a valid email address.');
            isValid = false;
        } else {
            emailFeedback.text('');
        }
            
        const passwordErrors = validatePassword(password);
        if (password === '') {
            passwordFeedback.text('Password is required.');
            isValid = false;
        } else if (passwordErrors.length > 0) {
            passwordFeedback.html(passwordErrors.join('<br>'));
            isValid = false;
        } else {
            passwordFeedback.text('');
        }

        if (confirmPassword === '') {
            confirmPasswordFeedback.text('Confirm password is required.');
            isValid = false;
        } else if (password !== confirmPassword) {
            confirmPasswordFeedback.text('Passwords do not match.');
            isValid = false;
        } else {
            confirmPasswordFeedback.text('');
        }
            
        if (isValid) {
            const submitButton = signupForm.find('button[type="submit"]');
            const originalText = submitButton.text();

            // Prepare form data as a JavaScript object
            const formData = {
                firstName: firstName,
                lastName: lastName,
                username: username,
                email: email,
                password: password
            };

            // Send form data to the server using AJAX
            $.ajax({
                type: 'POST',
                url: '/register',
                contentType: 'application/json', // Specify the content type
                data: JSON.stringify(formData), // Convert the object to a JSON string
                beforeSend: function() {
                    submitButton.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Creating Account...');
                },
                success: function(response) {
                    messageContainer.html('<div class="alert alert-success mb-3" role="alert">Sign up successful! Redirecting...</div>');
                    // In a real application, you'd handle redirection here, but since the Spring
                    // controller handles it, a simple delay is used for demonstration.
                    setTimeout(() => {
                         window.location.href = '/login';
                    }, 2000);
                },
                error: function(xhr, status, error) {
                    let errorMessage = 'An error occurred. Please try again.';
                    if (xhr.status === 409) { // Conflict status code for username/email conflict
                        errorMessage = 'Username or email already exists. Please choose a different one.';
                    } else if (xhr.responseText) {
                        errorMessage = xhr.responseText;
                    }
                    messageContainer.html('<div class="alert alert-danger mb-3" role="alert">' + errorMessage + '</div>');
                    submitButton.prop('disabled', false).text(originalText);
                }
            });
        } else {
            messageContainer.html('<div class="alert alert-danger mb-3" role="alert">Please correct the errors and try again.</div>');
        }
    });
});