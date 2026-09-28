package secret.kinetic.modules;

import com.google.common.collect.ImmutableClassToInstanceMap;
import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.KeyPressEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ModuleManager {

    private static final Logger LOGGER = LogManager.getLogger("Kinetic Modules");
    private final ImmutableClassToInstanceMap<Module> instanceMap;

    public ModuleManager() {
        instanceMap = scanAndBuildInstanceMap();
        getModules().forEach(Module::reflectProperties);
        getModules().forEach(Module::resetPropertyValues);
        Kinetic.INSTANCE.getEventBus().subscribe(this);
    }

    @EventHook
    public void onKeyPress(KeyPressEvent event) {
        final int keyPressed = event.getKey();
        for (final Module module : this.getModules()) {
            final int moduleBind = module.getKey();
            if (moduleBind == keyPressed) {
                module.toggle();
            }
        }
    }

    public void postInit() {
        getModules().forEach(Module::resetPropertyValues);

        for (final Module module : getModules()) {
            ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
            if (info != null && info.enabledByDefault() && !module.isEnabled()) {
                module.setEnabled(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ImmutableClassToInstanceMap<Module> scanAndBuildInstanceMap() {
        List<Module> modules = new ArrayList<>();

        for (Class<? extends Module> clazz : findModuleClasses()) {
            if (Modifier.isAbstract(clazz.getModifiers()))
                continue;

            Module module = instantiate(clazz);
            if (module != null) {
                modules.add(module);
            }
        }

        modules.sort(Comparator.comparing(Module::getLabel, String.CASE_INSENSITIVE_ORDER));

        ImmutableClassToInstanceMap.Builder<Module> modulesBuilder = ImmutableClassToInstanceMap.builder();
        for (Module module : modules) {
            modulesBuilder.put((Class<Module>) module.getClass(), module);
        }

        return modulesBuilder.build();
    }

    




    private static Collection<Class<? extends Module>> findModuleClasses() {
        Set<Class<? extends Module>> found = new LinkedHashSet<>();
        try {
            found.addAll(new Reflections("secret.kinetic.modules").getSubTypesOf(Module.class));
        } catch (Throwable t) {
            LOGGER.warn("Reflections scan for modules failed: " + t);
        }
        if (!found.isEmpty()) return found;

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = ModuleManager.class.getClassLoader();
        Collection<String> names = loaderClassNames(loader);
        if (names == null) {
            LOGGER.error("No module classes found: Reflections saw nothing and the class loader does not list its classes");
            return found;
        }
        for (String name : names) {
            if (!name.startsWith("secret.kinetic.modules.")) continue;
            try {
                Class<?> clazz = Class.forName(name, false, loader);
                if (Module.class.isAssignableFrom(clazz) && clazz != Module.class) {
                    found.add(clazz.asSubclass(Module.class));
                }
            } catch (Throwable ignored) {
            }
        }
        LOGGER.info("Found " + found.size() + " module classes through the class loader");
        return found;
    }

    
    @SuppressWarnings("unchecked")
    private static Collection<String> loaderClassNames(ClassLoader loader) {
        for (ClassLoader current = loader; current != null; current = current.getParent()) {
            try {
                Method method = current.getClass().getMethod("classNames");
                Object result = method.invoke(current);
                if (result instanceof Collection) return (Collection<String>) result;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                LOGGER.warn("classNames() failed on " + current.getClass().getName() + ": " + e);
            }
        }
        return null;
    }

    private Module instantiate(Class<? extends Module> clazz) {
        try {
            Field instanceField = clazz.getField("INSTANCE");
            if (Module.class.isAssignableFrom(instanceField.getType())) {
                Object value = instanceField.get(null);
                if (value instanceof Module) {
                    return (Module) value;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
        }

        return null;
    }

    public Collection<Module> getModules() {
        return instanceMap.values();
    }

    public <T extends Module> T getModule(Class<T> moduleClass)  {
        return instanceMap.getInstance(moduleClass);
    }

    public Module getModule(String label) {
        return getModules().stream().filter(module -> module.getLabel().replaceAll(" ", "").equalsIgnoreCase(label)).findFirst().orElse(null);
    }

    public static <T extends Module> T getInstance(Class<T> clazz) {
        return Kinetic.INSTANCE.getModuleManager().getModule(clazz);
    }

    public List<Module> getModulesForCategory(ModuleCategory category) {
        return getModules().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }
}