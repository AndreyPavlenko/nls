package com.aap.nls.eclipse.parser;

/**
 * @author Andrey Pavlenko
 */
public abstract class SourceFragment implements Comparable<SourceFragment> {

    public abstract int getOffset();

    public abstract int getLength();

    @Override
    public int compareTo(final SourceFragment o) {
        final int off1 = getOffset();
        final int off2 = o.getOffset();
        return (off1 > off2) ? 1 : (off1 < off2) ? -1 : 0;
    }
}
