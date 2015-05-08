package com.aap.nls.eclipse.wizard;

import java.util.Collection;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.patch.SourcePatch;

/**
 * @author Andrey Pavlenko
 */
public class SelectSourcesPage extends WizardPage {

    protected SelectSourcesPage() {
        super(Msg.SelectSourcesToInternationalize.toString(),
                Msg.SelectSourcesToInternationalize.toString(), null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void createControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final Collection<SourcePatch> patches = ((InternationalizeWizard) getWizard())
                .getPatches();

        if (!patches.isEmpty()) {
            final ContainerCheckedTreeViewer v = new ContainerCheckedTreeViewer(
                    composite, SWT.NONE);
            v.setAutoExpandLevel(3);
            v.setContentProvider(new StandardJavaElementContentProvider());
            v.setLabelProvider(new JavaElementLabelProvider(
                    JavaElementLabelProvider.SHOW_DEFAULT));
            v.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
//            v.setInput(patches.iterator().next().getCompilationUnit()
//                    .getJavaProject().getParent());
            v.setFilters(new ViewerFilter[] { new ViewerFilter() {

                @Override
                public boolean select(final Viewer viewer,
                        final Object parentElement, final Object element) {
                    if (element instanceof IJavaProject) {
                        for (final SourcePatch p : patches) {
                            if (element.equals(p.getMessageFinder()
                                    .getCompilationUnit().getJavaProject())) {
                                return true;
                            }
                        }
                    } else if (element instanceof IPackageFragmentRoot) {
                        for (final SourcePatch p : patches) {
                            if (element.equals(p.getMessageFinder()
                                    .getCompilationUnit().getParent()
                                    .getParent())) {
                                return true;
                            }
                        }
                    } else if (element instanceof IPackageFragment) {
                        for (final SourcePatch p : patches) {
                            if (element.equals(p.getMessageFinder()
                                    .getCompilationUnit().getParent())) {
                                return true;
                            }
                        }
                    } else if (element instanceof ICompilationUnit) {
                        for (final SourcePatch p : patches) {
                            if (element.equals(p.getMessageFinder()
                                    .getCompilationUnit())) {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            } });

            v.setAllChecked(true);
            v.addCheckStateListener(new ICheckStateListener() {

                @Override
                public void checkStateChanged(final CheckStateChangedEvent e) {
                    boolean pageComplete = false;

                    loop: for (final SourcePatch p : patches) {
                        final ICompilationUnit u = p.getMessageFinder()
                                .getCompilationUnit();

                        for (final Object o : v.getCheckedElements()) {
                            if (u.equals(o)) {
                                p.setEnabled(true);
                                pageComplete = true;
                                continue loop;
                            }
                        }

                        p.setEnabled(false);
                    }

                    setPageComplete(pageComplete);
                }
            });
        }

        composite.setLayout(new FillLayout());
        setControl(composite);
    }
}
