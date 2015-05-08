package com.aap.nls.eclipse.parser;

import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * @author Andrey Pavlenko
 */
public class SimpleMessage extends Message {
    private final ComplexMessage complexMessage;
    private final Line line;
    private final int offset;
    private final int length;
    private final String quotedText;
    private String format;
    private int index;
    private NonNlsTag nonNlsTag;

    SimpleMessage(final StringLiteral node,
            final ComplexMessage complexMessage, final Line line,
            final int offset, final int length, final String quotedText) {
        super(node);
        this.complexMessage = complexMessage;
        this.line = line;
        this.offset = offset;
        this.length = length;
        this.quotedText = quotedText;
        line.add(this);

        if (complexMessage != null) {
            complexMessage.addSimpleMessage(this);
        }
    }

    public Line getLine() {
        return line;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public boolean isInternalized() {
        return getNonNlsTag() != null;
    }

    @Override
    public TextEdit createInternalizeEdit() {
        return new InsertEdit(getLine().getEndOffset(), " //$NON-NLS-" //$NON-NLS-1$
                + getIndex() + '$');
    }

    @Override
    public TextEdit createExternalizeEdit(final String key) {
        return new ReplaceEdit(getOffset(), getLength(), key + ".toString()"); //$NON-NLS-1$
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getMessageFormat() {
        return format == null ? format = quotedText.substring(1,
                quotedText.length() - 1) : format;
    }

    @Override
    public String getArguments() {
        return ""; //$NON-NLS-1$
    }

    public String getQuotedText() {
        return quotedText;
    }

    public NonNlsTag getNonNlsTag() {
        return nonNlsTag;
    }

    public ComplexMessage getComplexMessage() {
        return complexMessage;
    }

    @Override
    public String toString() {
        return getQuotedText();
    }

    void setIndex(final int index) {
        this.index = index;
    }

    void setNonNlsTag(final NonNlsTag t) {
        if (line != t.getLine()) {
            throw new IllegalArgumentException();
        }
        if (index != t.getMessageIndex()) {
            throw new IllegalArgumentException();
        }
        if (complexMessage != null) {
            complexMessage.setHasNonNlsTags(true);
        }
        nonNlsTag = t;
    }
}
