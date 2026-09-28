package secret.kinetic.api.properties.impl;

import secret.kinetic.api.properties.Property;

import java.util.*;
import java.util.function.Supplier;

public class MultiModeProperty<T extends Enum<T>> extends Property<List<T>> {

    private final T[] values;
    private final Map<T, Supplier<Boolean>> suppliers = new LinkedHashMap<>();

    @SafeVarargs
    public MultiModeProperty(String label, Supplier<Boolean> dependency, T... values) {
        super(label, Arrays.asList(values), dependency);

        if (values.length == 0)
            throw new RuntimeException("Must have at least one default value.");

        this.values = getEnumConstants();
    }

    @SafeVarargs
    public MultiModeProperty(String label, T... values) {
        this(label, () -> true, values);
    }

    public MultiModeProperty<T> depend(T variant, Supplier<Boolean> supplier) {
        suppliers.put(variant, supplier);
        return this;
    }

    public boolean isVisible(Enum<?> variant) {
        Supplier<Boolean> s = suppliers.get(variant);
        return s == null || s.get();
    }

    @SuppressWarnings("unchecked")
    private T[] getEnumConstants() {
        return (T[]) value.get(0).getClass().getEnumConstants();
    }

    public T[] getValues() {
        return values;
    }

    public boolean isSelected(Enum<?> variant) {
        return getValue().contains(variant);
    }

    public void setValue(int index) {
        final List<T> values = new ArrayList<>(this.value);
        final T referencedVariant = this.values[index];
        if (values.contains(referencedVariant)) {
            values.remove(referencedVariant);
        } else {
            values.add(referencedVariant);
        }
        setValue(values);
    }
}
