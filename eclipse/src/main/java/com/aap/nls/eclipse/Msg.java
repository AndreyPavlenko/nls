package com.aap.nls.eclipse;

import java.text.MessageFormat;

//TODO: add support for nls bundles
@SuppressWarnings("nls")
public enum Msg {
    Parsing("Parsing {0}"),
    LookingForNonNls("Looking for not externalized strings"),
    ApplyPatches("Applying changes"),
    ApplyPatchesTo("Applying changes to {0}"),
    CustomizeMessages("Customize messages"),
    Key("Key"),
    Value("Value"),
    Ignore("Ignore"),
    NonNls("Non-nls"),
    Externalize("Externalize"),
    InvalidKeyValue("Invalid key value: {0}"),
    InternationalizeSources("Internationalize sources"),
    SelectNlsBundleClass("Select NLS Bundle class"),
    Browse("Browse"),
    SelectSourcesToInternationalize("Select sources to be internationalized"),
    NlsBundleDesc("<b>NLS Bundle class is a Java Emun which:</b><ul>"
            + "<li>Has constructor with a single argument of type <b><i>java.lang.String</i></b></li>"
            + "<li>The method <b><i>public String toString()</i></b> returns the value of the corresponding key</li>"
            + "<li>The method <b><i>public String format(Object... args)</i></b> returns the formatted value of the corresponding key</li></ul>");

    private final String value;

    private Msg(final String defaultValue) {
        value = String.valueOf(defaultValue);
    }

    public String format(final Object... args) {
        return new MessageFormat(value).format(args);
    }

    @Override
    public String toString() {
        return value;
    }
}
