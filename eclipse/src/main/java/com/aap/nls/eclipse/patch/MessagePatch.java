package com.aap.nls.eclipse.patch;

import com.aap.nls.eclipse.parser.Message;

/**
 * @author Andrey Pavlenko
 */
public class MessagePatch {
    private final SourcePatch sourcePatch;
    private final Message message;
    private String key;
    private String value;
    private Action action = Action.EXTERNALIZE;

    MessagePatch(final SourcePatch sourcePatch, final Message message) {
        this.sourcePatch = sourcePatch;
        this.message = message;
        value = message.getMessageFormat();
        key = sourcePatch.getNlsBundlePatch().proposeKey(value);
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public Message getMessage() {
        return message;
    }

    public SourcePatch getSourcePatch() {
        return sourcePatch;
    }

    public static enum Action {
        IGNORE,
        EXTERNALIZE,
        NON_NLS;
    }
}
