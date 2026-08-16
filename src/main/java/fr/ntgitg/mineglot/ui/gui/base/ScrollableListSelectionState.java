package fr.ntgitg.mineglot.ui.gui.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class ScrollableListSelectionState<T> {

    private final ScrollableListComponent.SelectionMode mode;
    private int selectedIndex = -1;
    private T selectedValue;
    private final Set<T> selectedValues = new HashSet<>();

    ScrollableListSelectionState(ScrollableListComponent.SelectionMode mode) {
        this.mode = mode == null ? ScrollableListComponent.SelectionMode.SINGLE : mode;
    }

    ScrollableListComponent.SelectionMode getMode() {
        return mode;
    }

    int getSelectedIndex() {
        return selectedIndex;
    }

    T getSelectedValue() {
        return selectedValue;
    }

    Set<T> getSelectedValues() {
        return Collections.unmodifiableSet(selectedValues);
    }

    void onFilterUpdated(List<T> displayItems) {
        if (mode == ScrollableListComponent.SelectionMode.SINGLE) {
            selectedIndex = displayItems.indexOf(selectedValue);
        }
    }

    void onItemClicked(int clickedIndex, T value) {
        if (mode == ScrollableListComponent.SelectionMode.SINGLE) {
            selectedIndex = clickedIndex;
            selectedValue = value;
            return;
        }

        if (mode == ScrollableListComponent.SelectionMode.MULTIPLE) {
            if (selectedValues.contains(value)) {
                selectedValues.remove(value);
            } else {
                selectedValues.add(value);
            }
        }
    }

    boolean isSelected(int idx, List<T> displayItems) {
        if (idx < 0 || idx >= displayItems.size()) {
            return false;
        }

        if (mode == ScrollableListComponent.SelectionMode.SINGLE) {
            return idx == selectedIndex;
        }

        if (mode == ScrollableListComponent.SelectionMode.MULTIPLE) {
            return selectedValues.contains(displayItems.get(idx));
        }

        return false;
    }

    Set<Integer> getSelectedIndices(List<T> displayItems) {
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < displayItems.size(); i++) {
            if (selectedValues.contains(displayItems.get(i))) {
                indices.add(i);
            }
        }
        return indices;
    }

    void setSelectedIndex(int idx, List<T> displayItems) {
        if (idx >= 0 && idx < displayItems.size()) {
            selectedIndex = idx;
            selectedValue = displayItems.get(idx);
            return;
        }
        selectedIndex = -1;
        selectedValue = null;
    }

    void setSelectedIndices(Set<Integer> indices, List<T> displayItems) {
        selectedValues.clear();
        if (indices == null) {
            return;
        }

        for (Integer idx : indices) {
            if (idx != null && idx >= 0 && idx < displayItems.size()) {
                selectedValues.add(displayItems.get(idx));
            }
        }
    }

    void setSelectedValue(T value, List<T> displayItems) {
        if (value != null && displayItems.contains(value)) {
            selectedValue = value;
            selectedIndex = displayItems.indexOf(value);
            return;
        }
        selectedValue = null;
        selectedIndex = -1;
    }

    void syncSelectionWithFilteredList(List<T> displayItems, Function<T, String> nameExtractor,
                                       Predicate<String> isSelectedPredicate) {
        if (mode != ScrollableListComponent.SelectionMode.SINGLE || isSelectedPredicate == null) {
            return;
        }

        int idx = -1;
        for (int i = 0; i < displayItems.size(); i++) {
            String name = nameExtractor.apply(displayItems.get(i));
            if (isSelectedPredicate.test(name)) {
                idx = i;
                break;
            }
        }

        setSelectedIndex(idx, displayItems);
    }
}
