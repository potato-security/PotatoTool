package com.dlsc.gemsfx;

import com.dlsc.gemsfx.skins.FilterViewSkin;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A view for presenting a set of predefined filter groups, each one with a list of filters.
 * The user can select one or more filters from each group. Elements found in the resulting
 * filtered list have to match ALL filters. The selected filters will be shown as "chips"
 * (see @link {@link ChipView}).
 * <p>
 * Items can be added via the {@link #getItems()} list. Table or list views have to use
 * the {@link #getFilteredItems()} list. This filtered list can also be wrapped via a
 * {@link SortedList} and then added to a table or list view.
 * </p>
 * <p>
 * An input field for filtering based on text input will appear as soon as a text filter provider
 * has been defined. See {@link #setTextFilterProvider(Callback)}.
 * </p>
 * <p>
 * Applications with additional filtering needs can utilize the {@link #additionalFilterPredicateProperty()}.
 * </p>
 *
 * @param <T> the type of the model objects filtered by the view
 */
public class FilterView<T> extends Control {

    public FilterView() {
        getStyleClass().add("filter-view");

        setFocusTraversable(false);

        InvalidationListener updatePredicateListener = (Observable it) -> {

            // first group the filters together according to the group to which they belong

            Map<FilterGroup<T>, List<Filter<T>>> map = new HashMap<>();
            for (Filter<T> filter : getFilters()) {
                for (FilterGroup<T> group : getFilterGroups()) {
                    if (group.getFilters().contains(filter)) {
                        map.computeIfAbsent(group, key -> new ArrayList<>()).add(filter);
                    }
                }
            }

            // now create the predicate. filters of same group use OR, else ANDs

            Predicate<T> predicate = item -> true; // "and" chain starts with a true

            for (FilterGroup<T> group : map.keySet()) {
                Predicate<T> groupPredicate = item -> false; // or chain starts with a false
                for (Filter<T> filter : map.get(group)) {
                    groupPredicate = groupPredicate.or(filter);
                }

                predicate = predicate.and(groupPredicate);
            }

            Callback<String, Predicate<T>> textFilterProvider = getTextFilterProvider();
            if (textFilterProvider != null && StringUtils.isNotBlank(getFilterText())) {
                predicate = predicate.and(textFilterProvider.call(getFilterText().toLowerCase()));
            }

            filterPredicate.set(predicate.and(getAdditionalFilterPredicate()));
        };

        filters.addListener(updatePredicateListener);
        textFilterProviderProperty().addListener(updatePredicateListener);
        filterTextProperty().addListener(updatePredicateListener);
        additionalFilterPredicateProperty().addListener(updatePredicateListener);

        items.addListener((Observable it) -> {
            FilteredList<T> filteredList = new FilteredList<>(getItems());
            filteredList.predicateProperty().bind(filterPredicateProperty());
            filteredItems.set(filteredList);
        });
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(FilterView.class.getResource("/css/filter-view.css")).toExternalForm();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new FilterViewSkin<>(this);
    }

    private final IntegerProperty scrollThreshold = new SimpleIntegerProperty(this, "scrollThreshold", 100);

    public int getScrollThreshold() {
        return scrollThreshold.get();
    }

    /**
     * The threshold number of filters at which a ScrollPane is introduced to handle large number of elements.
     * If the number of filters is equal to or exceeds this value, the filters will be displayed within a ScrollPane.
     */
    public IntegerProperty scrollThresholdProperty() {
        return scrollThreshold;
    }

    public void setScrollThreshold(int scrollThreshold) {
        this.scrollThreshold.set(scrollThreshold);
    }

    private final ObjectProperty<Label> titleLabel = new SimpleObjectProperty<>(this, "titleLabel", new Label());

    public final Label getTitleLabel() {
        return titleLabel.get();
    }

    /**
     * The label instance that will be used for displaying the title of the view.
     */
    public final ObjectProperty<Label> titleLabelProperty() {
        return titleLabel;
    }

    public final void setTitleLabel(Label titleLabel) {
        this.titleLabel.set(titleLabel);
    }

    private final ObjectProperty<Label> titlePostfixLabel = new SimpleObjectProperty<>(this, "titlePostfixLabel", new Label());

    public final Label getTitlePostfixLabel() {
        return titlePostfixLabel.get();
    }

    /**
     * The label instance that will be used for displaying the title postfix text of the view.
     */
    public final ObjectProperty<Label> titlePostfixLabelProperty() {
        return titlePostfixLabel;
    }

    public final void setTitlePostfixLabel(Label titlePostfixLabel) {
        this.titlePostfixLabel.set(titlePostfixLabel);
    }

    private final ObjectProperty<Label> subtitleLabel = new SimpleObjectProperty<>(this, "subtitleLabel", new Label());

    public Label getSubtitleLabel() {
        return subtitleLabel.get();
    }

    /**
     * The label instance that will be used for displaying the subtitle of the view.
     */
    public ObjectProperty<Label> subtitleLabelProperty() {
        return subtitleLabel;
    }

    public void setSubtitleLabel(Label subtitleLabel) {
        this.subtitleLabel.set(subtitleLabel);
    }

    private final BooleanProperty showHeader = new SimpleBooleanProperty(this, "showHeader", true);

    public final boolean isShowHeader() {
        return showHeader.get();
    }

    /**
     * A flag to control whether the title, subtitle, and the search field
     * will be shown or not.
     *
     * @return true if the header will be shown (default is "true")
     */
    public final BooleanProperty showHeaderProperty() {
        return showHeader;
    }

    public final void setShowHeader(boolean showHeader) {
        this.showHeader.set(showHeader);
    }

    private final ObjectProperty<Node> extras = new SimpleObjectProperty<>(this, "extras");

    public final Node getExtras() {
        return extras.get();
    }

    /**
     * An extra node that can be added to the header of the filter view. The node will appear
     * to the right of the (optional) text filter field.
     *
     * @return the extra node
     */
    public final ObjectProperty<Node> extrasProperty() {
        return extras;
    }

    public final void setExtras(Node extras) {
        this.extras.set(extras);
    }

    private final StringProperty title = new SimpleStringProperty(this, "title", "Untitled");

    public final String getTitle() {
        return title.get();
    }

    /**
     * The title for the filter view
     *
     * @return the title text
     */
    public final StringProperty titleProperty() {
        return title;
    }

    public final void setTitle(String title) {
        this.title.set(title);
    }

    private final StringProperty titlePostfix = new SimpleStringProperty(this, "titlePostfix", "");

    public final String getTitlePostfix() {
        return titlePostfix.get();
    }

    /**
     * A text that can be added to the title text.
     *
     * @return the title postfix text
     */
    public final StringProperty titlePostfixProperty() {
        return titlePostfix;
    }

    public final void setTitlePostfix(String titlePostfix) {
        this.titlePostfix.set(titlePostfix);
    }

    private final StringProperty subtitle = new SimpleStringProperty(this, "subtitle", "");

    public final String getSubtitle() {
        return subtitle.get();
    }

    /**
     * The subtitle text for the filter view.
     *
     * @return the subtitle text
     */
    public final StringProperty subtitleProperty() {
        return subtitle;
    }

    public final void setSubtitle(String subtitle) {
        this.subtitle.set(subtitle);
    }

    private final ListProperty<T> items = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());

    public final ObservableList<T> getItems() {
        return items.get();
    }

    /**
     * The list of items that will be managed by the view.
     *
     * @return the model
     */
    public final ListProperty<T> itemsProperty() {
        return items;
    }

    public final void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    private final ReadOnlyListWrapper<T> filteredItems = new ReadOnlyListWrapper<>();

    public final ReadOnlyListProperty<T> filteredItemsProperty() {
        return filteredItems.getReadOnlyProperty();
    }

    public final ObservableList<T> getFilteredItems() {
        return filteredItems.getReadOnlyProperty();
    }

    // text filter

    private final StringProperty filterText = new SimpleStringProperty(this, "filterText");

    public final String getFilterText() {
        return filterText.get();
    }

    /**
     * Stores the filter text entered by the user inside the filter text field.
     *
     * @return the filter text
     * @see #setTextFilterProvider(Callback)
     */
    public final StringProperty filterTextProperty() {
        return filterText;
    }

    public final void setFilterText(String filterText) {
        this.filterText.set(filterText);
    }

    // text filter predicate provider

    private final ObjectProperty<Callback<String, Predicate<T>>> textFilterProvider = new SimpleObjectProperty<>(this, "textFilterProvider");

    public final Callback<String, Predicate<T>> getTextFilterProvider() {
        return textFilterProvider.get();
    }

    /**
     * Returns a filter predicate for a given text. This predicate will be added to the list of
     * internally managed predicates. The input field for text will only appear if this predicate
     * has been specified.
     *
     * @return the text filter predicate provider
     */
    public final ObjectProperty<Callback<String, Predicate<T>>> textFilterProviderProperty() {
        return textFilterProvider;
    }

    public final void setTextFilterProvider(Callback<String, Predicate<T>> textFilterProvider) {
        this.textFilterProvider.set(textFilterProvider);
    }

    // filter groups

    private final ListProperty<FilterGroup<T>> filterGroups = new SimpleListProperty<>(FXCollections.observableArrayList());

    public final ObservableList<FilterGroup<T>> getFilterGroups() {
        return filterGroups.get();
    }

    public final ListProperty<FilterGroup<T>> filterGroupsProperty() {
        return filterGroups;
    }

    public final void setFilterGroups(ObservableList<FilterGroup<T>> filterGroups) {
        this.filterGroups.set(filterGroups);
    }

    // filters

    private final ListProperty<Filter<T>> filters = new SimpleListProperty<>(this, "filters", FXCollections.observableArrayList());

    public ObservableList<Filter<T>> getFilters() {
        return filters.get();
    }

    public ListProperty<Filter<T>> filtersProperty() {
        return filters;
    }

    public void setFilters(ObservableList<Filter<T>> filters) {
        this.filters.set(filters);
    }

    // additional filter predicate

    private final ObjectProperty<Predicate<T>> additionalFilterPredicate = new SimpleObjectProperty<>(this, "additionalFilterPredicate", item -> true);

    public final Predicate<T> getAdditionalFilterPredicate() {
        return additionalFilterPredicate.get();
    }

    public final ObjectProperty<Predicate<T>> additionalFilterPredicateProperty() {
        return additionalFilterPredicate;
    }

    public final void setAdditionalFilterPredicate(Predicate<T> additionalFilterPredicate) {
        this.additionalFilterPredicate.set(additionalFilterPredicate);
    }

    // filter predicate

    private final ReadOnlyObjectWrapper<Predicate<T>> filterPredicate = new ReadOnlyObjectWrapper<>(this, "filterPredicate", item -> true);

    public final Predicate<T> getFilterPredicate() {
        return filterPredicate.get();
    }

    public final ReadOnlyObjectProperty<Predicate<T>> filterPredicateProperty() {
        return filterPredicate.getReadOnlyProperty();
    }

    /**
     * A filter group consists of a group of filters and has a name. The name
     * is displayed in the UI as part of the dropdown list that expostes the
     * available filters.
     *
     * @param <T> the type of the model objects managed by the filter view
     */
    public static class FilterGroup<T> {

        /**
         * Constructs a new group.
         *
         * @param name the name that will be shown in the UI, e.g. "Gender"
         * @param id   the id of the group
         */
        public FilterGroup(String name, String id) {
            setName(name);
            setId(id);

            filters.addListener((Change<? extends Filter<T>> change) -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(filter -> filter.setGroup(this));
                    }
                }
            });
        }

        public FilterGroup(String name) {
            this(name, StringUtils.replaceEach(name, new String[]{"(", ")", "&", "_", " "}, new String[]{"", "", "and", "-", "-"}).toLowerCase());
        }

        // filters

        private final ListProperty<Filter<T>> filters = new SimpleListProperty<>(FXCollections.observableArrayList());

        public final ObservableList<Filter<T>> getFilters() {
            return filters.get();
        }

        public final ListProperty<Filter<T>> filtersProperty() {
            return filters;
        }

        public final void setFilters(ObservableList<Filter<T>> filters) {
            this.filters.set(filters);
        }

        // group name

        private final StringProperty name = new SimpleStringProperty(this, "name", "Untitled");

        /**
         * The name of the filter as shown in the filter group's dropdown list.
         *
         * @return the filter name
         */
        public final StringProperty nameProperty() {
            return name;
        }

        public final String getName() {
            return name.get();
        }

        public final void setName(String name) {
            this.name.set(name);
        }

        private final StringProperty id = new SimpleStringProperty(this, "id");

        public final String getId() {
            return id.get();
        }

        /**
         * An identifier, useful for persisting session state of the filter view.
         *
         * @return the ID of the filter group
         */
        public final StringProperty idProperty() {
            return id;
        }

        public final void setId(String id) {
            this.id.set(id);
        }
    }

    /**
     * A filter is a predicate that will be used for filtering the elements of an
     * observable list. A filter may only be added to one {@link FilterGroup}.
     *
     * @param <T> the type of the model objects managed by the filter view
     * @see FilterGroup#getFilters()
     * @see FilteredList#setPredicate(Predicate)
     */
    public abstract static class Filter<T> implements Predicate<T> {

        private FilterGroup<T> group;
        private boolean selected;

        /**
         * Constructs a new filter with the given name.
         *
         * @param name the name of the filter (e.g. "Male")
         * @param id   the id of the filter
         */
        public Filter(String name, String id) {
            Objects.requireNonNull(name, "filter name can not be null");
            setName(name);
            setId(id);
        }

        /**
         * Constructs a new filter with the given name.
         *
         * @param name the name of the filter (e.g. "Male")
         */
        public Filter(String name) {
            this(name, StringUtils.replaceEach(name, new String[]{"(", ")", "&", "_", " "}, new String[]{"", "", "and", "-", "-"}).toLowerCase());
        }

        /**
         * Constructs a new filter with the given name, setting it to the default selected state.
         *
         * @param name           the name of the filter (e.g., "Male")
         * @param selected the default selected state of the filter; {@code true} if the filter should be selected by default, {@code false} otherwise
         */
        public Filter(String name, boolean selected) {
            this(name);
            this.selected = selected;
        }

        /**
         * Constructs a new filter with the given name and id, setting it to the default selected state.
         *
         * @param name           the name of the filter (e.g., "Male")
         * @param id             the id of the filter
         * @param selected the default selected state of the filter; {@code true} if the filter should be selected by default, {@code false} otherwise
         */
        public Filter(String name, String id, boolean selected) {
            this(name, id);
            this.selected = selected;
        }

        /**
         * Returns the group to which the filter belongs.
         *
         * @return the filter's "parent" group
         * @see FilterGroup#getFilters()
         */
        public FilterGroup<T> getGroup() {
            return group;
        }

        void setGroup(FilterGroup<T> group) {
            this.group = group;
        }

        // name

        private final StringProperty name = new SimpleStringProperty(this, "name", "Untitled");

        /**
         * The name of the filter as shown inside the UI.
         *
         * @return the filter's name
         */
        public final StringProperty nameProperty() {
            return name;
        }

        public final String getName() {
            return name.get();
        }

        public final void setName(String name) {
            this.name.set(name);
        }

        private final StringProperty id = new SimpleStringProperty(this, "id");

        public final String getId() {
            return id.get();
        }

        /**
         * An identifier, useful for persisting session state of the filter view.
         *
         * @return the ID of the filter group
         */
        public final StringProperty idProperty() {
            return id;
        }

        public final void setId(String id) {
            this.id.set(id);
        }

        /**
         * Returns the default selected state of the filter.
         *
         * @return {@code true} if the filter is selected by default, {@code false} otherwise
         */
        public boolean isSelected() {
            return selected;
        }

    }
}
