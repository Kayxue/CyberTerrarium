package process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminationResult {
    private final int requestedCount;
    private final int terminatedCount;
    private final List<Long> failedPids;

    TerminationResult(int requestedCount, int terminatedCount, List<Long> failedPids) {
        this.requestedCount = requestedCount;
        this.terminatedCount = terminatedCount;
        this.failedPids = Collections.unmodifiableList(new ArrayList<>(failedPids));
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getTerminatedCount() {
        return terminatedCount;
    }

    public List<Long> getFailedPids() {
        return failedPids;
    }

    public boolean isSuccess() {
        return failedPids.isEmpty();
    }
}
