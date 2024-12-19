package com.eventrecommender.external;

import java.util.List;
import com.eventrecommender.entity.Item;

public interface ExternalAPI {

	public List<Item> search(double lat, double lon, String term);
}

