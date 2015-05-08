package com.aap.nls.eclipse.wizard;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.NlsPlugin;

/**
 * @author Andrey Pavlenko
 */
public class SelectBundlePage extends WizardPage implements Listener {
    private Text textField;
    private Button browse;

    protected SelectBundlePage() {
        super("SelectBundlePage", Msg.SelectNlsBundleClass.toString(), null); //$NON-NLS-1$
    }

    @Override
    public boolean isPageComplete() {
        return getNlsBundlePath().length() != 0;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite c = new Composite(parent, SWT.NONE);

        c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        c.setLayout(new GridLayout(2, false));

        addDescription(c);

        textField = new Text(c, SWT.READ_ONLY | SWT.LEFT | SWT.BORDER);
        textField.setText(getNlsBundlePath());
        textField
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        browse = new Button(c, SWT.PUSH);
        browse.setText(Msg.Browse.toString());
        browse.addListener(SWT.Selection, this);
        browse.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        setControl(c);
    }

    private static void addDescription(final Composite parent) {
        final Browser b = new Browser(parent, SWT.NONE);

        b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        b.setText("<html><body style='overflow: hidden'>" + Msg.NlsBundleDesc //$NON-NLS-1$
                + "<body/><html/>"); //$NON-NLS-1$
    }

    private void browse() {
        final ICompilationUnit u = selectBundle();

        if (u != null) {
            ((InternationalizeWizard) getWizard()).selectNlsBundlePatch(u);
            textField.setText(getNlsBundlePath());

            if (isCurrentPage()) {
                getContainer().updateButtons();
            }
        }
    }

    private String getNlsBundlePath() {
        final InternationalizeWizard w = (InternationalizeWizard) getWizard();
        final ICompilationUnit u = w.getSelectedNlsBundlePatch();

        if (u != null) {
            if (NlsBundleContentProvider.isAcceptable(u.getJavaProject(),
                    w.getPatches())) {
                return u.getPath().toString();
            }
        }

        return ""; //$NON-NLS-1$
    }

    private ICompilationUnit selectBundle() {
        final InternationalizeWizard w = (InternationalizeWizard) getWizard();
        final NlsBundleContentProvider provider = new NlsBundleContentProvider(
                w.getPatches());
        final ILabelProvider labelProvider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT);
        final ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                getShell(), labelProvider, provider) {
            @Override
            protected TreeViewer createTreeViewer(final Composite parent) {
                final TreeViewer v = super.createTreeViewer(parent);
                v.expandAll();
                return v;
            }
        };

        dialog.setAllowMultiple(false);
        dialog.setDoubleClickSelects(true);
        dialog.setValidator(new SelectionValidator());
        dialog.setComparator(new JavaElementComparator());
        dialog.setTitle(Msg.SelectNlsBundleClass.toString());
        dialog.setMessage(Msg.SelectNlsBundleClass.toString());
        dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace()
                .getRoot()));
        dialog.setInitialSelection(w.getSelectedNlsBundlePatch());

        if (dialog.open() == Window.OK) {
            return (ICompilationUnit) dialog.getFirstResult();
        }

        return null;
    }

    @Override
    public void handleEvent(final Event e) {
        if (e.type == SWT.Selection) {
            if (e.widget == browse) {
                browse();
            }
        }
    }

    private static final class SelectionValidator implements
            ISelectionStatusValidator {
        @Override
        public IStatus validate(final Object[] selection) {
            if (selection.length == 1) {
                if (selection[0] instanceof ICompilationUnit) {
                    return new Status(IStatus.OK, NlsPlugin.PLUGIN_ID, 0,
                            "", null); //$NON-NLS-1$
                }
            }
            return new Status(IStatus.ERROR, NlsPlugin.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
        }

    }
}
