package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.api.TypeWrapper;
import org.evrete.collections.ArrayOf;

import java.util.*;
import java.util.logging.Logger;

class TypeResolverImpl implements TypeResolver {
    private static final Logger LOGGER = Logger.getLogger(TypeResolverImpl.class.getName());
    private static final List<Class<?>> EMPTY_CLASS_LIST = new ArrayList<>();
    private final Map<String, Type<?>> typeDeclarationMap = new HashMap<>();
    private final Map<String, ArrayOf<Type<?>>> typesByJavaType = new HashMap<>();

    private final Map<String, TypeCacheEntry> typeInheritanceCache = new HashMap<>();
    private final ClassLoader classLoader;
    private int typeCounter = 0;
    private int fieldSetsCounter = 0;

    TypeResolverImpl(RuntimeContext<?> requester) {
        this.classLoader = requester.getClassLoader();
    }

    private TypeResolverImpl(TypeResolverImpl other) {
        this.classLoader = other.classLoader;
        this.typeCounter = other.typeCounter;
        this.fieldSetsCounter = other.fieldSetsCounter;
        for (Map.Entry<String, Type<?>> entry : other.typeDeclarationMap.entrySet()) {
            this.typeDeclarationMap.put(entry.getKey(), entry.getValue().copyOf());
        }

        for (Map.Entry<String, ArrayOf<Type<?>>> entry : other.typesByJavaType.entrySet()) {
            this.typesByJavaType.put(entry.getKey(), new ArrayOf<>(entry.getValue()));
        }
    }

    @Override
    public synchronized void wrapType(TypeWrapper<?> typeWrapper) {
        Type<?> delegate = typeWrapper.getDelegate();
        String typeName = typeWrapper.getName();
        Type<?> prev = this.typeDeclarationMap.put(typeName, typeWrapper);
        if (prev != delegate) {
            throw new IllegalStateException(typeWrapper + " wraps an unknown type");
        }

        ArrayOf<Type<?>> byJavaTypes = typesByJavaType.get(typeWrapper.getJavaType().getName());
        if (byJavaTypes == null) {
            throw new IllegalStateException();
        }

        boolean changed = false;
        for (int i = 0; i < byJavaTypes.data.length; i++) {
            if (byJavaTypes.data[i] == delegate) {
                byJavaTypes.data[i] = typeWrapper; // Replacing the type
                changed = true;
                break;
            }
        }

        if (!changed) {
            throw new IllegalStateException();
        }
    }

    //TODO scan interfaces as well
    private static List<Class<?>> superClasses(Class<?> subject) {
        if (subject.isArray() || subject.isPrimitive() || subject.equals(Object.class)) return EMPTY_CLASS_LIST;

        List<Class<?>> l = new ArrayList<>();

        Class<?> current = subject.getSuperclass();
        while (!current.equals(Object.class)) {
            l.add(current);
            current = current.getSuperclass();
        }

        return l;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(String javaType) {
        try {
            return (Class<T>) Class.forName(javaType, true, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public synchronized <T> Type<T> declare(String typeName, String javaType) {
        Type<T> type = getType(typeName);
        if (type != null)
            throw new IllegalStateException("Type name '" + typeName + "' has been already defined: " + type);
        Class<T> resolvedJavaType = classForName(javaType);
        if (resolvedJavaType == null)
            throw new IllegalStateException("Unable to resolve Java class name '" + javaType + "'");
        return saveNewType(typeName, new TypeImpl<>(typeName, resolvedJavaType));
    }

    @Override
    public synchronized <T> Type<T> declare(String typeName, Class<T> javaType) {
        Type<T> type = getType(typeName);
        if (type != null)
            throw new IllegalStateException("Type name '" + typeName + "' has been already defined: " + type);
        return saveNewType(typeName, new TypeImpl<>(typeName, javaType));
    }

    private <T> Type<T> saveNewType(String typeName, Type<T> type) {
        typeDeclarationMap.put(typeName, type);
        typesByJavaType.computeIfAbsent(
                type.getJavaType().getName(),
                k -> new ArrayOf<Type<?>>(new Type[]{}))
                .append(type);
        typeInheritanceCache.clear();
        return type;
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return Collections.unmodifiableCollection(typeDeclarationMap.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> getType(String name) {
        return (Type<T>) typeDeclarationMap.get(name);
    }

    private Type<?> findInSuperClasses(Class<?> type) {

        List<Type<?>> matching = new LinkedList<>();
        List<Class<?>> superClasses = superClasses(type);
        for (Class<?> sup : superClasses) {
            String supName = sup.getName();
            ArrayOf<Type<?>> match = typesByJavaType.get(supName);
            if (match != null && match.data.length == 1) {
                matching.add(match.data[0]);
            }
        }

        switch (matching.size()) {
            case 0:
                return null;
            case 1:
                return matching.iterator().next();
            default:
                LOGGER.warning("Unable to resolve type '" + type + "' due to ambiguity.");
                return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> resolve(Object o) {
        Objects.requireNonNull(o);
        Class<?> javaType = o.getClass();
        String name = javaType.getName();

        ArrayOf<Type<?>> associatedTypes = typesByJavaType.get(name);
        if (associatedTypes != null) {
            if (associatedTypes.data.length > 1) {
                LOGGER.warning("Ambiguous type declaration found, there are " + associatedTypes.data.length + " types associated with '" + name + "' Java type, returning <null>.");
                return null;
            } else {
                return (Type<T>) associatedTypes.data[0];
            }
        } else {
            // There is no direct match, but there might be a registered super class that can be used instead
            TypeCacheEntry cacheEntry = typeInheritanceCache.get(name);
            if (cacheEntry == null) {
                synchronized (this) {
                    cacheEntry = typeInheritanceCache.get(name);
                    if (cacheEntry == null) {
                        cacheEntry = new TypeCacheEntry(findInSuperClasses(javaType));
                        typeInheritanceCache.put(name, cacheEntry);
                    }
                }
            }
            return (TypeImpl<T>) cacheEntry.type;
        }
    }

    @Override
    public TypeResolverImpl copyOf() {
        return new TypeResolverImpl(this);
    }

    private static class TypeCacheEntry {
        private final Type<?> type;

        TypeCacheEntry(Type<?> resolved) {
            this.type = resolved;
        }
    }
}
