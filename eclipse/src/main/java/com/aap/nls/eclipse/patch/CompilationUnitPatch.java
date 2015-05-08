package com.aap.nls.eclipse.patch;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.text.edits.TextEdit;

import com.aap.nls.eclipse.NlsPlugin;

/**
 * @author Andrey Pavlenko
 */
public abstract class CompilationUnitPatch {

    public abstract ICompilationUnit getCompilationUnit();

    public abstract TextEdit createTextEdit();

    public void apply() {
        IPath path = null;
        boolean disconnect = false;
        ITextFileBufferManager m = null;
        final ICompilationUnit unit = getCompilationUnit();

        try {
            final TextEdit edit = createTextEdit();

            if (edit != null) {
                final ITextFileBuffer buf;

                path = unit.getPath();
                m = FileBuffers.getTextFileBufferManager();
                m.connect(path, LocationKind.IFILE, null);
                disconnect = true;
                buf = m.getTextFileBuffer(path, LocationKind.IFILE);
                edit.apply(buf.getDocument());
                buf.commit(null, false);
            }
        } catch (final Exception ex) {
            NlsPlugin
                    .logError("Failed to apply patch on " + unit.getPath(), ex); //$NON-NLS-1$
        } finally {
            if (disconnect) {
                try {
                    m.disconnect(path, LocationKind.IFILE, null);
                } catch (final CoreException ex) {
                    NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
                }
            }
        }
    }
}
