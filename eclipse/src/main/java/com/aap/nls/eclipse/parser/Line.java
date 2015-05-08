package com.aap.nls.eclipse.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Andrey Pavlenko
 */
public class Line {
    private final int number;
    private final int endOffset;
    private final Collection<SimpleMessage> simpleMessages = new ArrayList<SimpleMessage>();
    private Collection<NonNlsTag> nonNlsTags;

    Line(final int number, final int endOffset) {
        this.number = number;
        this.endOffset = endOffset;
    }

    public int getNumber() {
        return number;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public Collection<SimpleMessage> getSimpleMessages() {
        return simpleMessages;
    }

    public SimpleMessage getSimpleMessage(final int offset) {
        for (final SimpleMessage m : getSimpleMessages()) {
            if (m.getOffset() == offset) {
                return m;
            }
        }
        return null;
    }

    public Collection<NonNlsTag> getNonNlsTags() {
        return nonNlsTags == null ? Collections.<NonNlsTag> emptyList()
                : nonNlsTags;
    }

    @Override
    public String toString() {
        return "Line [\r\n    Number: " + getNumber() //$NON-NLS-1$
                + "\r\n    SimpleMessages: " + getSimpleMessages() //$NON-NLS-1$
                + "\r\n    NonNlsTags: " + getNonNlsTags() + "\r\n]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    void add(final SimpleMessage msg) {
        if (msg.getLine() != this) {
            throw new IllegalArgumentException();
        }

        simpleMessages.add(msg);
        msg.setIndex(simpleMessages.size());
    }

    void add(final NonNlsTag t) {
        if (t.getLine() != this) {
            throw new IllegalArgumentException();
        }

        if (nonNlsTags == null) {
            nonNlsTags = new ArrayList<NonNlsTag>();
        }

        nonNlsTags.add(t);
        final int ind = t.getMessageIndex();

        for (final SimpleMessage m : simpleMessages) {
            if (ind == m.getIndex()) {
                m.setNonNlsTag(t);
                break;
            }
        }
    }

    void remove(final SimpleMessage msg) {
        simpleMessages.remove(msg);
    }
}
