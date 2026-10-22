package com.turbo.mobile.data.model;

import java.util.List;

public class ConfirmResult {

    private List<Long> autoRejectedIds;

    public List<Long> getAutoRejectedIds() {
        return autoRejectedIds;
    }

    public void setAutoRejectedIds(List<Long> autoRejectedIds) {
        this.autoRejectedIds = autoRejectedIds;
    }
}
