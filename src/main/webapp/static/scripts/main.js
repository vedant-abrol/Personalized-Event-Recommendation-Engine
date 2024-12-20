(function () {
	var user_id = '1111';
	var lng = -122.08;
	var lat = 37.38;
  
	function init() {
	  document.getElementById('nearby-btn').addEventListener('click', loadNearbyItems);
	  document.getElementById('fav-btn').addEventListener('click', loadFavoriteItems);
	  document.getElementById('recommend-btn').addEventListener('click', loadRecommendedItems);
  
	  const darkModeToggle = document.getElementById('dark-mode-toggle');
	  const body = document.body;
  
	  // Initialize dark mode state
	  if (localStorage.getItem('dark-mode') === 'enabled') {
		enableDarkMode();
	  }
  
	  darkModeToggle.addEventListener('click', () => {
		if (body.classList.contains('dark-mode')) {
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
	  document.getElementById('dark-mode-toggle').innerHTML = '<i class="fa fa-sun-o"></i>';
	}
  
	function disableDarkMode() {
	  document.body.classList.remove('dark-mode');
	  localStorage.setItem('dark-mode', 'disabled');
	  document.getElementById('dark-mode-toggle').innerHTML = '<i class="fa fa-moon-o"></i>';
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
	  alert('Location retrieval failed! Using default location.');
	  loadNearbyItems();
	}
  
	function loadNearbyItems() {
	  loadItems('http://localhost:8080/EventRecommender/search', `user_id=${user_id}&lat=${lat}&lon=${lng}`, 'Nearby items not found.');
	}
  
	function loadFavoriteItems() {
	  loadItems('http://localhost:8080/EventRecommender/history', `user_id=${user_id}`, 'No favorite items found.');
	}
  
	function loadRecommendedItems() {
	  loadItems('http://localhost:8080/EventRecommender/recommendation', `user_id=${user_id}&lat=${lat}&lon=${lng}`, 'No recommended items found.');
	}
  
	function loadItems(url, params, errorMsg) {
	  fetch(`${url}?${params}`)
		.then(response => response.json())
		.then(items => {
		  if (!items || items.length === 0) {
			showErrorMessage(errorMsg);
		  } else {
			listItems(items);
		  }
		})
		.catch(() => showErrorMessage('Unable to fetch items.'));
	}
  
	function listItems(items) {
	  const itemList = document.getElementById('item-list');
	  itemList.innerHTML = '';
	  items.forEach(item => {
		const isFavorite = item.favorite === true; // Default to false if undefined
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
		// Add event listener for favorite toggle
		li.querySelector('.fav-link').addEventListener('click', toggleFavorite);
		itemList.appendChild(li);
	  });
	}
  
	function toggleFavorite(event) {
	  const favLink = event.currentTarget;
	  const item_id = favLink.dataset.itemId;
	  const isFavorite = favLink.dataset.favorite === 'true';
  
	  const method = isFavorite ? 'DELETE' : 'POST';
	  const url = 'http://localhost:8080/EventRecommender/history';
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
  
	function showErrorMessage(msg) {
	  document.getElementById('item-list').innerHTML = `<p>${msg}</p>`;
	}
  
	init();
  })();
  