package com.turbo.mobile.ui.driver;

import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.data.repository.VehicleRepository;

import java.util.List;

public class SearchViewModel {

    private final VehicleRepository vehicleRepository;

    public SearchViewModel(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Zone> getZones() {
        return vehicleRepository.getZones();
    }

    public List<VehicleSummary> search(SearchFilter filter) {
        return vehicleRepository.searchPublished(filter);
    }
}
