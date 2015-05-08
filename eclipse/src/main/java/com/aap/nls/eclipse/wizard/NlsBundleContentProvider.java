package com.aap.nls.eclipse.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import com.aap.nls.eclipse.NlsPlugin;
import com.aap.nls.eclipse.patch.SourcePatch;

/**
 * @author Andrey Pavlenko
 */
class NlsBundleContentProvider extends StandardJavaElementContentProvider {
    private final Collection<SourcePatch> patches;

    public NlsBundleContentProvider(final Collection<SourcePatch> patches) {
        this.patches = patches;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(final Object element) {
        if (!exists(element)) {
            return NO_CHILDREN;
        }

        try {
            if (element instanceof IJavaModel) {
                return getJavaProjects((IJavaModel) element);
            }

            if (element instanceof IJavaProject) {
                return getPackageFragmentRoots((IJavaProject) element);
            }

            if (element instanceof IPackageFragmentRoot) {
                return getPackageFragmentRootContent((IPackageFragmentRoot) element);
            }

            if (element instanceof IPackageFragment) {
                return getPackageContent((IPackageFragment) element);
            }
        } catch (final CoreException e) {
            return NO_CHILDREN;
        }

        return NO_CHILDREN;
    }

    @Override
    protected Object[] getJavaProjects(final IJavaModel jm)
            throws JavaModelException {
        final IJavaProject[] projects = jm.getJavaProjects();
        final List<IJavaProject> l = new ArrayList<IJavaProject>(
                projects.length);

        for (final IJavaProject p : projects) {
            if (isAcceptable(p)) {
                l.add(p);
            }
        }

        return l.size() == projects.length ? projects : l.toArray();
    }

    @Override
    protected Object[] getPackageFragmentRoots(final IJavaProject project)
            throws JavaModelException {
        final IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
        final List<IPackageFragmentRoot> l = new ArrayList<IPackageFragmentRoot>(
                roots.length);

        for (final IPackageFragmentRoot r : roots) {
            if (isAcceptable(r)) {
                l.add(r);
            }
        }

        return l.size() == roots.length ? roots : l.toArray();
    }

    @Override
    protected Object[] getPackageFragmentRootContent(
            final IPackageFragmentRoot r) throws JavaModelException {
        if (r.getKind() == IPackageFragmentRoot.K_SOURCE) {
            final IJavaElement[] children = r.getChildren();
            final List<IJavaElement> l = new ArrayList<IJavaElement>(
                    children.length);

            for (final IJavaElement c : children) {
                if (c instanceof IPackageFragment) {
                    if (isAcceptable((IPackageFragment) c)) {
                        l.add(c);
                    }
                } else if (c instanceof ICompilationUnit) {
                    if (isAcceptable((ICompilationUnit) c)) {
                        l.add(c);
                    }
                }
            }

            return l.size() == children.length ? children : l.toArray();
        }
        return NO_CHILDREN;
    }

    @Override
    protected Object[] getPackageContent(final IPackageFragment f)
            throws JavaModelException {
        final ICompilationUnit[] units = f.getCompilationUnits();
        final List<ICompilationUnit> l = new ArrayList<ICompilationUnit>(
                units.length);

        for (final ICompilationUnit u : units) {
            if (NlsPlugin.isNlsBundle(u)) {
                l.add(u);
            }
        }

        return l.size() == units.length ? units : l.toArray();
    }

    private boolean isAcceptable(final IJavaProject p)
            throws JavaModelException {
        if (isAcceptable(p, patches)) {
            for (final IPackageFragmentRoot r : p.getPackageFragmentRoots()) {
                if (isAcceptable(r)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean isAcceptable(final IJavaProject p,
            final Collection<SourcePatch> patches) {
        for (final SourcePatch patch : patches) {
            final IJavaProject proj = patch.getCompilationUnit()
                    .getJavaProject();

            if (proj.equals(p) || proj.isOnClasspath(p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAcceptable(final IPackageFragmentRoot r)
            throws JavaModelException {
        if (r.getKind() == IPackageFragmentRoot.K_SOURCE) {
            for (final IJavaElement c : r.getChildren()) {
                if (c instanceof IPackageFragment) {
                    if (isAcceptable((IPackageFragment) c)) {
                        return true;
                    }
                } else if (c instanceof ICompilationUnit) {
                    if (isAcceptable((ICompilationUnit) c)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isAcceptable(final IPackageFragment f)
            throws JavaModelException {
        for (final ICompilationUnit u : f.getCompilationUnits()) {
            if (isAcceptable(u)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAcceptable(final ICompilationUnit u)
            throws JavaModelException {
        return NlsPlugin.isNlsBundle(u);
    }
}
