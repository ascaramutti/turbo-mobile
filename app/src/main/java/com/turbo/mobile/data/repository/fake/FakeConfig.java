package com.turbo.mobile.data.repository.fake;

import com.turbo.mobile.data.model.RepoError;

import java.util.Set;

public final class FakeConfig {

    private static RepoError forced;

    private FakeConfig() {
    }

    public static void forceNextError(RepoError code) {
        forced = code;
    }

    public static void clear() {
        forced = null;
    }

    public static RepoError take(Set<? extends RepoError> possible) {
        if (forced != null && possible.contains(forced)) {
            RepoError code = forced;
            forced = null;
            return code;
        }
        return null;
    }
}
