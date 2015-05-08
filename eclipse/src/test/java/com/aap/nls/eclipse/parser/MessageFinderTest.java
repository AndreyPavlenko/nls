package com.aap.nls.eclipse.parser;

import junit.framework.TestCase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;

import com.aap.nls.eclipse.parser.Message;
import com.aap.nls.eclipse.parser.MessageFinder;

/**
 * @author Andrey Pavlenko
 */
@SuppressWarnings("nls")
public class MessageFinderTest extends TestCase {

    public void testExternalize() throws Exception {
        final String source = "class A {"
                + "String s = \"test\" + (1 + \"test\") + 2 + 3; "
                + "String s1 = \"sss\";}";
        final String expected = "class A {"
                + "String s = Key1.format((1 + \"test\"), 2, 3); "
                + "String s1 = Key2.toString();}";
        apply(source, expected, true);
    }

    public void testInternalize() throws Exception {
        final String source = "class A {"
                + "String s = \"test\" + (1 + \"test\") + 2 + 3; "
                + "String s1 = \"sss\";}";
        final String expected = "class A {"
                + "String s = \"test\" + (1 + \"test\") + 2 + 3; "
                + "String s1 = \"sss\";} //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$";
        apply(source, expected, false);
    }

    private static void apply(final String source, final String expected,
            final boolean externalize) throws MalformedTreeException,
            BadLocationException {
        final CompilationUnit cu;
        final MessageFinder finder;
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        int i = 1;

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        cu = (CompilationUnit) parser.createAST(null);
        finder = new MessageFinder(null, cu, source, new StringBuilder(),
                new StringBuilder());

        final IDocument doc = new Document(source);
        final MultiTextEdit edit = new MultiTextEdit();

        if (externalize) {
            for (final Message m : finder.getMessages()) {
                edit.addChild(m.createExternalizeEdit("Key" + i++));
            }
        } else {
            for (final Message m : finder.getMessages()) {
                edit.addChild(m.createInternalizeEdit());
            }
        }

        edit.apply(doc);
        assertEquals(expected, doc.get());
    }
}
