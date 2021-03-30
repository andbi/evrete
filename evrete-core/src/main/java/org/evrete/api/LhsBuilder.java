package org.evrete.api;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface LhsBuilder<C extends RuntimeContext<C>> {
    C execute(String literalRhs);

    RuleBuilder<C> setRhs(String literalConsumer);

    RuleBuilder<C> create();

    C execute();

    C execute(Consumer<RhsContext> consumer);

    Function<String, NamedType> getFactTypeMapper();

    RuleBuilder<C> getRuleBuilder();

    LhsBuilder<C> where(String... expressions);

    LhsBuilder<C> where(String expression, double complexity);

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, String... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, String... references);

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, String... references);

    LhsBuilder<C> where(ValuesPredicate predicate, String... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, FieldReference... references);

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, FieldReference... references);

    LhsBuilder<C> where(ValuesPredicate predicate, FieldReference... references);

    NamedType addFactDeclaration(String name, Type<?> type);

    NamedType addFactDeclaration(String name, String type);

    LhsBuilder<C> buildLhs(Collection<FactBuilder> facts);

    NamedType addFactDeclaration(String name, Class<?> type);
}