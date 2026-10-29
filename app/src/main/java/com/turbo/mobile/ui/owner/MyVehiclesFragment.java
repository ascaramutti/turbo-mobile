package com.turbo.mobile.ui.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;

import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;

/** W-14 My vehicles. */
public class MyVehiclesFragment extends Fragment {

    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private AppContainer container;
    private long ownerId;
    private LinearLayout vehicleList;
    private View emptyState;

    public MyVehiclesFragment() {
        super(R.layout.fragment_my_vehicles);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        vehicleList = view.findViewById(R.id.vehicle_list);
        emptyState = view.findViewById(R.id.empty_state);
        view.findViewById(R.id.add_vehicle).setOnClickListener(v -> comingSoon());
    }

    @Override
    public void onResume() {
        super.onResume();
        changeAndReload(null);
    }

    // Runs the optional change on the data thread, then reloads the list.
    private void changeAndReload(@Nullable Supplier<Result<?>> change) {
        container.executor.execute(() -> {
            Result<?> result = change == null ? null : change.get();
            List<OwnerVehicleItem> vehicles = container.vehicleRepository.getOwnerVehicles(ownerId);
            mainThread.post(() -> {
                if (getView() == null) {
                    return;
                }
                if (result instanceof Result.Error) {
                    Toast.makeText(requireContext(), ((Result.Error<?>) result).message, Toast.LENGTH_LONG).show();
                }
                render(vehicles);
            });
        });
    }

    private void render(List<OwnerVehicleItem> vehicles) {
        vehicleList.removeAllViews();
        for (OwnerVehicleItem item : vehicles) {
            View card = getLayoutInflater().inflate(R.layout.item_owner_vehicle, vehicleList, false);
            bind(card, item);
            vehicleList.addView(card);
        }
        emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bind(View card, OwnerVehicleItem item) {
        ((TextView) card.findViewById(R.id.vehicle_title)).setText(item.getTitle());
        TextView subtitle = card.findViewById(R.id.vehicle_subtitle);
        TextView primary = card.findViewById(R.id.chip_primary);
        TextView secondary = card.findViewById(R.id.chip_secondary);
        SpannableStringBuilder actions = new SpannableStringBuilder();
        addAction(actions, R.string.action_edit, this::comingSoon);

        switch (item.getBadge()) {
            case PUBLISHED:
                subtitle.setText(R.string.vehicle_approved_active);
                chip(primary, R.string.chip_approved, R.color.turbo_success_bg, R.color.turbo_success);
                chip(secondary, R.string.chip_active, R.color.turbo_success_bg, R.color.turbo_success);
                addAction(actions, R.string.action_deactivate,
                        () -> changeAndReload(() -> container.vehicleRepository.unpublish(ownerId, item.getId())));
                break;
            case NOT_PUBLISHED:
                subtitle.setText(R.string.vehicle_hidden_by_owner);
                chip(primary, R.string.chip_approved, R.color.turbo_success_bg, R.color.turbo_success);
                chip(secondary, R.string.chip_inactive, R.color.turbo_success_bg, R.color.turbo_success);
                addAction(actions, R.string.action_activate, () -> pickAvailableUntil(item));
                break;
            case PENDING_REVIEW:
                int pending = item.getPendingDocuments();
                subtitle.setText(getResources().getQuantityString(R.plurals.vehicle_documents_pending, pending, pending));
                chip(primary, R.string.chip_pending_review, R.color.turbo_warning_bg, R.color.turbo_warning);
                secondary.setVisibility(View.GONE);
                break;
            case ACTION_NEEDED:
                int document = item.getRejectedDocument() == DocumentType.REGISTRATION
                        ? R.string.document_registration : R.string.document_insurance;
                subtitle.setText(getString(R.string.vehicle_document_rejected, getString(document)));
                chip(primary, R.string.chip_action_needed, R.color.turbo_error_bg, R.color.turbo_error);
                chip(secondary, R.string.chip_not_visible, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                break;
        }

        addAction(actions, R.string.action_documents, this::comingSoon);
        addAction(actions, R.string.action_delete, () -> confirmDelete(item));
        TextView actionsView = card.findViewById(R.id.vehicle_actions);
        actionsView.setText(actions);
        actionsView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void chip(TextView chip, int label, int background, int textColor) {
        chip.setText(label);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), background));
        chip.setTextColor(ContextCompat.getColor(requireContext(), textColor));
    }

    // "Edit · Deactivate · Documents · Delete": one TextView, each word clickable.
    private void addAction(SpannableStringBuilder actions, int label, Runnable onClick) {
        if (actions.length() > 0) {
            actions.append(" · ");
        }
        int start = actions.length();
        actions.append(getString(label));
        actions.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // LinkMovementMethod leaves the tapped word selected (highlighted); clear it.
                Selection.removeSelection((Spannable) ((TextView) widget).getText());
                onClick.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint paint) {
                // Keep the TextView color, no underline.
            }
        }, start, actions.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // publish() needs an "available until" date and W-14 has no field for it, so ask with the native picker.
    private void pickAvailableUntil(OwnerVehicleItem item) {
        Calendar today = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            Calendar until = Calendar.getInstance();
            until.set(year, month, day, 23, 59, 0);
            changeAndReload(() -> container.vehicleRepository.publish(ownerId, item.getId(), until.getTimeInMillis()));
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(today.getTimeInMillis());
        picker.show();
    }

    private void confirmDelete(OwnerVehicleItem item) {
        AppCompatDialog dialog = new AppCompatDialog(requireContext());
        dialog.setContentView(R.layout.dialog_confirm);
        ((TextView) dialog.findViewById(R.id.dialog_title)).setText(R.string.delete_vehicle_title);
        String name = item.getTitle().replaceFirst(" \\d{4}$", ""); // "Toyota Corolla 2022" -> "Toyota Corolla", as in W-14
        ((TextView) dialog.findViewById(R.id.dialog_message)).setText(getString(R.string.delete_vehicle_body, name));
        TextView confirm = dialog.findViewById(R.id.dialog_confirm);
        confirm.setText(R.string.delete_vehicle);
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            changeAndReload(() -> container.vehicleRepository.deleteVehicle(ownerId, item.getId()));
        });
        dialog.findViewById(R.id.dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.52f); // ≈ the wireframe's 60% ink scrim
    }

    private void comingSoon() {
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }
}
