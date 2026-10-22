package com.turbo.mobile.data.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum OwnerTab {
    UPCOMING(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS),
    HISTORY(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.REJECTED);

    private final List<BookingStatus> statuses;

    OwnerTab(BookingStatus... statuses) {
        this.statuses = Collections.unmodifiableList(Arrays.asList(statuses));
    }

    public List<BookingStatus> getStatuses() {
        return statuses;
    }
}
