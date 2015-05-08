package com.aap.nls.eclipse.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.aap.nls.eclipse.NlsPlugin;

/**
 * @author Andrey Pavlenko
 */
public class MessageFinder {
    private static final Pattern NON_NLS = Pattern
            .compile("//\\$NON-NLS-(\\d+)\\$"); //$NON-NLS-1$
    private final ICompilationUnit compilationUnit;
    private final CompilationUnit ast;
    private final String source;
    private final String packageName;
    private final Collection<Message> messages;
    private final Collection<ImportDeclaration> imports;

    MessageFinder(final ICompilationUnit compilationUnit,
            final CompilationUnit ast, final String source,
            final StringBuilder sb1, final StringBuilder sb2) {
        this.compilationUnit = compilationUnit;
        this.ast = ast;
        this.source = source;

        if (NlsPlugin.isNlsBundle(compilationUnit)) {
            packageName = ""; //$NON-NLS-1$
            messages = Collections.emptyList();
            imports = Collections.emptyList();
        } else {
            final Visitor visitor = new Visitor();
            final List<Message> messages = new ArrayList<Message>();

            ast.accept(visitor);
            visitor.processComments();

            for (final ComplexMessage m : visitor.complexMessages) {
                m.process(this, sb1, sb2);

                if (!m.isInternalized()) {
                    messages.add(m);
                }
            }

            for (final Line l : visitor.lines.values()) {
                for (final SimpleMessage m : l.getSimpleMessages()) {
                    if ((!m.isInternalized())
                            && (m.getComplexMessage() == null)) {
                        messages.add(m);
                    }
                }
            }

            Collections.sort(messages);
            this.messages = messages;
            packageName = visitor.packageName;
            imports = visitor.imports;
        }
    }

    public String getSource() {
        return source;
    }

    public ICompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public CompilationUnit getAst() {
        return ast;
    }

    public String getPackageName() {
        return packageName;
    }

    public Collection<ImportDeclaration> getImports() {
        return imports;
    }

    public ImportDeclaration getImport(final String packageName,
            final String qname) {
        for (final ImportDeclaration i : getImports()) {
            if (!i.isStatic()) {
                if (i.isOnDemand()) {
                    if ((packageName != null)
                            && packageName.equals(i.getName()
                                    .getFullyQualifiedName())) {
                        return i;
                    }
                } else {
                    if ((qname != null)
                            && qname.equals(i.getName().getFullyQualifiedName())) {
                        return i;
                    }
                }
            }
        }

        return null;
    }

    public Collection<Message> getMessages() {
        return messages;
    }

    String getText(final int off, final int len) {
        try {
            return source.substring(off, off + len);
        } catch (final StringIndexOutOfBoundsException ex) {
            NlsPlugin.logError("Offset: " + off + ", Length: " + len //$NON-NLS-1$ //$NON-NLS-2$
                    + ", Source: \n" + source, ex); //$NON-NLS-1$
            return ex.toString();
        }
    }

    private final class Visitor extends ASTVisitor {
        final HashMap<Integer, Line> lines = new HashMap<Integer, Line>();
        final Collection<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
        final Collection<ComplexMessage> complexMessages = new ArrayList<ComplexMessage>();
        private String packageName = ""; //$NON-NLS-1$
        private ComplexMessage complexMessage;

        @Override
        public boolean visit(final StringLiteral node) {
            final int off = node.getStartPosition();
            final int len = node.getLength();
            final Line l = getLine(ast.getLineNumber(off), off);
            new SimpleMessage(node, complexMessage, l, off, len, getText(off,
                    len));
            return false;
        }

        @Override
        public boolean visit(final InfixExpression node) {
            if (complexMessage == null) {
                if (isComplexMessage(node)) {
                    final int off = node.getStartPosition();
                    final int len = node.getLength();
                    final int startLineNum = ast.getLineNumber(off);
                    final int endLineNum = ast.getLineNumber((off + len) - 1);
                    Collection<Line> lines;

                    if (startLineNum == endLineNum) {
                        lines = Collections.singletonList(getLine(startLineNum,
                                off));
                    } else {
                        lines = new ArrayList<Line>(endLineNum - startLineNum);
                        createLines(node);

                        for (int i = startLineNum; i <= endLineNum; i++) {
                            lines.add(getLine(i, off));
                        }

                        lines = Collections.unmodifiableCollection(lines);
                    }

                    complexMessage = new ComplexMessage(node, lines);
                    complexMessages.add(complexMessage);
                }
            }

            return true;
        }

        @Override
        public void endVisit(final InfixExpression node) {
            if ((complexMessage != null)
                    && (complexMessage.getExpression() == node)) {
                complexMessage = null;
            }
        }

        @Override
        public boolean visit(final ImportDeclaration node) {
            imports.add(node);
            return false;
        }

        @Override
        public boolean visit(final LineComment node) {
            final int off = node.getStartPosition();
            final Integer lineNum = ast.getLineNumber(off);
            final Line l = lines.get(lineNum);

            if (l != null) {
                for (final Matcher m = NON_NLS.matcher(getText(off,
                        node.getLength())); m.find();) {
                    try {
                        final int ind = Integer.parseInt(m.group(1));
                        final int start = m.start();
                        final int end = m.end();
                        new NonNlsTag(l, off + start, end - start, ind);
                    } catch (final NumberFormatException ex) {
                    }
                }
            }

            return false;
        }

        void processComments() {
            if (!lines.isEmpty()) {
                final List<?> comments = ast.getCommentList();

                if ((comments != null) && !comments.isEmpty()) {
                    for (final Object o : comments) {
                        if (o instanceof LineComment) {
                            ((LineComment) o).accept(this);
                        }
                    }
                }
            }
        }

        @Override
        public boolean visit(final AnnotationTypeDeclaration node) {
            return false;
        }

        @Override
        public boolean visit(final AnnotationTypeMemberDeclaration node) {
            return false;
        }

        @Override
        public boolean visit(final Javadoc node) {
            return false;
        }

        @Override
        public boolean visit(final MarkerAnnotation node) {
            return false;
        }

        @Override
        public boolean visit(final NormalAnnotation node) {
            return false;
        }

        @Override
        public boolean visit(final PackageDeclaration node) {
            packageName = node.getName().getFullyQualifiedName();
            return false;
        }

        @Override
        public boolean visit(final AssertStatement node) {
            return false;
        }

        @Override
        public boolean visit(final BreakStatement node) {
            return false;
        }

        @Override
        public boolean visit(final SingleMemberAnnotation node) {
            return false;
        }

        @Override
        public boolean visit(final TypeDeclaration node) {
            return !hasSupressNlsAnnotation(node);
        }

        @Override
        public boolean visit(final MethodDeclaration node) {
            return !hasSupressNlsAnnotation(node);
        }

        @Override
        public boolean visit(final FieldDeclaration node) {
            return !hasSupressNlsAnnotation(node);
        }

        @Override
        public boolean visit(final EnumConstantDeclaration node) {
            return !hasSupressNlsAnnotation(node);
        }

        @Override
        public boolean visit(final EnumDeclaration node) {
            return !hasSupressNlsAnnotation(node);
        }

        private boolean hasSupressNlsAnnotation(final BodyDeclaration d) {
            for (final Object o : d.modifiers()) {
                if (o instanceof SingleMemberAnnotation) {
                    final SingleMemberAnnotation a = (SingleMemberAnnotation) o;

                    if (isSuppressWarnings(a) && isSupressNls(a)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isSuppressWarnings(final Annotation a) {
            final String n = a.getTypeName().getFullyQualifiedName();
            return "SuppressWarnings".equals(n) || //$NON-NLS-1$
                    "java.lang.SuppressWarnings".equals(n); //$NON-NLS-1$

        }

        private boolean isSupressNls(final SingleMemberAnnotation a) {
            final Expression expr = a.getValue();

            if (expr instanceof StringLiteral) {
                if (isNls((StringLiteral) expr)) {
                    return true;
                }
            } else if (expr instanceof ArrayInitializer) {
                for (final Object o : ((ArrayInitializer) expr).expressions()) {
                    if ((o instanceof StringLiteral)
                            && (isNls((StringLiteral) o))) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean isNls(final StringLiteral str) {
            return "nls".equals(str.getLiteralValue()); //$NON-NLS-1$
        }

        private Line getLine(final int lineNum, final int off) {
            Line l = lines.get(lineNum);

            if (l == null) {
                l = new Line(lineNum, getEndOffset(off));
                lines.put(lineNum, l);
            }

            return l;
        }

        private int getEndOffset(int off) {
            final String src = source;
            final int len = src.length();

            for (; off < len; off++) {
                switch (src.charAt(off)) {
                case '\r':
                case '\n':
                    return off;
                }
            }

            return off;
        }

        private void createLines(final InfixExpression node) {
            createLine(node.getLeftOperand());
            createLine(node.getRightOperand());

            for (final Object o : node.extendedOperands()) {
                createLine((Expression) o);
            }
        }

        private void createLine(final Expression expr) {
            final int off = expr.getStartPosition();
            getLine(ast.getLineNumber(off), off);
        }

        private boolean isComplexMessage(final InfixExpression node) {
            if ((node.getLeftOperand() instanceof StringLiteral)
                    || (node.getRightOperand() instanceof StringLiteral)) {
                return true;
            }

            for (final Object expr : node.extendedOperands()) {
                if (expr instanceof StringLiteral) {
                    return true;
                }
            }

            return false;
        }
    }
}
