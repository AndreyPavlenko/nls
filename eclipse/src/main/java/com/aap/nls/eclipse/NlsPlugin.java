package com.aap.nls.eclipse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Andrey Pavlenko
 */
public class NlsPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.aap.nls.eclipse"; //$NON-NLS-1$
    public static final String IMG_IGNORE = "/icons/ignore.gif"; //$NON-NLS-1$
    public static final String IMG_EXTERNALIZE = "/icons/externalize.gif"; //$NON-NLS-1$
    public static final String IMG_NON_NLS = "/icons/non-nls.gif"; //$NON-NLS-1$
    public static final String EOL = System.getProperty("line.separator"); //$NON-NLS-1$
    private static NlsPlugin plugin;
    private final ConcurrentHashMap<String, Image> images = new ConcurrentHashMap<String, Image>();

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);

        for (final Image img : images.values()) {
            img.dispose();
        }
    }

    public static NlsPlugin getInstance() {
        return plugin;
    }

    public Image getImage(final String path) {
        Image img = images.get(path);

        if (img == null) {
            final ImageDescriptor descr = getImageDescriptor(path);

            if (descr != null) {
                final Image img2 = images.putIfAbsent(path,
                        img = descr.createImage());

                if (img2 != null) {
                    img.dispose();
                    return img2;
                }
            }
        }

        return img;
    }

    public static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static void logInfo(final Object msg) {
        logInfo(msg, null);
    }

    public static void logInfo(final Object msg, final Throwable ex) {
        log(IStatus.INFO, msg, ex);
    }

    public static void logWarning(final Object msg, final Throwable ex) {
        log(IStatus.WARNING, msg, ex);
    }

    public static void logError(final Object msg, final Throwable ex) {
        log(IStatus.ERROR, msg, ex);
    }

    public static void log(final int severity, final Object msg,
            final Throwable ex) {
        final Plugin plugin = getInstance();

        if (plugin != null) {
            plugin.getLog().log(
                    new Status(severity, PLUGIN_ID, String.valueOf(msg), ex));
        } else {
            System.err.println(msg);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }

    public static boolean isNlsBundle(final ICompilationUnit u) {
        if (u != null) {
            try {
                final IType[] types = u.getTypes();

                if ((types.length > 0) && types[0].isEnum()) {
                    final ASTParser p = ASTParser.newParser(AST.JLS8);
                    final NlsBundleDetector d = new NlsBundleDetector();
                    p.setKind(ASTParser.K_COMPILATION_UNIT);
                    p.setResolveBindings(true);
                    p.setSource(u);
                    p.createAST(null).accept(d);
                    return d.isNlsBundle();
                }
            } catch (final Exception ex) {
                NlsPlugin.logError("Error occurred", ex); //$NON-NLS-1$
            }
        }

        return false;
    }

    private static final class NlsBundleDetector extends ASTVisitor {
        private int counter;

        @Override
        public boolean visit(final MethodDeclaration node) {
            if (node.isConstructor()) {
                final List<?> params = node.parameters();

                if (params.size() == 1) {
                    final String n = ((SingleVariableDeclaration) params.get(0))
                            .getType().resolveBinding().getBinaryName();

                    if (String.class.getName().equals(n)) {
                        counter++;
                    }
                }
            } else if ("format".equals(node.getName().getIdentifier())) { //$NON-NLS-1$
                if (node.parameters().size() > 0) {
                    final String n = node.getReturnType2().resolveBinding()
                            .getBinaryName();

                    if (String.class.getName().equals(n)) {
                        counter++;
                    }
                }
            }

            return false;
        }

        boolean isNlsBundle() {
            return counter > 1;
        }
    }
}
