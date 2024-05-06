package com.dlsc.gemsfx.skins;

import com.dlsc.gemsfx.ChipView;
import com.dlsc.gemsfx.FilterView;
import com.dlsc.gemsfx.FilterView.Filter;
import com.dlsc.gemsfx.SearchTextField;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterViewSkin<T> extends SkinBase<FilterView<T>> {

    private final SearchTextField searchTextField = new SearchTextField();
    private final HBox filterGroupsPane = new HBox();
    private final FlowPane filtersPane = new FlowPane();
    private final HBox headerBox = new HBox();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox container;

    public FilterViewSkin(FilterView<T> view) {
        super(view);

        InvalidationListener updateHeaderListener = it -> updateHeaderBox();
        view.titleLabelProperty().addListener(updateHeaderListener);
        view.titlePostfixLabelProperty().addListener(updateHeaderListener);
        view.subtitleLabelProperty().addListener(updateHeaderListener);

        view.extrasProperty().addListener((obs, oldExtras, newExtras) -> {
            if (oldExtras != null) {
                headerBox.getChildren().remove(oldExtras);
            }
            if (newExtras != null) {
                headerBox.getChildren().add(newExtras);
            }
        });

        filterGroupsPane.getStyleClass().add("filter-groups");
        filterGroupsPane.setFillHeight(true);
        filterGroupsPane.visibleProperty().bind(Bindings.isNotEmpty(view.getFilterGroups()));
        filterGroupsPane.managedProperty().bind(Bindings.isNotEmpty(view.getFilterGroups()));

        filtersPane.getStyleClass().add("filters");
        filtersPane.prefWrapLengthProperty().bind(view.widthProperty());
        filtersPane.visibleProperty().bind(Bindings.isNotEmpty(filtersPane.getChildren()));
        filtersPane.managedProperty().bind(Bindings.isNotEmpty(filtersPane.getChildren()));
        filtersPane.setMinHeight(Region.USE_PREF_SIZE);

        searchTextField.visibleProperty().bind(view.textFilterProviderProperty().isNotNull());
        searchTextField.managedProperty().bind(view.textFilterProviderProperty().isNotNull());
        searchTextField.textProperty().bindBidirectional(view.filterTextProperty());

        view.textFilterProviderProperty().addListener(it -> updateGroups());
        view.getFilterGroups().addListener((Observable it) -> updateGroups());

        view.getFilters().addListener((Observable it) -> updateFilters());
        view.filterTextProperty().addListener(it -> updateFilters());
        view.textFilterProviderProperty().addListener(it -> updateFilters());

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.managedProperty().bind(scrollPane.visibleProperty());
        scrollPane.visibleProperty().bind(Bindings.isNotEmpty(filtersPane.getChildren()));
        scrollPane.contentProperty().bind(
                Bindings.when(Bindings.size(view.filtersProperty()).greaterThan(view.scrollThresholdProperty()))
                        .then(filtersPane)
                        .otherwise((FlowPane) null));
        view.scrollThresholdProperty().addListener(it -> updateFilters());

        container = new VBox(headerBox, filterGroupsPane, filtersPane);
        container.getStyleClass().add("filter-container");
        container.setFillWidth(true);
        getChildren().add(container);

        updateGroups();
        updateFilters();

        getSkinnable().getFilters().addListener((Observable it) -> Platform.runLater(() -> {
            for (Filter filter : filterItemMap.keySet()) {
                CheckMenuItem menuItem = filterItemMap.get(filter);
                menuItem.setSelected(getSkinnable().getFilters().contains(filter));
            }
        }));

        headerBox.setFillHeight(true);
        headerBox.getStyleClass().add("header-box");
        headerBox.visibleProperty().bind(view.showHeaderProperty());
        headerBox.managedProperty().bind(view.showHeaderProperty());

        updateHeaderBox();
    }

    private void updateHeaderBox() {
        FilterView<T> view = getSkinnable();

        Label titleLabel = view.getTitleLabel();
        titleLabel.textProperty().bind(view.titleProperty());
        titleLabel.getStyleClass().add("title");

        Label titlePostfixLabel = view.getTitlePostfixLabel();
        titlePostfixLabel.textProperty().bind(view.titlePostfixProperty());
        titlePostfixLabel.getStyleClass().addAll("title", "title-postfix");

        HBox titleBox = new HBox(titleLabel, titlePostfixLabel);
        titleBox.getStyleClass().add("title-box");

        Label subtitleLabel = view.getSubtitleLabel();
        subtitleLabel.textProperty().bind(view.subtitleProperty());
        subtitleLabel.getStyleClass().add("subtitle");

        VBox titleAndSubtitleBox = new VBox(titleBox, subtitleLabel);
        titleAndSubtitleBox.getStyleClass().add("title-subtitle-box");

        HBox.setHgrow(titleAndSubtitleBox, Priority.ALWAYS);

        if (view.getExtras() != null) {
            headerBox.getChildren().setAll(titleAndSubtitleBox, searchTextField, view.getExtras());
        } else {
            headerBox.getChildren().setAll(titleAndSubtitleBox, searchTextField);
        }
    }

    private final Map<Filter, CheckMenuItem> filterItemMap = new HashMap<>();

    private void updateGroups() {
        filterGroupsPane.getChildren().clear();

        filterItemMap.clear();

        getSkinnable().getFilterGroups().forEach(group -> {
            MenuButton menuButton = new MenuButton();
            menuButton.textProperty().bind(group.nameProperty());
            menuButton.setMaxWidth(Double.MAX_VALUE);
            menuButton.setMaxHeight(Double.MAX_VALUE);

            HBox.setHgrow(menuButton, Priority.ALWAYS);

            MenuItem all = new MenuItem("All");
            ObservableList<Filter<T>> activeFilters = getSkinnable().getFilters();

            all.setOnAction(evt -> {
                // first remove all, otherwise we end up with duplicates
                activeFilters.removeAll(group.getFilters());
                activeFilters.addAll(group.getFilters());
            });

            MenuItem none = new MenuItem("None");
            none.setOnAction(evt -> {
                // first remove all, otherwise we end up with duplicates
                activeFilters.removeAll(group.getFilters());
            });

            menuButton.getItems().addAll(all, none, new SeparatorMenuItem());

            List<CheckMenuItem> items = new ArrayList<>();
            group.getFilters().forEach(filter -> {
                CheckMenuItem item = new CheckMenuItem();
                item.textProperty().bind(filter.nameProperty());
                item.selectedProperty().addListener(it -> {
                    if (item.isSelected()) {
                        if (!activeFilters.contains(filter)) {
                            activeFilters.add(filter);
                        }
                    } else {
                        activeFilters.remove(filter);
                    }
                });
                item.setSelected(filter.isSelected());
//                menuButton.getItems().add(item);
                items.add(item);

                filterItemMap.put(filter, item);
            });
            menuButton.getItems().addAll(items);

            filterGroupsPane.getChildren().add(menuButton);
        });

        // special style tag for last menu button
        if (!filterGroupsPane.getChildren().isEmpty()) {
            filterGroupsPane.getChildren().get(filterGroupsPane.getChildren().size() - 1).getStyleClass().add("last");
        }
    }

    private void updateFilters() {
        filtersPane.getChildren().clear();

        FilterView<T> filterView = getSkinnable();

        ObservableList<Filter<T>> filters = filterView.getFilters();
        if (!filters.isEmpty() || StringUtils.isNotBlank(filterView.getFilterText())) {
            updateFiltersWrapper();

            filters.forEach(f -> {
                ChipView<Filter> chipView = new ChipView<>();
                chipView.setValue(f);
                chipView.textProperty().bind(f.nameProperty());
                chipView.setOnClose(filter -> filters.remove(filter));
                filtersPane.getChildren().add(chipView);
            });

            String filterText = filterView.getFilterText();
            if (StringUtils.isNotBlank(filterText)) {
                ChipView<String> chipView = new ChipView<>();
                chipView.setValue(filterView.getFilterText());
                chipView.setText("\"" + filterView.getFilterText() + "\"");
                chipView.setOnClose(filter -> filterView.setFilterText(null));
                filtersPane.getChildren().add(chipView);
            }

            Label clearFilter = new Label("清除");
            clearFilter.getStyleClass().add("clear-filter-label");
            clearFilter.setOnMouseClicked(evt -> {
                filters.clear();
                filterView.setFilterText(null);
            });

            filtersPane.getChildren().add(clearFilter);
        }
    }

    private void updateFiltersWrapper() {
        FilterView<T> filterView = getSkinnable();
        // use scrollPane if there is too many filters
        if (filterView.getFilters().size() > filterView.getScrollThreshold()) {
            container.getChildren().remove(filtersPane);
            if (!container.getChildren().contains(scrollPane)) {
                container.getChildren().add(scrollPane);
            }
        } else {
            container.getChildren().remove(scrollPane);
            if (!container.getChildren().contains(filtersPane)) {
                container.getChildren().add(filtersPane);
            }
        }
    }

}
