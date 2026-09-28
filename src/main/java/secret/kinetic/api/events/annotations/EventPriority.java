package secret.kinetic.api.events.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventPriority {
    





    public static final byte VERY_LOW = 0;
    public static final byte LOW = 1;
    public static final byte MEDIUM = 2;
    public static final byte HIGH = 3;
    public static final byte VERY_HIGH = 4;
}
