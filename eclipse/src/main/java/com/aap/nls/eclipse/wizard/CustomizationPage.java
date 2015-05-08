package com.aap.nls.eclipse.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.aap.nls.eclipse.Msg;
import com.aap.nls.eclipse.NlsPlugin;
import com.aap.nls.eclipse.parser.Message;
import com.aap.nls.eclipse.patch.MessagePatch;
import com.aap.nls.eclipse.patch.NlsBundlePatch;
import com.aap.nls.eclipse.patch.SourcePatch;

/**
 * @author Andrey Pavlenko
 */
public class CustomizationPage extends WizardPage implements
        IStructuredContentProvider, ITableLabelProvider, Listener,
        ICellModifier {
    private static final Pattern KEY_PATTERN = Pattern.compile("\\w+"); //$NON-NLS-1$
    private static final String[] COL_PROPS = new String[] { "Action", "Key", //$NON-NLS-1$ //$NON-NLS-2$
            "Value" }; //$NON-NLS-1$
    private IDocument doc;
    private Table table;
    private TableViewer tableViewer;
    private SourceViewer sourceViewer;
    private Label sourceLabel;
    private MessagePatch selectedPatch;

    protected CustomizationPage() {
        super("CustomizationPage", Msg.CustomizeMessages.toString(), null); //$NON-NLS-1$
        setPageComplete(false);
    }

    void refresh(final InternationalizeWizard w) {
        final NlsBundlePatch bp = w.getNlsBundlePatch();

        for (final SourcePatch p : w.getPatches()) {
            p.setNlsBundlePatch(bp);
        }

        tableViewer.setInput(this);
        setPageComplete(true);
        updatePreview();
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite c = new Composite(parent, SWT.NONE);

        c.setLayoutData(new GridData(GridData.FILL_BOTH));
        c.setLayout(new GridLayout());

        final SashForm sash = new SashForm(c, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        createTable(sash);
        createSourceViewer(sash);

        sash.setWeights(new int[] { 65, 45 });
        setControl(c);
    }

    private void createTable(final Composite parent) {
        final Composite c = new Composite(parent, SWT.NONE);
        c.setLayoutData(new GridData(GridData.FILL_BOTH));
        c.setLayout(new GridLayout(2, false));

        table = new Table(c, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.MULTI | SWT.FULL_SELECTION);
        final TableColumn c1 = new TableColumn(table, SWT.NONE);
        final TableColumn c2 = new TableColumn(table, SWT.NONE);
        final TableColumn c3 = new TableColumn(table, SWT.NONE);
        final TableLayout layout = new TableLayout();

        c1.setResizable(false);
        c2.setText(Msg.Key.toString());
        c3.setText(Msg.Value.toString());

        table.setHeaderVisible(true);
        table.addListener(SWT.Selection, this);
        table.setLayout(layout);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        layout.addColumnData(new ColumnPixelData(25, false));
        layout.addColumnData(new ColumnWeightData(20, 30, false));
        layout.addColumnData(new ColumnWeightData(20, 75, true));

        tableViewer = new TableViewer(table);
        tableViewer.setCellModifier(this);
        tableViewer.setLabelProvider(this);
        tableViewer.setContentProvider(this);
        tableViewer.setColumnProperties(COL_PROPS);
        tableViewer.setCellEditors(new CellEditor[] { null,
                new TextCellEditor(table), new TextCellEditor(table) });

        createButtons(c);
    }

    private void createButtons(final Composite parent) {
        Button b;
        final Composite c = new Composite(parent, SWT.NONE);
        final RowLayout l = new RowLayout(SWT.VERTICAL);

        c.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING
                | GridData.HORIZONTAL_ALIGN_CENTER));

        l.pack = false;
        c.setLayout(l);

        b = new Button(c, SWT.PUSH);
        b.setText(Msg.Ignore.toString());
        b.setData(MessagePatch.Action.IGNORE);
        b.addListener(SWT.Selection, this);

        b = new Button(c, SWT.PUSH);
        b.setText(Msg.NonNls.toString());
        b.setData(MessagePatch.Action.NON_NLS);
        b.addListener(SWT.Selection, this);

        b = new Button(c, SWT.PUSH);
        b.setText(Msg.Externalize.toString());
        b.setData(MessagePatch.Action.EXTERNALIZE);
        b.addListener(SWT.Selection, this);
    }

    public void createSourceViewer(final Composite parent) {
        final Composite c = new Composite(parent, SWT.NONE);
        final IPreferenceStore store = PreferenceConstants.getPreferenceStore();
        final JavaTextTools tools = new JavaTextTools(store);
        final JavaSourceViewerConfiguration conf = new JavaSourceViewerConfiguration(
                tools.getColorManager(), store, null,
                IJavaPartitions.JAVA_PARTITIONING);

        c.setLayoutData(new GridData(GridData.FILL_BOTH));
        c.setLayout(new GridLayout());
        createSourceLabel(c);

        doc = new Document();
        tools.setupJavaDocumentPartitioner(doc,
                IJavaPartitions.JAVA_PARTITIONING);
        sourceViewer = new SourceViewer(c, null, null, false, SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        sourceViewer.configure(conf);
        sourceViewer.setDocument(doc);
        sourceViewer.setEditable(false);
        sourceViewer.getControl().setLayoutData(
                new GridData(GridData.FILL_BOTH));
    }

    private void createSourceLabel(final Composite parent) {
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;

        sourceLabel = new Label(parent, SWT.HORIZONTAL | SWT.LEFT);
        sourceLabel.setLayoutData(gd);
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        final Collection<MessagePatch> patches = new ArrayList<MessagePatch>();

        for (final SourcePatch p : ((InternationalizeWizard) getWizard())
                .getPatches()) {
            if (p.isEnabled()) {
                for (final MessagePatch mp : p.getMessagePatches()) {
                    patches.add(mp);
                }
            }
        }

        return patches.toArray();
    }

    @Override
    public void addListener(final ILabelProviderListener listener) {
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false;
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {
    }

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        return (columnIndex == 0) ? getActionImage((MessagePatch) element)
                : null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        switch (columnIndex) {
        case 1:
            return ((MessagePatch) element).getKey();
        case 2:
            return ((MessagePatch) element).getValue();
        }
        return null;
    }

    @Override
    public void handleEvent(final Event event) {
        switch (event.type) {
        case SWT.Selection:
            if (event.widget == table) {
                updatePreview();
            } else if (event.widget instanceof Button) {
                changeAction((MessagePatch.Action) event.widget.getData());
            }
        }
    }

    @Override
    public boolean canModify(final Object element, final String property) {
        return property != COL_PROPS[0];
    }

    @Override
    public Object getValue(final Object element, final String property) {
        if (property == COL_PROPS[1]) {
            return ((MessagePatch) element).getKey();
        } else if (property == COL_PROPS[2]) {
            return ((MessagePatch) element).getValue();
        }
        return null;
    }

    @Override
    public void modify(final Object element, final String property,
            final Object value) {
        final MessagePatch p = (MessagePatch) ((TableItem) element).getData();

        if (property == COL_PROPS[1]) {
            final String k = value.toString();
            if (KEY_PATTERN.matcher(k).matches()) {
                p.setKey(k);
            } else {
                showErr(Msg.InvalidKeyValue.format(k));
                return;
            }
        } else if (property == COL_PROPS[2]) {
            p.setValue(value.toString());
        }

        tableViewer.refresh(p);
    }

    private void showErr(final String err) {
        // TODO: implement
    }

    private void changeAction(final MessagePatch.Action action) {
        for (final TableItem i : table.getSelection()) {
            final MessagePatch p = (MessagePatch) i.getData();
            p.setAction(action);
            i.setImage(0, getActionImage(p));
        }
    }

    private void updatePreview() {
        final TableItem[] selection = table.getSelection();

        if (selection.length > 0) {
            final TableItem i = selection[0];
            final MessagePatch p = (MessagePatch) i.getData();
            final MessagePatch oldPatch = selectedPatch;

            if (p != oldPatch) {
                final Message m = p.getMessage();
                selectedPatch = p;
                sourceLabel.setText(p.getSourcePatch().getMessageFinder()
                        .getCompilationUnit().getPath().toString());

                if ((oldPatch == null)
                        || (oldPatch.getSourcePatch() != p.getSourcePatch())) {
                    doc.set(p.getSourcePatch().getMessageFinder().getSource());
                    sourceViewer.setDocument(doc);
                }

                sourceViewer.setSelectedRange(m.getOffset(), m.getLength());
                sourceViewer.revealRange(m.getOffset(), m.getLength());
            }
        } else {
            selectedPatch = null;
            sourceLabel.setText(""); //$NON-NLS-1$
            doc.set(""); //$NON-NLS-1$
            sourceViewer.setDocument(doc);
        }

        ((Composite) getControl()).layout();
    }

    private static Image getActionImage(final MessagePatch p) {
        switch (p.getAction()) {
        case IGNORE:
            return NlsPlugin.getInstance().getImage(NlsPlugin.IMG_IGNORE);
        case EXTERNALIZE:
            return NlsPlugin.getInstance().getImage(NlsPlugin.IMG_EXTERNALIZE);
        case NON_NLS:
            return NlsPlugin.getInstance().getImage(NlsPlugin.IMG_NON_NLS);
        default:
            throw new IllegalStateException();
        }
    }
}
