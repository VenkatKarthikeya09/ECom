document.addEventListener('DOMContentLoaded', () => {

    const cartContainer = document.querySelector('main.container');
    const cartItemsContainer = document.getElementById('cart-items-container');
    const checkoutBtn = document.getElementById('checkout-btn');
    const billDetailsList = document.getElementById('bill-details-list');

    const cartNotification = document.getElementById('cart-notification');
    const notificationImg = document.getElementById('notification-img');
    const notificationName = document.getElementById('notification-name');
    const notificationMessage = document.getElementById('notification-message');
    const cartSizeNavElement = document.getElementById('cart-size');
    let notificationTimeout;
    
    // --- Savings UI refs ---
    const savingsDetailsList = document.getElementById('savings-details-list');
    const cartSavedAmountElement = document.getElementById('cart-saved-amount');
    
    // --- Confirmation Modal Logic ---
    const confirmationModal = document.getElementById('confirmation-modal');
    const confirmYesBtn = document.getElementById('confirm-yes');
    const confirmNoBtn = document.getElementById('confirm-no');
    const closeBtnModal = document.querySelector('.close-btn-modal');
    let productToRemoveId = null;

    const showConfirmationModal = (productId) => {
        productToRemoveId = productId;
        confirmationModal.style.display = 'flex';
    };

    const hideConfirmationModal = () => {
        confirmationModal.style.display = 'none';
        productToRemoveId = null;
    };

    closeBtnModal.addEventListener('click', () => {
        hideConfirmationModal();
    });

    confirmNoBtn.addEventListener('click', () => {
        hideConfirmationModal();
    });

    confirmYesBtn.addEventListener('click', () => {
        if (productToRemoveId) {
            const cart = JSON.parse(localStorage.getItem('cart')) || {};
            const productName = cart[productToRemoveId].name;
            const productImage = cart[productToRemoveId].image;
            delete cart[productToRemoveId];
            localStorage.setItem('cart', JSON.stringify(cart));
            
            renderCartItems(); // Re-render the cart UI
            
            hideConfirmationModal();
            showNotification(productName, productImage, 'Item has been removed from your cart.');
        }
    });

    // --- Notification Logic ---
    const showNotification = (productName, imageUrl, message) => {
        clearTimeout(notificationTimeout);
        notificationImg.src = imageUrl;
        notificationName.textContent = productName;
        notificationMessage.textContent = message;
        cartNotification.classList.add('show');
        notificationTimeout = setTimeout(() => {
            cartNotification.classList.remove('show');
        }, 3000);
    };

    // Global function to hide notification
    window.hideNotification = () => {
        clearTimeout(notificationTimeout);
        cartNotification.classList.remove('show');
    };

    // --- Bill Details Rendering Logic ---
    const renderBillDetails = (cart) => {
        billDetailsList.innerHTML = '';
        if (Object.keys(cart).length === 0) {
            billDetailsList.innerHTML = `<li class="text-muted">No items in bill.</li>`;
            return;
        }

        for (const productId in cart) {
            const item = cart[productId];
            const itemTotal = parseFloat(item.price) * parseFloat(item.quantity);
            const detailHTML = `
                <li class="d-flex justify-content-between mb-1">
                    <span class="text-muted">${item.name} (${item.quantity}x)</span>
                    <span class="fw-bold">₹${itemTotal.toFixed(2)}</span>
                </li>
            `;
            billDetailsList.innerHTML += detailHTML;
        }
    };

    // --- Savings rendering ---
    const renderSavings = (cart) => {
        if (!savingsDetailsList || !cartSavedAmountElement) return;
        savingsDetailsList.innerHTML = '';
        let savedTotal = 0;
        for (const productId in cart) {
            const item = cart[productId];
            const price = parseFloat(item.price);
            const quantity = parseFloat(item.quantity);
            // Heuristic: List price is 20% above selling price
            const listPrice = price * 1.2;
            const perUnitSaved = Math.max(0, listPrice - price);
            const saved = perUnitSaved * quantity;
            savedTotal += saved;
            const row = `
                <li class="d-flex justify-content-between mb-1">
                    <span class="text-muted">${item.name} (${quantity}x)</span>
                    <span class="text-success">₹${saved.toFixed(2)}</span>
                </li>`;
            savingsDetailsList.innerHTML += row;
        }
        cartSavedAmountElement.textContent = `₹${savedTotal.toFixed(2)}`;
    };

    // --- Main Cart Rendering Logic ---
    const updateCartTotals = () => {
        const cart = JSON.parse(localStorage.getItem('cart')) || {};
        let totalPrice = 0;
        let totalItems = 0;

        for (const productId in cart) {
            const item = cart[productId];
            totalPrice += parseFloat(item.price) * parseFloat(item.quantity);
            totalItems++;
        }
        
        const totalPriceElement = document.getElementById('cart-total-price');
        const finalTotalPriceElement = document.getElementById('final-total-price');
        const cartItemCountElement = document.getElementById('cart-item-count');
        
        totalPriceElement.textContent = `₹${totalPrice.toFixed(2)}`;
        finalTotalPriceElement.textContent = `₹${totalPrice.toFixed(2)}`;
        cartItemCountElement.textContent = totalItems;
        cartSizeNavElement.textContent = totalItems;

        // Render savings after totals so UI shows consistent figures
        renderSavings(cart);

        if (totalItems > 0) {
            checkoutBtn.classList.remove('disabled');
        } else {
            checkoutBtn.classList.add('disabled');
        }
    };

    const renderCartItems = () => {
        const cart = JSON.parse(localStorage.getItem('cart')) || {};
        cartItemsContainer.innerHTML = '';

        if (Object.keys(cart).length === 0) {
            cartItemsContainer.innerHTML = `
                <div class="col-12">
                    <div class="alert alert-info text-center mt-4" role="alert">
                        Your cart is empty. <a href="/" class="alert-link">Start shopping now!</a>
                    </div>
                </div>
            `;
        } else {
            for (const productId in cart) {
                const item = cart[productId];
                const itemTotal = parseFloat(item.price) * parseFloat(item.quantity);

                const cartItemHTML = `
                    <div class="col-12 mb-4">
                        <div class="card cart-item border-0 shadow-sm rounded-4" data-product-id="${productId}">
                            <div class="card-body p-4 d-flex flex-column flex-md-row align-items-center">
                                <img src="${item.image}" alt="${item.name}" class="img-fluid rounded-3 me-md-4 mb-3 mb-md-0" style="width: 120px; height: 120px; object-fit: cover;">
                                <div class="flex-grow-1 text-center text-md-start me-md-4">
                                    <h5 class="fw-bold mb-1">${item.name}</h5>
                                    <p class="text-muted mb-2">₹${parseFloat(item.price).toFixed(2)} per item</p>
                                </div>
                                <div class="d-flex align-items-center justify-content-center quantity-control my-3 my-md-0 me-md-4">
                                    <button class="btn btn-outline-secondary btn-sm rounded-pill px-3 me-2 btn-minus" data-product-id="${productId}">-</button>
                                    <input type="text" class="form-control quantity-input text-center rounded-pill" value="${item.quantity}" readonly style="width: 60px;">
                                    <button class="btn btn-outline-secondary btn-sm rounded-pill px-3 ms-2 btn-plus" data-product-id="${productId}">+</button>
                                </div>
                                <p class="fw-bold fs-5 text-primary ms-md-auto mb-0 item-total-price" data-product-id="${productId}">₹${itemTotal.toFixed(2)}</p>
                                <button class="btn btn-sm btn-outline-danger ms-md-4 mt-3 mt-md-0 remove-item-btn rounded-pill" data-product-id="${productId}">
                                    <i class="bi bi-trash"></i> Remove
                                </button>
                            </div>
                        </div>
                    </div>
                `;
                cartItemsContainer.innerHTML += cartItemHTML;
            }
        }
        updateCartTotals();
        renderBillDetails(cart);
    };

    // --- Event Listeners for Quantity and Remove Buttons ---
    document.addEventListener('click', (event) => {
        const target = event.target;
        
        if (target.classList.contains('btn-plus') || target.classList.contains('btn-minus') || target.closest('.remove-item-btn')) {
            const productId = target.dataset.productId || target.closest('.remove-item-btn').dataset.productId;
            const cart = JSON.parse(localStorage.getItem('cart')) || {};

            if (target.classList.contains('btn-plus')) {
                cart[productId].quantity = parseFloat(cart[productId].quantity) + 1;
                showNotification(cart[productId].name, cart[productId].image, 'Quantity updated.');
            } else if (target.classList.contains('btn-minus')) {
                if (cart[productId].quantity > 1) {
                    cart[productId].quantity = parseFloat(cart[productId].quantity) - 1;
                    showNotification(cart[productId].name, cart[productId].image, 'Quantity updated.');
                } else {
                    showConfirmationModal(productId);
                    return;
                }
            } else if (target.closest('.remove-item-btn')) {
                showConfirmationModal(productId);
                return;
            }

            localStorage.setItem('cart', JSON.stringify(cart));
            renderCartItems();
        }
    });

    // --- Event Listener for Checkout Button ---
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', () => {
            const cart = JSON.parse(localStorage.getItem('cart')) || {};
            if (Object.keys(cart).length > 0) {
                window.location.href = '/checkout';
            } else {
                alert('Your cart is empty. Please add items before proceeding to checkout.');
            }
        });
    }

    // Initial render of cart items on page load
    renderCartItems();

});