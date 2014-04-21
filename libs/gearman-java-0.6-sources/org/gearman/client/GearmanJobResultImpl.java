/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import java.util.Arrays;

public class GearmanJobResultImpl implements GearmanJobResult {

    private long denominator;
    private long numerator;
    private byte[] results;
    private byte[] warnings;
    private byte[] exceptions;
    private byte[] handle;
    boolean succeeded = false;

    private GearmanJobResultImpl() {
    }

    public GearmanJobResultImpl(byte[] handle) {
        this(handle, false, null, null, null, -1, -1);
    }

    public GearmanJobResultImpl(byte[] handle, boolean succeeded,
            byte[] results, byte[] warnings, byte[] exceptions, long numerator,
            long denominator) {
        this.handle = copyArray(handle);
        this.succeeded = succeeded;
        this.numerator = numerator;
        this.denominator = denominator;
        this.results = copyArray(results);
        this.warnings = copyArray(warnings);
        this.exceptions = copyArray(exceptions);
    }

    public GearmanJobResultImpl addJobResult(GearmanJobResultImpl that) {
        if (!Arrays.equals(handle, that.handle)) {
            throw new IllegalArgumentException("Only results for the same job" +
                    " can be merged");
        }
        byte[] mergedResults = new byte[results.length + that.results.length];
        System.arraycopy(results, 0, mergedResults, 0, results.length);
        System.arraycopy(that.results, 0, mergedResults, results.length,
                that.results.length);

        byte[] mergedWarnings = new byte[warnings.length + that.warnings.length];
        System.arraycopy(warnings, 0, mergedWarnings, 0, warnings.length);
        System.arraycopy(that.warnings, 0, mergedWarnings, warnings.length,
                that.warnings.length);

        byte[] mergedExceptions = new byte[exceptions.length + that.exceptions.length];
        System.arraycopy(exceptions, 0, mergedExceptions, 0, exceptions.length);
        System.arraycopy(that.exceptions, 0, mergedExceptions,
                exceptions.length, that.exceptions.length);

        long newNum = that.numerator == -1 ? numerator : that.numerator;
        long newDen = that.denominator == -1 ? denominator : that.denominator;

        return new GearmanJobResultImpl(handle, that.succeeded, mergedResults,
                mergedWarnings, mergedExceptions, newNum, newDen);
    }

    public byte[] getResults() {
        return copyArray(results);
    }

    public byte[] getWarnings() {
        return copyArray(warnings);
    }

    public byte[] getExceptions() {
        return copyArray(exceptions);
    }

    public long getDenominator() {
        return denominator;
    }

    public long getNumerator() {
        return numerator;
    }

    public byte[] getJobHandle() {
        return copyArray(handle);
    }

    public boolean jobSucceeded() {
        return succeeded;
    }

    public GearmanJobResult copy() {
        GearmanJobResultImpl res = new GearmanJobResultImpl(handle, succeeded,
                copyArray(results), copyArray(warnings), copyArray(exceptions),
                numerator, denominator);
        return res;
    }

    GearmanJobResultImpl addResults(byte[] newResults) {
        byte[] mergedResults = new byte[results.length + newResults.length];
        System.arraycopy(results, 0, mergedResults, 0, results.length);
        System.arraycopy(newResults, 0, mergedResults, results.length,
                newResults.length);
        return new GearmanJobResultImpl(handle, succeeded, mergedResults,
                warnings, exceptions, numerator, denominator);
    }

    GearmanJobResultImpl addWarnings(byte[] newWarnings) {
        byte[] mergedWarnings = new byte[warnings.length + newWarnings.length];
        System.arraycopy(warnings, 0, mergedWarnings, 0, warnings.length);
        System.arraycopy(newWarnings, 0, mergedWarnings, warnings.length,
                newWarnings.length);
        return new GearmanJobResultImpl(handle, succeeded, results,
                mergedWarnings, exceptions, numerator, denominator);
    }

    GearmanJobResultImpl addExceptions(byte[] newExceptions) {
        byte[] mergedExceptions = new byte[exceptions.length + newExceptions.length];
        System.arraycopy(exceptions, 0, mergedExceptions, 0, exceptions.length);
        System.arraycopy(newExceptions, 0, mergedExceptions, exceptions.length,
                newExceptions.length);
        return new GearmanJobResultImpl(handle, succeeded, results, warnings,
                mergedExceptions, numerator, denominator);
    }

    private byte[] copyArray(byte[] src) {
        if (src == null) {
            return new byte[0];
        }
        byte[] copy = new byte[src.length];
        System.arraycopy(src, 0, copy, 0, src.length);
        return copy;
    }
}
