(function () {
    // Global variables to store user ID and default coordinates
    var user_id = '1111';
    var lng = -122.08;
    var lat = 37.38;

    // Initialize the application
    function init() {
        // Event listeners for navigation buttons
        document.getElementById('nearby-btn').addEventListener('click', () => {
            setActiveButton('nearby-btn'); // Highlight the active button
            loadNearbyItems(); // Load nearby events
        });
        document.getElementById('fav-btn').addEventListener('click', () => {
            setActiveButton('fav-btn'); // Highlight the active button
            loadFavoriteItems(); // Load favorite items
        });
        document.getElementById('recommend-btn').addEventListener('click', () => {
            setActiveButton('recommend-btn'); // Highlight the active button
            loadRecommendedItems(); // Load recommended events
        });

        // Dark mode toggle initialization
        const darkModeToggle = document.getElementById('dark-mode-toggle');
        const body = document.body;

        // Check and apply saved dark mode preference
        if (localStorage.getItem('dark-mode') === 'enabled') {
            enableDarkMode();
        }

        // Add event listener for dark mode toggle
        darkModeToggle.addEventListener('click', () => {
            if (body.classList.contains('dark-mode')) {
                disableDarkMode();
            } else {
                enableDarkMode();
            }
        });

        // Initialize geolocation to get user's current location
        initGeoLocation();
    }

    // Enable dark mode
    function enableDarkMode() {
        document.body.classList.add('dark-mode');
        localStorage.setItem('dark-mode', 'enabled'); // Save preference
        document.getElementById('dark-mode-toggle').innerHTML = '<i class="fa fa-sun-o"></i>';
    }

    // Disable dark mode
    function disableDarkMode() {
        document.body.classList.remove('dark-mode');
        localStorage.setItem('dark-mode', 'disabled'); // Save preference
        document.getElementById('dark-mode-toggle').innerHTML = '<i class="fa fa-moon-o"></i>';
    }

    // Initialize geolocation to fetch user's current position
    function initGeoLocation() {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(onPositionUpdated, onLoadPositionFailed);
        } else {
            onLoadPositionFailed(); // Use default location if geolocation fails
        }
    }

    // Update coordinates on successful geolocation
    function onPositionUpdated(position) {
        lat = position.coords.latitude;
        lng = position.coords.longitude;
        loadNearbyItems(); // Load nearby events based on current location
    }

    // Handle geolocation failure
    function onLoadPositionFailed() {
        alert('Location retrieval failed! Using default location.');
        loadNearbyItems();
    }

    // Load nearby events
    function loadNearbyItems() {
        loadItems(
            'https://35.173.220.122:8443/EventRecommender/search',
            `user_id=${user_id}&lat=${lat}&lon=${lng}`,
            'Nearby items not found.'
        );
    }

    // Load favorite items
    function loadFavoriteItems() {
        loadItems(
            'https://35.173.220.122:8443/EventRecommender/history',
            `user_id=${user_id}`,
            'No favorite items found.',
            true // Indicate favorite view
        );
    }

    // Load recommended events
    function loadRecommendedItems() {
        loadItems(
            'https://35.173.220.122:8443/EventRecommender/recommendation',
            `user_id=${user_id}&lat=${lat}&lon=${lng}`,
            'No recommended items found.'
        );
    }

    // Generic function to load items from a specific URL
    function loadItems(url, params, errorMsg, isFavoriteView = false) {
        fetch(`${url}?${params}`)
            .then(response => response.json())
            .then(items => {
                if (!items || items.length === 0) {
                    showErrorMessage(errorMsg); // Show error if no items found
                } else {
                    listItems(items, isFavoriteView); // Display items
                }
            })
            .catch(() => showErrorMessage('Unable to fetch items.'));
    }

    // Display the list of items in the UI
    function listItems(items, isFavoriteView = false) {
        const itemList = document.getElementById('item-list');
        itemList.innerHTML = ''; // Clear previous items
        items.forEach(item => {
            const isFavorite = isFavoriteView || item.favorite === true; // Ensure correct favorite status
            const li = document.createElement('li');
            li.className = 'item';
            li.innerHTML = `
                <img src="${item.image_url || 'https://via.placeholder.com/300'}" alt="Event Image">
                <a href="${item.url || '#'}" target="_blank" class="item-name">${item.name || 'No Title'}</a>
                <p class="item-category">Category: ${item.categories?.join(', ') || 'N/A'}</p>
                <p class="item-priceRange">Price: ${item.priceRange || 'N/A'}</p>
                <p class="item-address">${item.address || 'No Address'}</p>
                <p class="item-startDate">${item.date || 'No Date'}</p>
                <div class="fav-link" data-item-id="${item.item_id}" data-favorite="${isFavorite}">
                    <i class="fa ${isFavorite ? 'fa-heart' : 'fa-heart-o'}"></i>
                </div>
            `;
            // Add favorite toggle functionality
            li.querySelector('.fav-link').addEventListener('click', toggleFavorite);
            itemList.appendChild(li);
        });
    }

    // Toggle favorite status for an item
    function toggleFavorite(event) {
        const favLink = event.currentTarget;
        const item_id = favLink.dataset.itemId;
        const isFavorite = favLink.dataset.favorite === 'true';

        const method = isFavorite ? 'DELETE' : 'POST';
        const url = 'https://35.173.220.122:8443/EventRecommender/history';
        const payload = JSON.stringify({ user_id: user_id, favorite: [item_id] });

        fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: payload,
        })
            .then(response => response.json())
            .then(result => {
                if (result.result === 'SUCCESS') {
                    favLink.dataset.favorite = isFavorite ? 'false' : 'true';
                    favLink.querySelector('i').className = isFavorite ? 'fa fa-heart-o' : 'fa fa-heart';
                } else {
                    alert('Failed to update favorite.');
                }
            })
            .catch(() => alert('Error updating favorite.'));
    }

    // Set the active navigation button
    function setActiveButton(buttonId) {
        const buttons = document.querySelectorAll('.nav-btn');
        buttons.forEach(btn => btn.classList.remove('active'));
        document.getElementById(buttonId).classList.add('active');
    }

    // Display an error message in the UI
    function showErrorMessage(msg) {
        document.getElementById('item-list').innerHTML = `<p>${msg}</p>`;
    }

    init(); // Initialize the app on load
})();
