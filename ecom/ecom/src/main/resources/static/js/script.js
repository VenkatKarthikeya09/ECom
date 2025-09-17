document.addEventListener('DOMContentLoaded', () => {
    console.log("script.js is running");

    // Countdown Timer Logic
    const countdown = () => {
        const timerEl = document.querySelector('.countdown-timer');
        if (!timerEl) return;
        const daysEl = document.getElementById('days');
        const hoursEl = document.getElementById('hours');
        const minutesEl = document.getElementById('minutes');
        const secondsEl = document.getElementById('seconds');
        if (!daysEl || !hoursEl || !minutesEl || !secondsEl) return;

        const now = new Date().getTime();
        const end = localStorage.getItem('saleEndTs') ? parseInt(localStorage.getItem('saleEndTs')) : (Date.now() + 24*60*60*1000);
        localStorage.setItem('saleEndTs', end);
        const distance = end - now;
        const days = Math.max(0, Math.floor(distance / (1000 * 60 * 60 * 24)));
        const hours = Math.max(0, Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)));
        const minutes = Math.max(0, Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60)));
        const seconds = Math.max(0, Math.floor((distance % (1000 * 60)) / 1000));

        daysEl.textContent = String(days).padStart(2, '0');
        hoursEl.textContent = String(hours).padStart(2, '0');
        minutesEl.textContent = String(minutes).padStart(2, '0');
        secondsEl.textContent = String(seconds).padStart(2, '0');

        if (distance <= 0) {
            clearInterval(timerInterval);
            timerEl.innerHTML = 'Sale has ended!';
            localStorage.removeItem('saleEndTs');
        }
    };

    const timerInterval = setInterval(countdown, 1000);
    countdown();

    // Utility function to render a single product card
    const renderProductCard = (product) => {
        const oldPrice = (product.price * 1.2).toFixed(2);
        
        const cart = JSON.parse(localStorage.getItem('cart')) || {};
        const isProductInCart = !!cart[product.productId];
        const quantity = isProductInCart ? cart[product.productId].quantity : 1;

        return `
            <div class="col">
                <div class="card h-100 shadow-sm product-card border-0 rounded-4" data-product-id="${product.productId}">
                    <div class="sale-tag">Sale</div>
                    <div class="product-image-container">
                        <img src="${product.imageUrl}" class="card-img-top rounded-top-4" alt="${product.name}">
                    </div>
                    <div class="card-body d-flex flex-column p-4">
                        <h5 class="card-title fw-bold">${product.name}</h5>
                        <p class="card-text text-muted mb-2">${product.description}</p>
                        <div class="d-flex align-items-baseline mt-auto">
                            <p class="fw-bold fs-5 text-primary mb-0 me-2">₹${product.price}</p>
                            <p class="text-muted text-decoration-line-through mb-0 price-old">₹${oldPrice}</p>
                        </div>
                        <div class="w-100 mt-3" data-product-id="${product.productId}">
                            <button class="btn btn-success w-100 add-to-cart-btn rounded-pill shadow-sm ${isProductInCart ? 'd-none' : ''}"
                                    data-product-id="${product.productId}"
                                    data-product-name="${product.name}"
                                    data-product-image="${product.imageUrl}"
                                    data-product-price="${product.price}">
                                Add to Cart
                            </button>
                            <div class="quantity-controls ${isProductInCart ? '' : 'd-none'}">
                                <button class="btn btn-outline-primary btn-sm rounded-pill px-3 me-2 minus-btn">-</button>
                                <input type="text" class="form-control quantity-input text-center rounded-pill" value="${quantity}" readonly>
                                <button class="btn btn-outline-primary btn-sm rounded-pill px-3 ms-2 plus-btn">+</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    };

    // --- Search Bar Logic ---
    const searchInput = document.getElementById('search-input');
    const mainContent = document.getElementById('featured-section');
    
    // Lightweight typeahead suggestions
    const suggestionBox = document.createElement('div');
    suggestionBox.className = 'position-absolute bg-white shadow rounded w-75 mt-2 p-2';
    suggestionBox.style.zIndex = '1050';
    suggestionBox.style.display = 'none';
    if (searchInput && searchInput.parentElement && searchInput.parentElement.parentElement) {
        searchInput.parentElement.parentElement.appendChild(suggestionBox);
    }

    const renderSuggestions = (products) => {
        if (!products || products.length === 0) {
            suggestionBox.style.display = 'none';
            suggestionBox.innerHTML = '';
            return;
        }
        suggestionBox.innerHTML = products.slice(0, 6).map(p => `
            <a href="/product/${p.productId}" class="d-block text-decoration-none text-dark py-1">
                <div class="d-flex align-items-center">
                    <img src="${p.imageUrl}" alt="${p.name}" class="me-2" style="width:32px;height:32px;object-fit:cover;border-radius:6px;"/>
                    <span>${p.name}</span>
                </div>
            </a>
        `).join('');
        suggestionBox.style.display = 'block';
    };

    const fetchSuggestions = async (query) => {
        if (!query || query.trim().length < 2) {
            renderSuggestions([]);
            return;
        }
        try {
            const response = await fetch(`/api/products/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('bad');
            const products = await response.json();
            renderSuggestions(products);
        } catch (e) {
            renderSuggestions([]);
        }
    };

    if (searchInput) {
        searchInput.addEventListener('input', (e) => fetchSuggestions(e.target.value));
        document.addEventListener('click', (e) => {
            if (!suggestionBox.contains(e.target) && e.target !== searchInput) {
                suggestionBox.style.display = 'none';
            }
        });
        const form = document.getElementById('search-form');
        if (form) {
            form.addEventListener('submit', (e) => {
                // Let the form navigate to /search
                suggestionBox.style.display = 'none';
    });
        }
    }

    // --- New Sections Logic ---
    const recentlySearchedSection = document.getElementById('recently-searched-section');
    const recentlySearchedContainer = document.getElementById('recently-searched-container');
    const suggestedProductsContainer = document.getElementById('suggested-products-container');

    const fetchAndRenderRecentlySearched = async () => {
        const searchHistory = JSON.parse(localStorage.getItem('searchHistory')) || [];
        if (searchHistory.length === 0) {
            recentlySearchedSection.style.display = 'none';
            return;
        }

        try {
            const response = await fetch(`/api/products/recently-searched?productIds=${searchHistory.join(',')}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const products = await response.json();

            recentlySearchedContainer.innerHTML = '';
            products.forEach(product => {
                recentlySearchedContainer.innerHTML += renderProductCard(product);
            });
            recentlySearchedSection.style.display = 'block';
            initializeProductUIs(); // Re-initialize UI for new products
        } catch (error) {
            console.error('Error fetching recently searched products:', error);
        }
    };

    const fetchAndRenderSuggestedProducts = async () => {
        try {
            const response = await fetch(`/api/products/suggested`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const products = await response.json();

            suggestedProductsContainer.innerHTML = '';
            products.forEach(product => {
                suggestedProductsContainer.innerHTML += renderProductCard(product);
            });
            initializeProductUIs(); // Re-initialize UI for new products
        } catch (error) {
            console.error('Error fetching suggested products:', error);
        }
    };
    
    // --- Cart Notification and Controls (Existing Logic) ---
    const cartNotification = document.getElementById('cart-notification');
    const notificationImg = document.getElementById('notification-img');
    const notificationName = document.getElementById('notification-name');
    const notificationMessage = document.getElementById('notification-message');
    const cartSizeCounter = document.getElementById('cart-size');
    let notificationTimeout;

    const cart = JSON.parse(localStorage.getItem('cart')) || {};

    const saveCart = () => {
        localStorage.setItem('cart', JSON.stringify(cart));
    };

    const computeLocalCartQty = () => Object.values(cart).reduce((sum, it) => sum + Number(it.quantity || 0), 0);

    const updateCartSize = () => {
        const localQty = computeLocalCartQty();
        cartSizeCounter.textContent = localQty;
        if (window.updateNavbarCartBadge) window.updateNavbarCartBadge();
    };

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

    window.hideNotification = () => {
        clearTimeout(notificationTimeout);
        cartNotification.classList.remove('show');
    };

    const updateProductUI = (productId, quantity) => {
        const productCard = document.querySelector(`.product-card[data-product-id="${productId}"]`);
        if (!productCard) return;

        const addToCartBtn = productCard.querySelector('.add-to-cart-btn');
        const quantityControls = productCard.querySelector('.quantity-controls');
        const quantityInput = productCard.querySelector('.quantity-input');

        if (quantity > 0) {
            if (addToCartBtn) addToCartBtn.classList.add('d-none');
            if (quantityControls) quantityControls.classList.remove('d-none');
            if (quantityInput) quantityInput.value = quantity;
        } else {
            if (addToCartBtn) addToCartBtn.classList.remove('d-none');
            if (quantityControls) quantityControls.classList.add('d-none');
        }
    };

    const initializeProductUIs = () => {
        // This function needs to handle all product cards on the page, not just the initial ones.
        document.querySelectorAll('.product-card').forEach(card => {
            const productId = card.dataset.productId;
            const quantity = cart[productId] ? cart[productId].quantity : 0;
            updateProductUI(productId, quantity);
        });
    };

    document.addEventListener('click', (event) => {
        const target = event.target;

        if (target.classList.contains('add-to-cart-btn')) {
            const button = target;
            const productId = button.dataset.productId;
            const productName = button.dataset.productName;
            const productImage = button.dataset.productImage;
            const productPrice = parseFloat(button.dataset.productPrice);
            const quantity = 1;

            if (cart[productId]) {
                cart[productId].quantity++;
            } else {
                cart[productId] = {
                    name: productName,
                    image: productImage,
                    price: productPrice,
                    quantity: quantity
                };
            }
            saveCart();
            updateCartSize();
            updateProductUI(productId, cart[productId].quantity);
            showNotification(productName, productImage, 'Item added to your cart.');
        }

        if (target.classList.contains('minus-btn')) {
            const productCard = target.closest('.product-card');
            const productId = productCard.dataset.productId;
            
            if (cart[productId] && cart[productId].quantity > 0) {
                cart[productId].quantity--;
                if (cart[productId].quantity === 0) {
                    showConfirmationModal(productId);
                } else {
                    saveCart();
                    updateProductUI(productId, cart[productId].quantity);
                    updateCartSize();
                }
            }
        }

        if (target.classList.contains('plus-btn')) {
            const productCard = target.closest('.product-card');
            const productId = productCard.dataset.productId;

            if (cart[productId]) {
                cart[productId].quantity++;
                saveCart();
                updateProductUI(productId, cart[productId].quantity);
                updateCartSize();
            }
        }
    });

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

    confirmNoBtn.addEventListener('click', () => {
        hideConfirmationModal();
        if (productToRemoveId && cart[productToRemoveId]) {
            cart[productToRemoveId].quantity = 1;
            saveCart();
            updateProductUI(productToRemoveId, cart[productToRemoveId].quantity);
        }
    });

    closeBtnModal.addEventListener('click', () => {
        hideConfirmationModal();
        if (productToRemoveId && cart[productToRemoveId]) {
            cart[productToRemoveId].quantity = 1;
            saveCart();
            updateProductUI(productToRemoveId, cart[productToRemoveId].quantity);
        }
    });

    confirmYesBtn.addEventListener('click', () => {
        if (productToRemoveId) {
            const productName = cart[productToRemoveId].name;
            const productImage = cart[productToRemoveId].image;
            delete cart[productToRemoveId];
            saveCart();
            updateCartSize();
            updateProductUI(productToRemoveId, 0);
            hideConfirmationModal();
            showNotification(productName, productImage, 'Item has been removed from your cart.');
        }
    });

    // Initial page load functions
    updateCartSize();
    fetchAndRenderRecentlySearched();
    fetchAndRenderSuggestedProducts();
    initializeProductUIs();
});