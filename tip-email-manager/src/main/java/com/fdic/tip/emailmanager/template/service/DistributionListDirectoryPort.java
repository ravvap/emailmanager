package com.fdic.tip.emailmanager.template.service;

import java.util.Collection;
import java.util.List;

/** Boundary into the existing distribution-list directory endpoint (same one the Recipients Mode screen lists from). */
public interface DistributionListDirectoryPort {

    /** Returns the subset of the given ids that are NOT active distribution lists. Empty means every id is valid. */
    List<Long> findInvalidIds(Collection<Long> distributionListIds);
}
