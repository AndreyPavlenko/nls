package com.aap.nls.eclipse.parser;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.NlsPlugin;

/**
 * @author Andrey Pavlenko
 */
public class NlsParser {

    public static MessageFinder parse(final ICompilationUnit u,
            final IProgressMonitor monitor) {
        return parse(u, monitor, new StringBuilder(), new StringBuilder());
    }

    public static Collection<MessageFinder> parse(
            final Collection<ICompilationUnit> units,
            final IProgressMonitor monitor) {
        final Collection<MessageFinder> finders = new ArrayList<MessageFinder>(
                units.size());
        final StringBuilder sb1 = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        final IProgressMonitor nm = new NullProgressMonitor();
        monitor.beginTask(Msg.LookingForNonNls.toString(), units.size());

        for (final ICompilationUnit u : units) {
            monitor.subTask(Msg.Parsing.format(u.getPath()));
            final MessageFinder f = parse(u, nm, sb1, sb2);

            if (!f.getMessages().isEmpty()) {
                finders.add(f);
            }

            if (monitor.isCanceled()) {
                break;
            }

            monitor.worked(1);
        }

        return finders;
    }

    private static MessageFinder parse(final ICompilationUnit u,
            final IProgressMonitor monitor, final StringBuilder sb1,
            final StringBuilder sb2) {
        try {
            final String source = u.getSource();
            final CompilationUnit cu;
            final MessageFinder finder;
            final ASTParser parser = ASTParser.newParser(AST.JLS8);

            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(source.toCharArray());
            cu = (CompilationUnit) parser.createAST(monitor);
            finder = new MessageFinder(u, cu, source, sb1, sb2);
            return finder;
        } catch (final JavaModelException ex) {
            NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            return null;
        }
    }

}
