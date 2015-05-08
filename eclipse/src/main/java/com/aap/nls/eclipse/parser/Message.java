package com.aap.nls.eclipse.parser;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.text.edits.TextEdit;

/**
 * @author Andrey Pavlenko
 */
public abstract class Message extends SourceFragment {

    private final Expression expression;

    public Message(final Expression expression) {
        this.expression = expression;
    }

    public abstract boolean isInternalized();

    public abstract String getMessageFormat();

    public abstract String getArguments();

    public abstract TextEdit createInternalizeEdit();

    public abstract TextEdit createExternalizeEdit(String key);

    public Expression getExpression() {
        return expression;
    }
}
