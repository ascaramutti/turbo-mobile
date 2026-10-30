package com.turbo.mobile.ui.owner;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.EarningItem;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

/** W-19 Earnings: totals and paid shifts, for all the owner's vehicles or just one. */
public class EarningsFragment extends Fragment {

    private AppContainer container;
    private long ownerId;
    private Long vehicleId = null; // vehicle chosen in the filter; null = all vehicles
    private List<OwnerVehicleItem> vehicles = new ArrayList<>();

    private TextView vehicleFilter;
    private TextView totalEarned;
    private TextView completedShifts;
    private TextView thisMonth;
    private LinearLayout earningList;
    private View emptyText;

    public EarningsFragment() {
        super(R.layout.fragment_earnings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        vehicleFilter = view.findViewById(R.id.vehicle_filter);
        totalEarned = view.findViewById(R.id.total_earned);
        completedShifts = view.findViewById(R.id.completed_shifts);
        thisMonth = view.findViewById(R.id.this_month);
        earningList = view.findViewById(R.id.earning_list);
        emptyText = view.findViewById(R.id.earnings_empty);
        vehicleFilter.setOnClickListener(v -> chooseVehicle());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEarnings();
    }

    // Reads the earnings and also the owner's vehicles (the options of the filter).
    private void loadEarnings() {
        Long filter = vehicleId;
        container.executor.execute(() -> {
            List<OwnerVehicleItem> ownerVehicles = container.vehicleRepository.getOwnerVehicles(ownerId);
            Result<OwnerEarnings> result = container.paymentRepository.getOwnerEarnings(ownerId, filter);
            TurboUi.runOnMainThread(this, () -> {
                vehicles = ownerVehicles;
                if (result instanceof Result.Success) {
                    showEarnings(((Result.Success<OwnerEarnings>) result).data);
                } else {
                    TurboUi.showError(requireContext(), result);
                }
            });
        });
    }

    private void showEarnings(OwnerEarnings earnings) {
        totalEarned.setText(TurboUi.moneyWithoutCents(earnings.getTotalCents()));
        completedShifts.setText(String.valueOf(earnings.getCompletedRentals()));
        thisMonth.setText(TurboUi.moneyWithoutCents(earnings.getMonthCents()));

        earningList.removeAllViews();
        for (EarningItem earning : earnings.getHistory()) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, earningList, false);
            TextView title = card.findViewById(R.id.card_title);
            TextView subtitle = card.findViewById(R.id.card_subtitle);
            TextView chip = card.findViewById(R.id.chip_primary);
            TextView footer = card.findViewById(R.id.card_footer);

            // "Toyota Corolla · Ana L.", "Oct 2 · 4 h", "Paid", "+ CAD 40.00"
            String vehicleAndDriver = TurboUi.withoutYear(earning.getVehicleTitle()) + " · " + earning.getDriverName();
            String when = TurboUi.date(earning.getPaidAt()) + " · " + earning.getHours() + " h";
            title.setText(vehicleAndDriver);
            subtitle.setText(when);
            TurboUi.paintChip(chip, getString(R.string.chip_paid), R.color.turbo_success_bg, R.color.turbo_success);
            footer.setText(getString(R.string.payout, TurboUi.money(earning.getPayoutCents())));
            earningList.addView(card);
        }
        emptyText.setVisibility(earnings.getHistory().isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Shows a list: "All vehicles" plus one row per vehicle. Choosing one reloads the earnings.
    private void chooseVehicle() {
        String[] options = new String[vehicles.size() + 1];
        options[0] = getString(R.string.all_vehicles);
        for (int i = 0; i < vehicles.size(); i++) {
            options[i + 1] = vehicles.get(i).getTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    // which = the position tapped. 0 is "All vehicles", so the vehicles start at 1.
                    if (which == 0) {
                        vehicleId = null;
                    } else {
                        vehicleId = vehicles.get(which - 1).getId();
                    }
                    vehicleFilter.setText(options[which]);
                    loadEarnings();
                })
                .show();
    }
}
