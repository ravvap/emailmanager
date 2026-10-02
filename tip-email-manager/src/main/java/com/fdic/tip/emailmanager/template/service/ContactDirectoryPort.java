package com.fdic.tip.emailmanager.template.service;

import java.util.Collection;
import java.util.List;

/** Boundary into the existing contacts directory endpoint (same one the Recipients Mode screen lists from). */
public interface ContactDirectoryPort {

    /** Returns the subset of the given ids that are NOT active contacts. Empty means every id is valid. */
    List<Long> findInvalidIds(Collection<Long> contactIds);
}
