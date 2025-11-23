(function () {
    const user_id = '1111';
    let lng = -122.08;
    let lat = 37.38;

    const API_BASE = window.location.origin.includes('localhost')
        ? 'http://localhost:8080/EventRecommender'
        : 'https://35.173.220.122:8443/EventRecommender';

    const itemList = document.getElementById('item-list');
    const statusBanner = document.getElementById('status-banner');
    const darkModeToggle = document.getElementById('dark-mode-toggle');

    function init() {
        document.getElementById('nearby-btn').addEventListener('click', () => {
            setActiveButton('nearby-btn');
            loadNearbyItems();
        });

        document.getElementById('fav-btn').addEventListener('click', () => {
            setActiveButton('fav-btn');
            loadFavoriteItems();
        });

        document.getElementById('recommend-btn').addEventListener('click', () => {
            setActiveButton('recommend-btn');
            loadRecommendedItems();
        });

        if (localStorage.getItem('dark-mode') === 'enabled') {
            enableDarkMode();
        }

        darkModeToggle.addEventListener('click', () => {
            if (document.body.classList.contains('dark-mode')) {
                disableDarkMode();
            } else {
                enableDarkMode();
            }
        });

        initGeoLocation();
    }

    function enableDarkMode() {
        document.body.classList.add('dark-mode');
        localStorage.setItem('dark-mode', 'enabled');
        darkModeToggle.innerHTML = '<i class="fa fa-sun-o"></i>';
    }

    function disableDarkMode() {
        document.body.classList.remove('dark-mode');
        localStorage.setItem('dark-mode', 'disabled');
        darkModeToggle.innerHTML = '<i class="fa fa-moon-o"></i>';
    }

    function initGeoLocation() {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(onPositionUpdated, onLoadPositionFailed);
        } else {
            onLoadPositionFailed();
        }
    }

    function onPositionUpdated(position) {
        lat = position.coords.latitude;
        lng = position.coords.longitude;
        loadNearbyItems();
    }

    function onLoadPositionFailed() {
        showStatus('Using default location because we could not read your GPS.', 'warning');
        loadNearbyItems();
    }

    function loadNearbyItems() {
        loadItems(
            `${API_BASE}/search`,
            `user_id=${user_id}&lat=${lat}&lon=${lng}`,
            'Nearby items not found.'
        );
    }

    function loadFavoriteItems() {
        loadItems(
            `${API_BASE}/history`,
            `user_id=${user_id}`,
            'No favorite items found.',
            true
        );
    }

    function loadRecommendedItems() {
        loadItems(
            `${API_BASE}/recommendation`,
            `user_id=${user_id}&lat=${lat}&lon=${lng}`,
            'No recommended items found.'
        );
    }

    function loadItems(url, params, errorMsg, isFavoriteView = false) {
        setLoading(true);
        clearStatus();

        fetch(`${url}?${params}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error('Unexpected response');
                }
                return response.json();
            })
            .then((items) => {
                if (!items || items.length === 0) {
                    showEmptyState(errorMsg);
                } else {
                    listItems(items, isFavoriteView);
                }
            })
            .catch(() => {
                showEmptyState('Unable to fetch items. Please try again.');
                showStatus('The service is unreachable right now. We will keep trying.', 'error');
            })
            .finally(() => setLoading(false));
    }

    function listItems(items, isFavoriteView = false) {
        itemList.innerHTML = '';
        items.forEach((item) => {
            const isFavorite = isFavoriteView || item.favorite === true;
            const li = document.createElement('li');
            li.className = 'item';
            li.innerHTML = `
                <div class="item-header">
                    <div class="badge">${item.categories?.[0] || 'Event'}</div>
                    <button class="pill-btn">${item.date || 'Date TBD'}</button>
                </div>
                <img src="${item.image_url || 'https://via.placeholder.com/640x360?text=Event'}" alt="Event Image">
                <a href="${item.url || '#'}" target="_blank" class="item-name">${item.name || 'No Title'}</a>
                <p class="item-address"><i class="fa fa-map-marker"></i> ${item.address || 'No Address'}</p>
                <p class="item-priceRange"><i class="fa fa-ticket"></i> ${item.priceRange || 'Pricing info coming soon'}</p>
                <div class="item-footer">
                    <span class="tag">${(item.categories && item.categories.slice(0, 2).join(' • ')) || 'General'}</span>
                    <div class="fav-link" data-item-id="${item.item_id}" data-favorite="${isFavorite}">
                        <i class="fa ${isFavorite ? 'fa-heart' : 'fa-heart-o'}"></i>
                    </div>
                </div>
            `;

            li.querySelector('.fav-link').addEventListener('click', toggleFavorite);
            itemList.appendChild(li);
        });
    }

    function toggleFavorite(event) {
        const favLink = event.currentTarget;
        const item_id = favLink.dataset.itemId;
        const isFavorite = favLink.dataset.favorite === 'true';

        const method = isFavorite ? 'DELETE' : 'POST';
        const url = `${API_BASE}/history`;
        const payload = JSON.stringify({ user_id: user_id, favorite: [item_id] });

        favLink.classList.add('busy');

        fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: payload,
        })
            .then((response) => response.json())
            .then((result) => {
                if (result.result === 'SUCCESS') {
                    favLink.dataset.favorite = isFavorite ? 'false' : 'true';
                    favLink.querySelector('i').className = isFavorite ? 'fa fa-heart-o' : 'fa fa-heart';
                    showStatus(isFavorite ? 'Removed from favorites' : 'Saved to favorites', 'success');
                } else {
                    throw new Error('failed');
                }
            })
            .catch(() => showStatus('Failed to update favorite', 'error'))
            .finally(() => favLink.classList.remove('busy'));
    }

    function setActiveButton(buttonId) {
        const buttons = document.querySelectorAll('.nav-btn');
        buttons.forEach((btn) => btn.classList.remove('active'));
        document.getElementById(buttonId).classList.add('active');
    }

    function showEmptyState(msg) {
        itemList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon"><i class="fa fa-compass"></i></div>
                <p>${msg}</p>
                <small>Try switching tabs or adjusting your location settings.</small>
            </div>
        `;
    }

    function showStatus(message, type = 'info') {
        if (!statusBanner) return;
        statusBanner.textContent = message;
        statusBanner.className = `status-banner ${type}`;
        statusBanner.removeAttribute('hidden');
    }

    function clearStatus() {
        if (!statusBanner) return;
        statusBanner.textContent = '';
        statusBanner.className = 'status-banner';
        statusBanner.setAttribute('hidden', 'hidden');
    }

    function setLoading(isLoading) {
        itemList.classList.toggle('loading', isLoading);
        if (isLoading) {
            renderSkeletons();
        }
    }

    function renderSkeletons() {
        itemList.innerHTML = '';
        for (let i = 0; i < 6; i++) {
            const li = document.createElement('li');
            li.className = 'item skeleton';
            li.innerHTML = `
                <div class="skeleton-thumb"></div>
                <div class="skeleton-line short"></div>
                <div class="skeleton-line"></div>
                <div class="skeleton-line"></div>
            `;
            itemList.appendChild(li);
        }
    }

    init();
})();
