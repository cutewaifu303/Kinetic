package secret.kinetic.api.events;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final Logger LOGGER = LogManager.getLogger("Kinetic EventBus");
    private static final Comparator<Method> PRIORITY_COMPARATOR = Comparator.comparingInt(EventBus::priorityOf);

    private final Map<Method, Class<?>> registeredMethodMap;
    private final Map<Method, Object> methodObjectMap;
    private final Map<Class<? extends Event>, List<Method>> priorityMethodMap;
    
    private final Map<Method, Boolean> reportedFailures = new ConcurrentHashMap<>();

    private static int priorityOf(Method method) {
        EventHook priority = method.getAnnotation(EventHook.class);
        return (priority != null) ? priority.value() : EventPriority.MEDIUM;
    }

    public EventBus() {
        registeredMethodMap = new ConcurrentHashMap<>();
        methodObjectMap = new ConcurrentHashMap<>();
        priorityMethodMap = new ConcurrentHashMap<>();
    }

    




    public void subscribe(Object... obj) {
        for (Object object : obj) {
            subscribe(object);
        }
    }

    




    public void subscribe(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            Annotation[] annotations = method.getDeclaredAnnotations();

            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == EventHook.class && method.getParameterTypes().length == 1) {
                    registeredMethodMap.put(method, method.getParameterTypes()[0]);
                    methodObjectMap.put(method, obj);
                    method.setAccessible(true);

                    Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
                    List<Method> handlers = priorityMethodMap.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
                    handlers.add(method);
                    PriorityHolder.sort(handlers);
                }
            }
        }
    }

    




    public void unsubscribe(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (registeredMethodMap.containsKey(method)) {
                registeredMethodMap.remove(method);
                methodObjectMap.remove(method);
                Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
                List<Method> priorityMethods = priorityMethodMap.get(eventClass);
                if (priorityMethods != null) {
                    priorityMethods.remove(method);
                    PriorityHolder.sort(priorityMethods);
                }
            }
        }
    }

    





    public Event post(Event event) {
        Class<? extends Event> eventClass = event.getClass();

        List<Method> methods = priorityMethodMap.get(eventClass);
        if (methods != null) {
            for (Method method : methods) {
                Object obj = methodObjectMap.get(method);
                try {
                    method.invoke(obj, event);
                } catch (Exception e) {
                    if (reportedFailures.putIfAbsent(method, Boolean.TRUE) == null) {
                        Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                        LOGGER.error("{}#{} failed on {}", method.getDeclaringClass().getSimpleName(), method.getName(), eventClass.getSimpleName(), cause);
                    }
                }
            }
        }

        return event;
    }

    
    private static final class PriorityHolder {
        static void sort(List<Method> methods) {
            methods.sort(PRIORITY_COMPARATOR);
        }
    }
}
