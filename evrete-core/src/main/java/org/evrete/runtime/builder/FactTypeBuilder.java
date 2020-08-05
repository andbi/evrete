package org.evrete.runtime.builder;

import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class FactTypeBuilder implements NamedType {
    private final LhsBuilder<?, ?> group;
    private final String var;
    private final Type type;
    private final Set<TypeField> betaFields = new HashSet<>();

    FactTypeBuilder(LhsBuilder<?, ?> group, String var, Type type) {
        Objects.requireNonNull(var);
        Objects.requireNonNull(type);
        this.group = group;
        this.var = var;
        this.type = type;
    }

    /**
     * @return true if this Type builder is a member of beta condition
     */
    public boolean isBetaTypeBuilder() {
        return betaFields.size() > 0;
    }

    void addBetaField(FieldReference ref) {
        this.betaFields.add(ref.field());
    }

    public final Set<TypeField> getBetaTypeFields() {
        return this.betaFields;
    }

    @Override
    public String getVar() {
        return var;
    }

    public LhsBuilder<?, ?> getGroup() {
        return group;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "{" +
                "var='" + var + '\'' +
                ", type=" + type +
                '}';
    }
}