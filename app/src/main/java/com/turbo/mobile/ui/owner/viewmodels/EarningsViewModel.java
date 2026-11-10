package com.turbo.mobile.ui.owner.viewmodels;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class EarningsViewModel {

    private final PaymentRepository paymentRepository;
    private final VehicleRepository vehicleRepository;
    private final SessionManager sessionManager;

    public EarningsViewModel(PaymentRepository paymentRepository, VehicleRepository vehicleRepository, SessionManager sessionManager) {
        this.paymentRepository = paymentRepository;
        this.vehicleRepository = vehicleRepository;
        this.sessionManager = sessionManager;
    }

    public List<OwnerVehicleItem> getVehicles() {
        return vehicleRepository.getOwnerVehicles(sessionManager.getUserId());
    }

    public Result<OwnerEarnings> getEarnings(Long vehicleId) {
        return paymentRepository.getOwnerEarnings(sessionManager.getUserId(), vehicleId);
    }

    public int getErrorMessage(RepoError code) {
        if (code == PaymentError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == PaymentError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
