/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Xavier Raynaud - add cut/copy/paste/dnd support
 *   Vincent Perot - Add subfield filtering
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.dialog;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.FilterTreeContentProvider;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.FilterTreeLabelProvider;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.ITmfFilterTreeNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.SegmentFilterMatchesNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterAndNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterAspectNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterOrNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterRootNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.filter.model.TmfFilterTreeNode;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

class FilterViewer extends Composite {


    private static class AspectItem {
        public String label;
        public @Nullable ISegmentAspect eventAspect;

        public AspectItem(String label, ISegmentAspect eventAspect) {
            this.label = label;
            this.eventAspect = eventAspect;
        }
    }

    private TreeViewer fViewer;

    private Composite fComposite;
    @Nullable private MenuManager fMenuManager;

    private boolean fIsDialog = false;
    @Nullable private List<ISegmentAspect> fAspects;

    public FilterViewer(Composite parent, int style) {
        this(parent, style, false, null);
    }

    public FilterViewer(Composite parent, int style, boolean isDialog, @Nullable List<ISegmentAspect> aspects) {
        super(parent, style);

        this.fIsDialog = isDialog;
        fAspects = aspects;

        setLayout(new FillLayout());
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        setLayoutData(gd);

        final SashForm sash = new SashForm(this, SWT.HORIZONTAL);

        // Create the tree viewer to display the filter tree
        fViewer = new TreeViewer(sash, SWT.NONE);
        fViewer.setContentProvider(new FilterTreeContentProvider());
        fViewer.setLabelProvider(new FilterTreeLabelProvider());
        fViewer.setInput(new TmfFilterRootNode());

        // Create the empty filter node properties panel
        fComposite = new Composite(sash, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        fComposite.setLayout(gl);

        createContextMenu();

        fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(@Nullable SelectionChangedEvent event) {
                @NonNull SelectionChangedEvent e = checkNotNull(event);
                if (!e.getSelection().isEmpty() && e.getSelection() instanceof IStructuredSelection) {
                    // Update the filter node properties panel to the selection
                    IStructuredSelection selection = (IStructuredSelection) e.getSelection();
                    ITmfFilterTreeNode node = (ITmfFilterTreeNode) selection.getFirstElement();
                    updateFilterNodeComposite(node);
                    // Highlight the selection's children
                    highlightTreeItems(fViewer.getTree().getSelection()[0].getItems());
                } else {
                    updateFilterNodeComposite(null);
                }
            }
        });

        fViewer.getTree().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                TmfFilterTreeNode root = (TmfFilterTreeNode) fViewer.getInput();
                if ((root == null || root.getChildrenCount() == 0) && e != null) {
                    checkNotNull(e.gc).setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                    e.gc.drawText(Messages.FilterViewer_EmptyTreeHintText, 5, 0);
                }
            }
        });
    }

    /**
     * Create the context menu for the tree viewer
     */
    private void createContextMenu() {
        // Adds root context menu
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(@Nullable IMenuManager manager) {
                fillContextMenu(checkNotNull(manager));
            }
        });

        fMenuManager = menuManager;
        // Context
        Menu contextMenu = fMenuManager.createContextMenu(fViewer.getTree());

        // Publish it
        fViewer.getTree().setMenu(contextMenu);
    }

    public MenuManager getMenuManager() {
        return checkNotNull(fMenuManager);
    }

    /**
     * Fill the context menu for the tree viewer.
     *
     * @param manager
     *            The menu manager
     */
    protected void fillContextMenu(IMenuManager manager) {
        final ISelection selection = fViewer.getSelection();
        ITmfFilterTreeNode filterTreeNode = null;
        if (selection instanceof StructuredSelection) {
            Object element = ((StructuredSelection) selection).getFirstElement();
            if (element instanceof ITmfFilterTreeNode) {
                filterTreeNode = (ITmfFilterTreeNode) element;
            }
        }

        final ITmfFilterTreeNode selectedNode = filterTreeNode;
        if (selectedNode != null) {
            fillContextMenuForNode(selectedNode, manager);
        }

        manager.add(new Separator("delete")); //$NON-NLS-1$

        if (fIsDialog && (selectedNode != null)) {
            Action deleteAction = new Action(Messages.FilterViewer_DeleteActionText) {
                @Override
                public void run() {
                    selectedNode.remove();
                    fViewer.refresh();
                }
            };
            deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
            manager.add(deleteAction);
        }
        manager.add(new Separator("edit")); //$NON-NLS-1$

        if (fViewer.getInput() instanceof TmfFilterRootNode || filterTreeNode == null) {
            manager.add(new Separator());
            ITmfFilterTreeNode root = (ITmfFilterTreeNode) fViewer.getInput();
            fillContextMenuForNode(root, manager);
        }
    }

    /**
     * Fill the context menu with the valid children of the provided node
     *
     * @param node
     *            The target node
     * @param manager
     *            The menu manager
     */
    protected void fillContextMenuForNode(final ITmfFilterTreeNode node, IMenuManager manager) {
        for (final String child : node.getValidChildren()) {
            final Action action = new Action() {
                @Override
                public void run() {
                    ITmfFilterTreeNode newNode = null;
                    if (TmfFilterNode.NODE_NAME.equals(child)) {
                        newNode = new TmfFilterNode(node, ""); //$NON-NLS-1$
                    } else if (TmfFilterAndNode.NODE_NAME.equals(child)) {
                        newNode = new TmfFilterAndNode(node);
                    } else if (TmfFilterOrNode.NODE_NAME.equals(child)) {
                        newNode = new TmfFilterOrNode(node);
                    } else if (SegmentFilterMatchesNode.NODE_NAME.equals(child)) {
                        newNode = new SegmentFilterMatchesNode(node);
                    }
                    if (newNode != null) {
                        fViewer.refresh();
                        fViewer.setSelection(new StructuredSelection(newNode), true);
                    }
                }
            };
            if (TmfFilterNode.NODE_NAME.equals(child)) {
                action.setText(Messages.FilterViewer_NewPrefix + " " + child); //$NON-NLS-1$
            } else {
                action.setText(child);
            }
            manager.add(action);
        }
    }

    /**
     * Create the appropriate filter node properties composite
     */
    private void updateFilterNodeComposite(@Nullable ITmfFilterTreeNode node) {
        for (Control control : fComposite.getChildren()) {
            control.dispose();
        }

        if (node instanceof TmfFilterNode) {
            new FilterNodeComposite(fComposite, (TmfFilterNode) node);
        } else if (node instanceof TmfFilterAndNode) {
            new FilterAndNodeComposite(fComposite, (TmfFilterAndNode) node);
        } else if (node instanceof TmfFilterOrNode) {
            new FilterOrNodeComposite(fComposite, (TmfFilterOrNode) node);
        } else if (node instanceof SegmentFilterMatchesNode) {
            new FilterMatchesNodeComposite(fComposite, (SegmentFilterMatchesNode) node);
        } else {
            new FilterBaseNodeComposite(fComposite);
        }
        fComposite.layout();
    }

    /**
     * Highlight the provided tree items
     */
    private void highlightTreeItems(TreeItem[] items) {
        resetTreeItems(fViewer.getTree().getItems());
        for (TreeItem item : items) {
            item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
        }

    }

    /**
     * Reset the provided tree items (remove highlight)
     */
    private void resetTreeItems(TreeItem[] items) {
        for (TreeItem item : items) {
            item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            resetTreeItems(item.getItems());
        }
    }

    public void setInput(ITmfFilterTreeNode root) {
        fViewer.setInput(root);
        fViewer.expandAll();

        updateFilterNodeComposite(null);
    }

    public ITmfFilterTreeNode getInput() {
        return (ITmfFilterTreeNode) fViewer.getInput();
    }

    public void refresh() {
        fViewer.refresh();
    }

    public void setSelection(ITmfFilterTreeNode node, boolean reveal) {
        fViewer.setSelection(new StructuredSelection(node), reveal);
    }

    public void setSelection(ITmfFilterTreeNode node) {
        fViewer.setSelection(new StructuredSelection(node));
    }

    public ITmfFilterTreeNode getSelection() {
        final ISelection selection = fViewer.getSelection();
        ITmfFilterTreeNode filterTreeNode = null;
        if (selection instanceof StructuredSelection) {
            Object element = ((StructuredSelection) selection).getFirstElement();
            if (element instanceof ITmfFilterTreeNode) {
                filterTreeNode = (ITmfFilterTreeNode) element;
            }
        }

        final ITmfFilterTreeNode selectedNode = filterTreeNode;
        return selectedNode;
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        fViewer.addSelectionChangedListener(listener);
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        fViewer.removeSelectionChangedListener(listener);
    }

    @Override
    public boolean setFocus() {
        return fViewer.getControl().setFocus();
    }

    /**
     * @return whether the tree is in focus or not
     */
    public boolean isTreeInFocus() {
        return fViewer.getControl().isFocusControl();
    }

    /**
     * Gets the TreeViewer displaying filters
     *
     * @return a {@link TreeViewer}
     */
    TreeViewer getTreeViewer() {
        return fViewer;
    }

    private class FilterBaseNodeComposite extends Composite {

        FilterBaseNodeComposite(Composite parent) {
            super(parent, SWT.NONE);
            setLayout(new GridLayout(2, false));
            setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        }
    }

    private abstract class FilterAspectNodeComposite extends FilterBaseNodeComposite {
        TmfFilterAspectNode fAspectNode;
        @Nullable Combo fAspectCombo;
        @Nullable Label fFieldLabel;
        @Nullable Text fFieldText;
        @Nullable List<AspectItem> fAspectList = null;

        FilterAspectNodeComposite(Composite parent, TmfFilterAspectNode node) {
            super(parent);
            fAspectNode = node;
        }

        protected void createAspectControls() {
            Label label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText(Messages.FilterViewer_AspectLabel);

            List<@NonNull AspectItem> aspectList = getAspectList();

            Combo aspectCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            aspectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            for (AspectItem aspect : aspectList) {
                aspectCombo.add(aspect.label);
            }
            @Nullable ISegmentAspect eventAspect = fAspectNode.getEventAspect();
            if (eventAspect != null) {
                for (int i = 0; i < aspectList.size(); i++) {
                    AspectItem aspect = aspectList.get(i);
                    @Nullable ISegmentAspect eventAspect2 = aspect.eventAspect;
                    if (eventAspect2 != null &&
                            eventAspect.getName().equals(eventAspect2.getName())) {
                        aspectCombo.select(i);
                        if (eventAspect instanceof TmfEventFieldAspect) {
                            aspect.eventAspect = eventAspect;
                            createFieldControls((TmfEventFieldAspect) eventAspect, aspect);
                        }
                        break;
                    }
                }
            }
            aspectCombo.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(@Nullable ModifyEvent e) {
                    int selection = aspectCombo.getSelectionIndex();
                    AspectItem aspect = null;
                    if (selection != -1) {
                        aspect = aspectList.get(aspectCombo.getSelectionIndex());
                        @NonNull ISegmentAspect eventAspect2 = checkNotNull(aspect.eventAspect);
                        fAspectNode.setEventAspect(eventAspect2);
                    } else {
                        fAspectNode.setEventAspect(null);
                    }
                    if (eventAspect instanceof TmfEventFieldAspect) {
                        TmfEventFieldAspect eventFieldAspect = (TmfEventFieldAspect) eventAspect;
                        createFieldControls(eventFieldAspect, aspect);
                        layout();
                    } else if (fFieldLabel != null && fFieldText != null) {
                        fFieldLabel.dispose();
                        fFieldLabel = null;
                        checkNotNull(fFieldText).dispose();
                        fFieldText = null;
                        layout();
                    }
                    fViewer.refresh(fAspectNode);
                }
            });

            fAspectList = aspectList;
            fAspectList.toString();
            fAspectCombo = aspectCombo;
        }

        private void createFieldControls(final TmfEventFieldAspect eventFieldAspect, final AspectItem aspect) {
            if (fFieldLabel != null) {
                fFieldLabel.dispose();
            }
            Label label = new Label(this, SWT.NONE);
            label.moveBelow(fAspectCombo);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText(Messages.FilterViewer_FieldLabel);
            fFieldLabel = label;

            if (fFieldText != null) {
                fFieldText.dispose();
            }
            Text text = new Text(this, SWT.BORDER);
            text.moveBelow(fFieldLabel);
            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            text.setToolTipText(Messages.FilterViewer_Subfield_ToolTip);
            if (eventFieldAspect.getFieldPath() != null) {
                text.setText(eventFieldAspect.getFieldPath());
            }
            if (text.getText().length() == 0) {
                text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                text.setText(Messages.FilterViewer_FieldHint);
            }
            text.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(@Nullable FocusEvent e) {
                    if (text.getText().length() == 0) {
                        text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                        text.setText(Messages.FilterViewer_FieldHint);
                    }
                }

                @Override
                public void focusGained(@Nullable FocusEvent e) {
                    if (text.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
                        text.setText(""); //$NON-NLS-1$
                    }
                    text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                }
            });
            text.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(@Nullable ModifyEvent e) {
                    if (!text.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
//                        if (text.getText().isEmpty()) {
//                            fAspectNode.setEventAspect(eventFieldAspect.forField(null));
//                        } else {
//                            fAspectNode.setEventAspect(eventFieldAspect.forField(fFieldText.getText()));
//                        }
                        aspect.eventAspect = fAspectNode.getEventAspect();
                        fViewer.refresh(fAspectNode);
                    }
                }
            });
            fFieldText = text;
        }


        private List<AspectItem> getAspectList() {
            ArrayList<AspectItem> aspectList = new ArrayList<>();

            if (fAspects != null) {
                for (ISegmentAspect aspect : fAspects) {
                    aspectList.add(new AspectItem(aspect.getName(), aspect));
                }
            }
            return aspectList;
        }
    }

    private class FilterNodeComposite extends FilterBaseNodeComposite {
        TmfFilterNode fNode;
        Text fNameText;

        FilterNodeComposite(Composite parent, TmfFilterNode node) {
            super(parent);
            fNode = node;

            Label label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText(Messages.FilterViewer_NameLabel);

            fNameText = new Text(this, SWT.BORDER);
            fNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            if (node.getFilterName() != null && checkNotNull(node.getFilterName()).length() > 0) {
                fNameText.setText(node.getFilterName());
            } else {
                fNameText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                fNameText.setText(Messages.FilterViewer_FilterNameHint);
            }
            fNameText.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(@Nullable FocusEvent e) {
                    if (fNode.getFilterName() == null || checkNotNull(fNode.getFilterName()).length() == 0) {
                        fNameText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                        fNameText.setText(Messages.FilterViewer_FilterNameHint);
                    }
                }

                @Override
                public void focusGained(@Nullable FocusEvent e) {
                    if (fNameText.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
                        fNameText.setText(""); //$NON-NLS-1$
                    }
                    fNameText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                }
            });
            fNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(@Nullable ModifyEvent e) {
                    if (!fNameText.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
                        fNode.setFilterName(fNameText.getText());
                        fViewer.refresh(fNode);
                    }
                }
            });
        }
    }

private class FilterAndNodeComposite extends FilterBaseNodeComposite {
        TmfFilterAndNode fNode;
        Button fNotButton;

        FilterAndNodeComposite(Composite parent, TmfFilterAndNode node) {
            super(parent);
            fNode = node;

            Label label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText("not");

            fNotButton = new Button(this, SWT.CHECK);
            fNotButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            fNotButton.setSelection(fNode.isNot());
            fNotButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    fNode.setNot(fNotButton.getSelection());
                    fViewer.refresh(fNode);
                }
            });
        }
    }

    private class FilterOrNodeComposite extends FilterBaseNodeComposite {
        TmfFilterOrNode fNode;
        Button fNotButton;

        FilterOrNodeComposite(Composite parent, TmfFilterOrNode node) {
            super(parent);
            fNode = node;

            Label label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText("not");

            fNotButton = new Button(this, SWT.CHECK);
            fNotButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            fNotButton.setSelection(fNode.isNot());
            fNotButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    fNode.setNot(fNotButton.getSelection());
                    fViewer.refresh(fNode);
                }
            });
        }
    }

    private class FilterMatchesNodeComposite extends FilterAspectNodeComposite {
        SegmentFilterMatchesNode fNode;
        Button fNotButton;
        Text fRegexText;

        FilterMatchesNodeComposite(Composite parent, SegmentFilterMatchesNode node) {
            super(parent, node);
            fNode = node;

            Label label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText(Messages.FilterViewer_NotLabel);

            fNotButton = new Button(this, SWT.CHECK);
            fNotButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            fNotButton.setSelection(fNode.isNot());
            fNotButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(@Nullable SelectionEvent e) {
                    fNode.setNot(fNotButton.getSelection());
                    fViewer.refresh(fNode);
                }
            });

            createAspectControls();

            label = new Label(this, SWT.NONE);
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            label.setText(Messages.FilterViewer_RegexLabel);

            fRegexText = new Text(this, SWT.BORDER);
            fRegexText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            if (node.getRegex().length() > 0) {
                fRegexText.setText(node.getRegex());
            } else {
                fRegexText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                fRegexText.setText(Messages.FilterViewer_RegexHint);
            }
            fRegexText.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(@Nullable FocusEvent e) {
                    if (fNode.getRegex().length() == 0) {
                        fRegexText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                        fRegexText.setText(Messages.FilterViewer_RegexHint);
                    }
                }

                @Override
                public void focusGained(@Nullable FocusEvent e) {
                    if (fRegexText.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
                        fRegexText.setText(""); //$NON-NLS-1$
                    }
                    fRegexText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
                }
            });
            fRegexText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(@Nullable ModifyEvent e) {
                    if (!fRegexText.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY))) {
                        fNode.setRegex(fRegexText.getText());
                        fViewer.refresh(fNode);
                    }
                }
            });
        }
    }
}
