/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.reactfx.EventStreams;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.lang.java.ast.TypeNode;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;
import net.sourceforge.pmd.util.fxdesigner.model.MetricEvaluator;
import net.sourceforge.pmd.util.fxdesigner.model.MetricResult;
import net.sourceforge.pmd.util.fxdesigner.util.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.util.IteratorUtil;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeCell;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeItem;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


/**
 * Controller of the node info panel (left).
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeInfoPanelController extends AbstractController {

    private final MainDesignerController parent;

    /** List of attribute names that are ignored if {@link #isShowAllAttributes()} is false. */
    private static final List<String> IGNORABLE_ATTRIBUTES =
        Arrays.asList("BeginLine", "EndLine", "BeginColumn", "EndColumn", "FindBoundary", "SingleLine");

    @FXML
    private ToolbarTitledPane metricsTitledPane;
    @FXML
    private CheckMenuItem showAllAttributesMenuItem;
    @FXML
    private TabPane nodeInfoTabPane;
    @FXML
    private Tab xpathAttributesTab;
    @FXML
    private ListView<String> xpathAttributesListView;
    @FXML
    private Tab metricResultsTab;
    @FXML
    private ListView<MetricResult> metricResultsListView;
    @FXML
    private TreeView<Object> scopeHierarchyTreeView;
    private MetricEvaluator metricEvaluator = new MetricEvaluator();
    private Node selectedNode;

    public NodeInfoPanelController(MainDesignerController mainController) {
        parent = mainController;
    }

    @Override
    protected void beforeParentInit() {

        xpathAttributesListView.setPlaceholder(new Label("No available attributes"));

        EventStreams.valuesOf(scopeHierarchyTreeView.getSelectionModel().selectedItemProperty())
                    .filter(Objects::nonNull)
                    .map(TreeItem::getValue)
                    .filterMap(o -> o instanceof NameDeclaration, o -> (NameDeclaration) o)
                    .subscribe(declaration -> {
                        Platform.runLater(() -> setFocusNode(declaration.getNode(), true));
                        parent.onNameDeclarationSelected(declaration);
                    });

        scopeHierarchyTreeView.setCellFactory(view -> new ScopeHierarchyTreeCell());

        showAllAttributesProperty()
            .values()
            .distinct()
            .subscribe(show -> displayAttributes(selectedNode));

    }


    /**
     * Displays info about a node. If null, the panels are reset.
     *
     * @param node Node to inspect
     */
    public void setFocusNode(Node node) {
        setFocusNode(node, false);
    }


    private void setFocusNode(Node node, boolean focusScopeView) {
        if (node == null) {
            invalidateInfo();
            return;
        }

        if (node.equals(selectedNode)) {
            return;
        }
        selectedNode = node;

        displayAttributes(node);
        displayMetrics(node);
        displayScopes(node, focusScopeView);
    }

    private void displayAttributes(Node node) {
        ObservableList<String> atts = getAttributes(node);
        xpathAttributesListView.setItems(atts);
    }


    private void displayMetrics(Node node) {
        ObservableList<MetricResult> metrics = evaluateAllMetrics(node);
        metricResultsListView.setItems(metrics);
        notifyMetricsAvailable(metrics.stream()
                                      .map(MetricResult::getValue)
                                      .filter(result -> !result.isNaN())
                                      .count());
    }


    private void displayScopes(Node node, boolean focusScopeView) {

        // current selection
        TreeItem<Object> previousSelection = scopeHierarchyTreeView.getSelectionModel().getSelectedItem();

        ScopeHierarchyTreeItem rootScope = ScopeHierarchyTreeItem.buildAscendantHierarchy(node);
        scopeHierarchyTreeView.setRoot(rootScope);

        if (focusScopeView && previousSelection != null) {
            int maxDepth = IteratorUtil.count(IteratorUtil.parentIterator(previousSelection, true));

            rootScope.tryFindNode(previousSelection.getValue(), maxDepth)
                     .ifPresent(scopeHierarchyTreeView.getSelectionModel()::select);
        }
    }

    /**
     * Invalidates the info being displayed.
     */
    private void invalidateInfo() {
        metricResultsListView.setItems(FXCollections.emptyObservableList());
        xpathAttributesListView.setItems(FXCollections.emptyObservableList());
        scopeHierarchyTreeView.setRoot(null);
    }


    private void notifyMetricsAvailable(long numMetrics) {
        metricResultsTab.setText("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + ")");
        metricsTitledPane.setTitle("Metrics\t(" + (numMetrics == 0 ? "none" : numMetrics) + " available)");
        metricResultsTab.setDisable(numMetrics == 0);
    }


    private ObservableList<MetricResult> evaluateAllMetrics(Node n) {
        try {
            return FXCollections.observableArrayList(metricEvaluator.evaluateAllMetrics(n));
        } catch (UnsupportedOperationException e) {
            return FXCollections.emptyObservableList();
        }
    }


    @PersistentProperty
    public boolean isShowAllAttributes() {
        return showAllAttributesMenuItem.isSelected();
    }


    public void setShowAllAttributes(boolean bool) {
        showAllAttributesMenuItem.setSelected(bool);
    }


    public Var<Boolean> showAllAttributesProperty() {
        return Var.fromVal(showAllAttributesMenuItem.selectedProperty(), showAllAttributesMenuItem::setSelected);
    }


    /**
     * Gets the XPath attributes of the node for display within a listview.
     */
    private ObservableList<String> getAttributes(Node node) {
        if (node == null) {
            return FXCollections.emptyObservableList();
        }

        ObservableList<String> result = FXCollections.observableArrayList();
        Iterator<Attribute> attributeAxisIterator = node.getXPathAttributesIterator();
        while (attributeAxisIterator.hasNext()) {
            Attribute attribute = attributeAxisIterator.next();

            if (isShowAllAttributes() || !IGNORABLE_ATTRIBUTES.contains(attribute.getName())) {
                // TODO the display should be handled in a ListCell
                result.add(attribute.getName() + " = "
                               + ((attribute.getValue() != null) ? attribute.getStringValue() : "null"));
            }
        }

        if (node instanceof TypeNode) {
            result.add("typeIs() = " + ((TypeNode) node).getType());
        }
        Collections.sort(result);
        return result;
    }
}
