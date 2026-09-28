package secret.kinetic.api.properties.impl;

import secret.kinetic.api.properties.Property;

import java.util.function.Supplier;

public class DescriptorProperty extends Property<String> {

    private final int paddingTop;
    private final int paddingBottom;

    public DescriptorProperty(String label, int paddingTop, int paddingBottom, Supplier<Boolean> dependency) {
        super(label, "placeholduh", dependency);
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
    }

    public DescriptorProperty(String label, int paddingTop, int paddingBottom) {
        this(label, paddingTop, paddingBottom, () -> true);
    }

    public DescriptorProperty(String label) {
        this(label,4, 4);
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }
}
