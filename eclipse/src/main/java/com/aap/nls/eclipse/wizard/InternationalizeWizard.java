package com.aap.nls.eclipse.wizard;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.parser.MessageFinder;
import com.aap.nls.eclipse.patch.NlsBundlePatch;
import com.aap.nls.eclipse.patch.SourcePatch;

/**
 * @author Andrey Pavlenko
 */
public class InternationalizeWizard extends Wizard {
    private static ICompilationUnit selectedNlsBundlePatch;
    private final Collection<SourcePatch> patches;
    private NlsBundlePatch nlsBundlePatch;

    public InternationalizeWizard(final Collection<MessageFinder> finders) {
        setWindowTitle(Msg.InternationalizeSources.toString());
        setHelpAvailable(false);
        this.patches = new ArrayList<SourcePatch>(finders.size());

        for (final MessageFinder f : finders) {
            patches.add(new SourcePatch(f));
        }
        if ((selectedNlsBundlePatch != null)
                && !selectedNlsBundlePatch.exists()) {
            selectedNlsBundlePatch = null;
        }
    }

    @Override
    public void addPages() {
        addPage(new SelectSourcesPage());
        addPage(new SelectBundlePage());
        addPage(new CustomizationPage());
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage p = super.getNextPage(page);

        if (p instanceof CustomizationPage) {
            ((CustomizationPage) p).refresh(this);
        }

        return p;
    }

    @Override
    public boolean performFinish() {
        return true;
    }

    public Collection<SourcePatch> getPatches() {
        return patches;
    }

    public NlsBundlePatch getNlsBundlePatch() {
        if ((nlsBundlePatch == null) && (selectedNlsBundlePatch != null)) {
            return nlsBundlePatch = NlsBundlePatch
                    .createNlsBundlePatch(selectedNlsBundlePatch);
        }
        return nlsBundlePatch;
    }

    void selectNlsBundlePatch(final ICompilationUnit nlsBundlePatch) {
        if (nlsBundlePatch.exists()) {
            selectedNlsBundlePatch = nlsBundlePatch;
            this.nlsBundlePatch = null;
        }
    }

    ICompilationUnit getSelectedNlsBundlePatch() {
        return selectedNlsBundlePatch;
    }
}
