package space.kiibou.reflect;

import space.kiibou.net.server.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

public class ReflectUtils {

    public static <T> T createInstance(final String name, final String sup, final Class[] parameterTypes, final Object... args) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sup);
        Objects.requireNonNull(parameterTypes);
        Objects.requireNonNull(args);

        final Class<T> clazz;
        final T object;

        try {
            //noinspection unchecked
            clazz = (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(name);

            if (!hasSupertype(clazz, Service.class))
                throw new RuntimeException(String.format("%s is not subtype of %s", name, Service.class.getCanonicalName()));

            object = clazz.getDeclaredConstructor(parameterTypes).newInstance(args);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return object;
    }

    private static boolean hasSupertype(final Class<? extends Object> clazz, final Class<Service> superClazz) {
        /* search all supertypes of clazz
         * do this by searching until either Type Object is reached or Type Service is found */
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(superClazz);

        Class zuper = clazz.getSuperclass();
        boolean searching = true;
        boolean found = false;
        while (searching && !found) {
            if (zuper.equals(Object.class)) searching = false;
            if (zuper.equals(superClazz)) found = true;
            else zuper = zuper.getSuperclass();
        }

        return found;
    }

    public static Field[] getAnnotatedFields(final Object obj, final Class<? extends Annotation> annotation) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(annotation);

        return Arrays.stream(obj.getClass().getFields())
                .filter(field -> field.isAnnotationPresent(annotation))
                .toArray(Field[]::new);
    }

}
