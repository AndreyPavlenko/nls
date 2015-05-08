package com.aap.nls.eclipse.parser;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * @author Andrey Pavlenko
 */
public class NonNlsTag extends SourceFragment {
    private final Line line;
    private final int offset;
    private final int length;
    private final int messageIndex;

    NonNlsTag(final Line line, final int offset, final int length,
            final int messageIndex) {
        this.line = line;
        this.offset = offset;
        this.length = length;
        this.messageIndex = messageIndex;
        line.add(this);
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

    public int getMessageIndex() {
        return messageIndex;
    }

    public TextEdit createDeleteEdit() {
        return new DeleteEdit(getOffset(), getLength());
    }

    @Override
    public String toString() {
        return "//$NON-NLS-" + messageIndex + "$"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
