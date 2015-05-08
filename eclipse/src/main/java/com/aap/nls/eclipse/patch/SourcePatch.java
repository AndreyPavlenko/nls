package com.aap.nls.eclipse.patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.aap.nls.eclipse.NlsPlugin;
import com.aap.nls.eclipse.parser.Message;
import com.aap.nls.eclipse.parser.MessageFinder;

/**
 * @author Andrey Pavlenko
 */
public class SourcePatch extends CompilationUnitPatch {
    private final MessageFinder finder;
    private NlsBundlePatch nlsBundlePatch;
    private Collection<MessagePatch> messagePatches;
    private boolean enabled = true;

    public SourcePatch(final MessageFinder finder) {
        this.finder = finder;
    }

    public MessageFinder getMessageFinder() {
        return finder;
    }

    public NlsBundlePatch getNlsBundlePatch() {
        return nlsBundlePatch;
    }

    public void setNlsBundlePatch(final NlsBundlePatch nlsBundlePatch) {
        this.nlsBundlePatch = nlsBundlePatch;
        messagePatches = null;
    }

    public Collection<MessagePatch> getMessagePatches() {
        if (!isEnabled() || (getNlsBundlePatch() == null)) {
            return Collections.emptyList();
        }

        if (messagePatches == null) {
            final Collection<Message> messages = getMessageFinder()
                    .getMessages();
            messagePatches = new ArrayList<MessagePatch>(messages.size());

            for (final Message msg : messages) {
                messagePatches.add(new MessagePatch(this, msg));
            }
        }

        return messagePatches;
    }

    public boolean isEnabled() {
        if ((nlsBundlePatch != null)
                && getMessageFinder().getCompilationUnit().getPath()
                        .equals(nlsBundlePatch.getCompilationUnit().getPath())) {
            return false;
        }
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (!enabled) {
            messagePatches = null;
        }
        this.enabled = enabled;
    }

    @Override
    public ICompilationUnit getCompilationUnit() {
        return getMessageFinder().getCompilationUnit();
    }

    @Override
    public TextEdit createTextEdit() {
        if (isEnabled()) {
            final NlsBundlePatch bp = getNlsBundlePatch();

            if (bp != null) {
                final MessageFinder f = getMessageFinder();
                final MultiTextEdit me = new MultiTextEdit();
                boolean addImport = false;
                TextEdit e = null;

                for (final MessagePatch mp : getMessagePatches()) {
                    try {
                        switch (mp.getAction()) {
                        case NON_NLS:
                            e = mp.getMessage().createInternalizeEdit();
                            me.addChild(e);
                            break;
                        case EXTERNALIZE:
                            final String k = mp.getKey();

                            addImport = true;
                            e = mp.getMessage().createExternalizeEdit(
                                    bp.getSimpleName() + "." + k); //$NON-NLS-1$
                            me.addChild(e);
                            bp.addMsgessage(k, mp.getValue(), null);
                            break;
                        }
                    } catch (final Exception ex) {
                        throw new RuntimeException("Failed to add patch for: '" //$NON-NLS-1$
                                + mp.getMessage() + "'. TextEdit: " + e, ex); //$NON-NLS-1$
                    }
                }

                if (me.getChildrenSize() > 0) {
                    if (addImport) {
                        try {
                            addImport(me, f, bp);
                            return me;
                        } catch (final JavaModelException ex) {
                            NlsPlugin.logError(ex.getMessage(), ex);
                        }
                    } else {
                        return me;
                    }
                }
            }
        }

        return null;
    }

    private static void addImport(final MultiTextEdit me,
            final MessageFinder f, final NlsBundlePatch bp)
            throws JavaModelException {
        final String pkg = bp.getPackageName();

        if ((pkg.length() > 0)
                && !pkg.equals(f.getPackageName())
                && (f.getImport(bp.getPackageName(), bp.getQualifiedName()) == null)) {
            final CompilationUnit cu = f.getAst();
            final AST ast = cu.getAST();
            final ASTRewrite rewrite = ASTRewrite.create(ast);
            final ListRewrite listRewriter = rewrite.getListRewrite(cu,
                    CompilationUnit.IMPORTS_PROPERTY);
            final ImportDeclaration i = ast.newImportDeclaration();

            i.setStatic(false);
            i.setOnDemand(false);
            i.setName(ast.newName(bp.getQualifiedName()));
            listRewriter.insertLast(i, null);
            me.addChild(rewrite.rewriteAST(new Document(f.getSource()), null));
        }
    }
}
