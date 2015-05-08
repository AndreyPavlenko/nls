package com.aap.nls.eclipse.patch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.TextEdit;

import com.aap.nls.eclipse.NlsPlugin;

/**
 * @author Andrey Pavlenko
 */
public class NlsBundlePatch extends CompilationUnitPatch {
    private final ICompilationUnit compilationUnit;
    private final String simpleName;
    private final String qualifiedName;
    private final String packageName;
    private final CompilationUnit ast;
    private final String source;
    private final Map<String, String> messages;
    private final Map<String, String> addedMessages;
    private final Map<String, String> localMessages;
    private final Map<String, String> keyProposals;
    private final ASTRewrite rewrite;
    private final ListRewrite listRewriter;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private NlsBundlePatch(final ICompilationUnit compilationUnit,
            final IType type, final CompilationUnit ast, final String source,
            final EnumDeclaration enumDeclaration) {
        StringBuilder sb = null;
        final List<EnumConstantDeclaration> constants = enumDeclaration
                .enumConstants();
        final int size = constants.size();

        this.compilationUnit = compilationUnit;
        this.ast = ast;
        this.source = source;
        simpleName = type.getElementName();
        qualifiedName = type.getFullyQualifiedName();
        packageName = getPackageName(qualifiedName);
        messages = new HashMap<String, String>(size);
        addedMessages = new HashMap<String, String>();
        localMessages = new HashMap<String, String>(size);
        keyProposals = new HashMap<String, String>(size);

        for (final EnumConstantDeclaration c : constants) {
            final String n = c.getName().getFullyQualifiedName();
            final List args = c.arguments();

            if (args.size() == 1) {
                final Object arg = args.get(0);

                if (arg instanceof StringLiteral) {
                    addMsg(n, toString((StringLiteral) arg));
                    continue;
                } else if (arg instanceof InfixExpression) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }

                    addMsg(n, toString((InfixExpression) arg, sb));
                    continue;
                }
            }

            messages.put(n, null);
        }

        rewrite = ASTRewrite.create(ast.getAST());
        listRewriter = rewrite.getListRewrite(enumDeclaration,
                EnumDeclaration.ENUM_CONSTANTS_PROPERTY);
    }

    public static NlsBundlePatch createNlsBundlePatch(final ICompilationUnit u) {
        IType[] types = null;
        String source = null;
        CompilationUnit ast = null;
        final Visitor v = new Visitor();

        try {
            types = u.getTypes();

            if ((types.length > 0) && types[0].isEnum()) {
                final ASTParser p = ASTParser.newParser(AST.JLS8);

                source = u.getSource();
                p.setKind(ASTParser.K_COMPILATION_UNIT);
                p.setSource(u);
                ast = (CompilationUnit) p.createAST(null);
                ast.accept(v);
            }
        } catch (final Exception ex) {
            if (ex == Visitor.STOP_PARSING) {
                return new NlsBundlePatch(u, types[0], ast, source,
                        v.enumDeclaration);
            }
            NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
        }

        return null;
    }

    @Override
    public ICompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    @Override
    public TextEdit createTextEdit() {
        try {
            return rewrite.rewriteAST();
        } catch (final JavaModelException ex) {
            NlsPlugin.logError(ex.getMessage(), ex);
            return null;
        }
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSource() {
        return source;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    public void addMsgessage(final String key, final String value,
            final String javadoc) {
        if (!addedMessages.containsKey(key) && !messages.containsKey(key)) {
            final AST ast = this.ast.getAST();
            final EnumConstantDeclaration f = ast.newEnumConstantDeclaration();
            final StringLiteral v = ast.newStringLiteral();

            v.setEscapedValue("\"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$
            f.setName(ast.newSimpleName(key));
            f.arguments().add(v);

            if (javadoc != null) {
                final Javadoc doc = ast.newJavadoc();
                final TagElement tagComment = ast.newTagElement();
                final TextElement text = ast.newTextElement();

                text.setText(javadoc);
                tagComment.fragments().add(text);
                doc.tags().add(tagComment);
                f.setJavadoc(doc);
            }

            listRewriter.insertLast(f, null);
            addedMessages.put(key, value);
        }
    }

    public String proposeKey(final String value) {
        String k = keyProposals.get(value);

        if (k == null) {
            final StringBuilder sb = new StringBuilder(value.length());
            boolean uc = true;

            for (int i = 0; i < value.length(); i++) {
                final char c = value.charAt(i);

                if (Character.isLetter(c)) {
                    if (uc) {
                        sb.append(Character.toUpperCase(c));
                    } else {
                        sb.append(c);
                    }

                    uc = false;
                } else if ((sb.length() > 0)
                        && ((Character.isDigit(c)) || (c == '_'))) {
                    sb.append(c);
                    uc = true;
                } else {
                    uc = true;
                }
            }

            if (sb.length() == 0) {
                sb.append('M');
            } else if (sb.charAt(sb.length() - 1) == '_') {
                sb.setLength(sb.length() - 1);
            }

            k = sb.toString();

            for (int i = 1; localMessages.containsKey(k); i++) {
                k = sb.toString() + '_' + (i++);
            }

            keyProposals.put(value, k);
            localMessages.put(k, value);
        }

        return k;
    }

    private void addMsg(final String key, final String value) {
        messages.put(key, value);
        localMessages.put(key, value);

        if (value != null) {
            keyProposals.put(value, key);
        }
    }

    private static String getPackageName(final String className) {
        final int ind = className.lastIndexOf('.');
        return ind == -1 ? "" : className.substring(0, ind); //$NON-NLS-1$
    }

    private String toString(final StringLiteral str) {
        final int len = str.getLength();
        return (len == 2) ? "" : getText(str.getStartPosition() + 1, //$NON-NLS-1$
                len - 2);
    }

    private String toString(final InfixExpression ie, final StringBuilder sb) {
        Expression expr;
        sb.setLength(0);

        if ((expr = ie.getLeftOperand()) instanceof StringLiteral) {
            sb.append(toString((StringLiteral) expr));
        } else {
            return null;
        }

        if ((expr = ie.getRightOperand()) instanceof StringLiteral) {
            sb.append(toString((StringLiteral) expr));
        } else {
            return null;
        }

        for (final Object o : ie.extendedOperands()) {
            if (o instanceof StringLiteral) {
                sb.append(toString((StringLiteral) o));
            } else {
                return null;
            }
        }

        return sb.toString();
    }

    private String getText(final int off, final int len) {
        try {
            return source.substring(off, off + len);
        } catch (final StringIndexOutOfBoundsException ex) {
            NlsPlugin.logError("Offset: " + off + ", Length: " + len //$NON-NLS-1$ //$NON-NLS-2$
                    + ", Source: \n" + source, ex); //$NON-NLS-1$
            return ex.toString();
        }
    }

    private static final class Visitor extends ASTVisitor {
        static final RuntimeException STOP_PARSING = new RuntimeException();
        EnumDeclaration enumDeclaration;

        @Override
        public boolean visit(final EnumDeclaration node) {
            enumDeclaration = node;
            throw STOP_PARSING;
        }
    }
}
