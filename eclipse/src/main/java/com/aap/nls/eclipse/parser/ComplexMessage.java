package com.aap.nls.eclipse.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * @author Andrey Pavlenko
 */
public class ComplexMessage extends Message {
    private final Collection<Line> lines;
    private final List<SimpleMessage> simpleMessages;
    private boolean isInternalized = true;
    private boolean hasNonNlsTags = false;
    private String messageFormat;
    private String arguments;

    ComplexMessage(final InfixExpression expr, final Collection<Line> lines) {
        super(expr);
        this.lines = lines;
        simpleMessages = new ArrayList<SimpleMessage>();
    }

    @Override
    public int getOffset() {
        return getExpression().getStartPosition();
    }

    @Override
    public int getLength() {
        return getExpression().getLength();
    }

    @Override
    public boolean isInternalized() {
        return isInternalized;
    }

    @Override
    public TextEdit createInternalizeEdit() {
        final MultiTextEdit e = new MultiTextEdit();

        for (final SimpleMessage m : getSimpleMessages()) {
            if (!m.isInternalized()) {
                e.addChild(m.createInternalizeEdit());
            }
        }

        return e;
    }

    @Override
    public TextEdit createExternalizeEdit(final String key) {
        final TextEdit edit;

        if (arguments.length() == 0) {
            edit = new ReplaceEdit(getOffset(), getLength(), key
                    + ".toString()"); //$NON-NLS-1$
        } else {
            edit = new ReplaceEdit(getOffset(), getLength(), key + ".format(" //$NON-NLS-1$
                    + getArguments() + ")"); //$NON-NLS-1$
        }

        if (hasNonNlsTags && !simpleMessages.isEmpty()) {
            final MultiTextEdit me = new MultiTextEdit();
            final int endOff = edit.getOffset() + edit.getLength();

            for (final SimpleMessage m : getSimpleMessages()) {
                final NonNlsTag t = m.getNonNlsTag();

                if ((t != null) && (t.getOffset() >= endOff)) {
                    me.addChild(t.createDeleteEdit());
                }
            }

            if (me.getChildrenSize() > 0) {
                me.addChild(edit);
                return me;
            }
        }

        return edit;
    }

    public Collection<Line> getLines() {
        return lines;
    }

    public Collection<SimpleMessage> getSimpleMessages() {
        return simpleMessages;
    }

    @Override
    public String getMessageFormat() {
        return messageFormat;
    }

    @Override
    public String getArguments() {
        return arguments;
    }

    void addSimpleMessage(final SimpleMessage msg) {
        simpleMessages.add(msg);
    }

    void setHasNonNlsTags(final boolean hasNonNlsTags) {
        this.hasNonNlsTags = hasNonNlsTags;
    }

    void process(final MessageFinder finder, final StringBuilder msg,
            final StringBuilder args) {
        Expression expr;
        final AtomicInteger argCounter = new AtomicInteger();
        final InfixExpression node = (InfixExpression) getExpression();

        msg.setLength(0);
        args.setLength(0);

        if ((expr = node.getLeftOperand()) instanceof StringLiteral) {
            processStringLiteral(finder, msg, args, (StringLiteral) expr,
                    argCounter);
        } else {
            processExpr(finder, msg, args, expr, argCounter);
        }

        if ((expr = node.getRightOperand()) instanceof StringLiteral) {
            processStringLiteral(finder, msg, args, (StringLiteral) expr,
                    argCounter);
        } else {
            processExpr(finder, msg, args, expr, argCounter);
        }

        for (final Object o : node.extendedOperands()) {
            if (o instanceof StringLiteral) {
                processStringLiteral(finder, msg, args, (StringLiteral) o,
                        argCounter);
            } else if (o instanceof Expression) {
                processExpr(finder, msg, args, (Expression) o, argCounter);
            }
        }

        if (args.length() >= 2) {
            args.setLength(args.length() - 2);
        }

        messageFormat = msg.toString();
        arguments = args.toString();
    }

    private void processStringLiteral(final MessageFinder finder,
            final StringBuilder msg, final StringBuilder args,
            final StringLiteral node, final AtomicInteger argCounter) {
        final int off = node.getStartPosition();
        final SimpleMessage m = getSimpleMessage(off);

        if (m != null) {
            msg.append(m.getMessageFormat());

            if (!m.isInternalized()) {
                isInternalized = false;
            }
        } else {
            msg.append('{');
            msg.append(argCounter.getAndIncrement());
            msg.append('}');
            args.append(finder.getText(off, node.getLength()));
            args.append(", "); //$NON-NLS-1$
        }
    }

    private void processExpr(final MessageFinder finder,
            final StringBuilder msg, final StringBuilder args,
            final Expression node, final AtomicInteger argCounter) {
        if (node instanceof InfixExpression) {
            final InfixExpression expr = (InfixExpression) node;

            if (isSimple(expr)) {
                processStringLiteral(finder, msg, args,
                        (StringLiteral) expr.getLeftOperand(), argCounter);
                processStringLiteral(finder, msg, args,
                        (StringLiteral) expr.getRightOperand(), argCounter);

                for (final Object o : expr.extendedOperands()) {
                    processStringLiteral(finder, msg, args, (StringLiteral) o,
                            argCounter);
                }
            }
        } else {
            msg.append('{');
            msg.append(argCounter.getAndIncrement());
            msg.append('}');
            args.append(finder.getText(node.getStartPosition(),
                    node.getLength()));
            args.append(", "); //$NON-NLS-1$
        }
    }

    private static boolean isSimple(final InfixExpression expr) {
        if (!(expr.getLeftOperand() instanceof StringLiteral)
                || !(expr.getRightOperand() instanceof StringLiteral)) {
            return false;
        }

        for (final Object o : expr.extendedOperands()) {
            if (!(o instanceof StringLiteral)) {
                return false;
            }
        }

        return true;
    }

    private SimpleMessage getSimpleMessage(final int offset) {
        for (final Line l : lines) {
            final SimpleMessage m = l.getSimpleMessage(offset);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getExpression().toString();
    }
}
