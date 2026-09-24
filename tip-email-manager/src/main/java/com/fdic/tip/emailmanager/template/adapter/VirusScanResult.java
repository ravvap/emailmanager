package com.fdic.tip.emailmanager.template.adapter;

/** Outcome of a virus scan on a single file's bytes. */
public record VirusScanResult(Verdict verdict, String detail) {

    public enum Verdict {
        CLEAN,
        INFECTED,
        FAILED
    }

    public boolean isClean() {
        return verdict == Verdict.CLEAN;
    }
}
