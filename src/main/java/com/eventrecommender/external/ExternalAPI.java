package com.eventrecommender.external;

import java.util.List;
import com.eventrecommender.entity.Item;

public interface ExternalAPI {
	public List<Item> getNearbyEvents(double lat, double lon);
	public List<Item> searchEventsByKeyword(double lat, double lon, String term);
}

