package org.evrete.spi.minimal;

import org.evrete.runtime.builder.FieldReference;
import org.evrete.util.NextIntSupplier;

class ConditionStringTerm extends FieldReferenceImpl {
    final int start;
    final int end;
    final String varName;

    ConditionStringTerm(int start, int end, FieldReference delegate, NextIntSupplier fieldCounter) {
        super(delegate);
        this.start = start;
        this.end = end;
        this.varName = "var" + fieldCounter.next();
    }
}