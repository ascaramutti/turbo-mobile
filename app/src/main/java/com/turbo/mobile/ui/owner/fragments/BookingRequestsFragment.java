package com.turbo.mobile.ui.owner.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentBookingRequestsBinding;
import com.turbo.mobile.ui.driver.BookingDetailActivity;
import com.turbo.mobile.ui.owner.adapters.OwnerBookingAdapter;
import com.turbo.mobile.ui.owner.viewmodels.OwnerBookingViewModel;

import java.util.List;

public class BookingRequestsFragment extends Fragment {

    private FragmentBookingRequestsBinding binding;
    private OwnerBookingViewModel viewModel;
    private OwnerBookingAdapter adapter;
    private boolean showingRequests = true;
    private OwnerTab tab = OwnerTab.UPCOMING;

    public BookingRequestsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookingRequestsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new OwnerBookingViewModel(appContainer.bookingRepository, appContainer.sessionManager);

        adapter = new OwnerBookingAdapter(appContainer.imageStorage, this);
        binding.recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBookings.setAdapter(adapter);

        binding.tabRequests.setOnClickListener(view -> selectRequests());
        binding.tabUpcoming.setOnClickListener(view -> selectTab(OwnerTab.UPCOMING));
        binding.tabHistory.setOnClickListener(view -> selectTab(OwnerTab.HISTORY));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void openBooking(BookingListItem booking) {
        Intent intent = new Intent(requireContext(), BookingDetailActivity.class);
        intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
        startActivity(intent);
    }

    public void confirm(BookingListItem booking) {
        hideBanners();
        Result<ConfirmResult> result = viewModel.confirm(booking.getId());
        if (result instanceof Result.Success) {
            int rejected = ((Result.Success<ConfirmResult>) result).data.getAutoRejectedIds().size();
            if (rejected == 0) {
                binding.successBanner.setText(R.string.booking_requests_confirmed);
            } else {
                binding.successBanner.setText(getResources().getQuantityString(R.plurals.booking_requests_confirmed_overlap, rejected, rejected));
            }
            binding.successBanner.setVisibility(View.VISIBLE);
        } else {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<ConfirmResult>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
        }
        loadBookings();
    }

    public void openRejectDialog(BookingListItem booking) {
        hideBanners();
        Bundle arguments = new Bundle();
        arguments.putLong(RejectBookingDialogFragment.ARG_BOOKING_ID, booking.getId());
        RejectBookingDialogFragment dialog = new RejectBookingDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getChildFragmentManager(), "reject_booking");
    }

    public void bookingRejected() {
        binding.successBanner.setText(R.string.booking_requests_rejected);
        binding.successBanner.setVisibility(View.VISIBLE);
        loadBookings();
    }

    private void selectRequests() {
        showingRequests = true;
        hideBanners();
        loadBookings();
    }

    private void selectTab(OwnerTab selected) {
        showingRequests = false;
        tab = selected;
        hideBanners();
        loadBookings();
    }

    private void loadBookings() {
        List<BookingListItem> requests = viewModel.getPendingRequests();
        binding.tabRequests.setText(getString(R.string.booking_requests_tab_requests, requests.size()));
        binding.tabRequests.setSelected(showingRequests);
        binding.tabUpcoming.setSelected(!showingRequests && tab == OwnerTab.UPCOMING);
        binding.tabHistory.setSelected(!showingRequests && tab == OwnerTab.HISTORY);

        List<BookingListItem> bookings = requests;
        if (!showingRequests) {
            bookings = viewModel.getBookings(tab);
        }
        adapter.setItems(bookings);
        binding.emptyState.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void hideBanners() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);
    }
}
