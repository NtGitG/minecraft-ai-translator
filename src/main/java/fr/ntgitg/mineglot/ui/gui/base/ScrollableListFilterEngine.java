package fr.ntgitg.mineglot.ui.gui.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

final class ScrollableListFilterEngine<T> {

    private final Function<T, String> nameExtractor;

    ScrollableListFilterEngine(Function<T, String> nameExtractor) {
        this.nameExtractor = nameExtractor;
    }

    List<T> filter(List<T> sourceItems, String query) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return new ArrayList<>();
        }

        if (query == null || query.isEmpty()) {
            return new ArrayList<>(sourceItems);
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<T> result = new ArrayList<>();
        for (T item : sourceItems) {
            String name = nameExtractor.apply(item);
            if (name != null && name.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                result.add(item);
            }
        }
        return result;
    }

    List<T> reorderSelectedOnTop(List<T> filteredItems,
                                 ScrollableListComponent.SelectionMode mode,
                                 T selectedValue,
                                 Set<T> selectedValues) {
        if (filteredItems.isEmpty()) {
            return filteredItems;
        }

        if (mode == ScrollableListComponent.SelectionMode.SINGLE && selectedValue != null) {
            List<T> ordered = new ArrayList<>();
            for (T item : filteredItems) {
                if (selectedValue.equals(item)) {
                    ordered.add(item);
                }
            }
            for (T item : filteredItems) {
                if (!selectedValue.equals(item)) {
                    ordered.add(item);
                }
            }
            return ordered;
        }

        if (mode == ScrollableListComponent.SelectionMode.MULTIPLE
                && selectedValues != null && !selectedValues.isEmpty()) {
            List<T> selected = new ArrayList<>();
            List<T> unselected = new ArrayList<>();
            for (T item : filteredItems) {
                if (selectedValues.contains(item)) {
                    selected.add(item);
                } else {
                    unselected.add(item);
                }
            }
            selected.addAll(unselected);
            return selected;
        }

        return filteredItems;
    }

    List<String> extractNames(List<T> sourceItems) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>(sourceItems.size());
        for (T item : sourceItems) {
            String name = nameExtractor.apply(item);
            result.add(name == null ? "" : name);
        }
        return result;
    }
}
