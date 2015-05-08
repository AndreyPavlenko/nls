package com.aap.nls.eclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.NlsPlugin;
import com.aap.nls.eclipse.parser.MessageFinder;
import com.aap.nls.eclipse.parser.NlsParser;
import com.aap.nls.eclipse.patch.NlsBundlePatch;
import com.aap.nls.eclipse.patch.SourcePatch;
import com.aap.nls.eclipse.wizard.InternationalizeWizard;

/**
 * @author Andrey Pavlenko
 */
public class Internationalize implements IObjectActionDelegate,
        IEditorActionDelegate {
    private volatile ISelection selection;
    private volatile IEditorPart targetEditor;

    @Override
    public void selectionChanged(final IAction action,
            final ISelection selection) {
        this.selection = selection;
    }

    @Override
    public void setActivePart(final IAction action,
            final IWorkbenchPart targetPart) {
    }

    @Override
    public void setActiveEditor(final IAction action,
            final IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }

    @Override
    public void run(final IAction action) {
        final ProgressMonitorDialog d = new ProgressMonitorDialog(Display
                .getCurrent().getActiveShell());
        final IProgressMonitor m = d.getProgressMonitor();
        final AtomicReference<Collection<MessageFinder>> findersRef = new AtomicReference<Collection<MessageFinder>>();
        m.setTaskName(Msg.LookingForNonNls.toString());

        try {
            d.run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor m)
                        throws InvocationTargetException, InterruptedException {
                    findersRef.set(lookup(m));
                }
            });
        } catch (final InterruptedException ex) {
        } catch (final InvocationTargetException ex) {
            NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
        } finally {
            m.done();
        }

        if (!m.isCanceled()) {
            final Collection<MessageFinder> finders = findersRef.get();

            if ((finders != null) && !finders.isEmpty()) {
                final InternationalizeWizard w = new InternationalizeWizard(
                        findersRef.get());
                final WizardDialog wd = new WizardDialog(Display.getCurrent()
                        .getActiveShell(), w);

                if (wd.open() == Window.OK) {
                    applyPatches(w);
                }
            }
        }
    }

    private Collection<MessageFinder> lookup(final IProgressMonitor m) {
        final Set<ICompilationUnit> units = new TreeSet<ICompilationUnit>(
                CompilationUnitComparator.instance);

        if (targetEditor != null) {
            final IEditorInput input = targetEditor.getEditorInput();

            if (input instanceof IFileEditorInput) {
                units.add(JavaCore
                        .createCompilationUnitFrom(((IFileEditorInput) input)
                                .getFile()));
            }
        } else if (selection instanceof IStructuredSelection) {
            final Iterator<?> it = ((IStructuredSelection) selection)
                    .iterator();

            while (!m.isCanceled() && it.hasNext()) {
                final Object o = it.next();

                if (o instanceof ICompilationUnit) {
                    units.add((ICompilationUnit) o);
                } else if (o instanceof IPackageFragment) {
                    addUnits((IPackageFragment) o, units, false, m);
                } else if (o instanceof IPackageFragmentRoot) {
                    addUnits((IPackageFragmentRoot) o, units, m);
                } else if (o instanceof IJavaProject) {
                    addUnits((IJavaProject) o, units, m);
                } else if (o instanceof IWorkingSet) {
                    addUnits((IWorkingSet) o, units, m);
                }
            }
        }

        return m.isCanceled() ? Collections.<MessageFinder> emptyList()
                : NlsParser.parse(units, m);
    }

    private static void applyPatches(final InternationalizeWizard w) {
        final NlsBundlePatch bp = w.getNlsBundlePatch();
        final Collection<SourcePatch> patches = w.getPatches();

        if ((bp != null) && !patches.isEmpty()) {
            final ProgressMonitorDialog d = new ProgressMonitorDialog(Display
                    .getCurrent().getActiveShell());
            final IProgressMonitor m = d.getProgressMonitor();
            m.setTaskName(Msg.ApplyPatches.toString());

            try {
                d.run(false, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor m)
                            throws InvocationTargetException,
                            InterruptedException {
                        m.beginTask(Msg.ApplyPatches.toString(),
                                patches.size() + 1);

                        for (final SourcePatch p : patches) {
                            if (m.isCanceled()) {
                                return;
                            }

                            m.subTask(Msg.ApplyPatchesTo.format(p
                                    .getCompilationUnit().getPath()));
                            p.apply();
                            m.worked(1);
                        }

                        m.subTask(Msg.ApplyPatchesTo.format(bp
                                .getCompilationUnit().getPath()));
                        bp.apply();
                        m.worked(1);
                    }
                });
            } catch (final InterruptedException ex) {
            } catch (final InvocationTargetException ex) {
                NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            } finally {
                m.done();
            }
        }
    }

    private static void addUnits(final IPackageFragment f,
            final Set<ICompilationUnit> units, final boolean addSubPackages,
            final IProgressMonitor m) {
        if (f.exists()) {
            try {
                for (final IJavaElement e : f.getChildren()) {
                    if (m.isCanceled()) {
                        break;
                    }
                    if (addSubPackages && (e instanceof IPackageFragment)) {
                        addUnits((IPackageFragment) e, units, true, m);
                    } else if (e instanceof ICompilationUnit) {
                        units.add((ICompilationUnit) e);
                    }
                }
            } catch (final JavaModelException ex) {
                NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            }
        }
    }

    private static void addUnits(final IPackageFragmentRoot r,
            final Set<ICompilationUnit> units, final IProgressMonitor m) {
        if (r.exists()) {
            try {
                for (final IJavaElement e : r.getChildren()) {
                    if (m.isCanceled()) {
                        break;
                    }
                    if (e instanceof IPackageFragment) {
                        addUnits((IPackageFragment) e, units, true, m);
                    }
                }
            } catch (final JavaModelException ex) {
                NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            }
        }
    }

    private static void addUnits(final IJavaProject p,
            final Set<ICompilationUnit> units, final IProgressMonitor m) {
        final IProject project = p.getProject();

        if (project.exists() && project.isOpen()) {
            try {
                for (final IPackageFragmentRoot r : p
                        .getAllPackageFragmentRoots()) {
                    if (m.isCanceled()) {
                        break;
                    }
                    if (r.getJavaProject() == p) {
                        addUnits(r, units, m);
                    }
                }
            } catch (final JavaModelException ex) {
                NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            }
        }
    }

    private static void addUnits(final IWorkingSet ws,
            final Set<ICompilationUnit> units, final IProgressMonitor m) {
        for (final Object o : ws.getElements()) {
            if (o instanceof IJavaProject) {
                addUnits((IJavaProject) o, units, m);
            }
        }
    }

    private static final class CompilationUnitComparator implements
            Comparator<ICompilationUnit> {
        static final CompilationUnitComparator instance = new CompilationUnitComparator();

        @Override
        public int compare(final ICompilationUnit u1, final ICompilationUnit u2) {
            return u1.getPath().toFile().compareTo(u2.getPath().toFile());
        }
    }
}
