package org.evrete.api;

import org.evrete.util.CompilationException;

/**
 * The {@code RuntimeRule} is a representation of a rule that is already associated with a {@link RuleSession}.
 */
public interface RuntimeRule extends Rule {

    RuleSession<?> getRuntime();


    /**
     * <p>
     * Compiles a string expression and returns it as an {@link Evaluator} instance.
     * </p>
     *
     * @param expression expression to compile
     * @return evaluator instance
     */
    default Evaluator buildExpression(String expression) {
        try {
            return getRuntime().compile(LiteralExpression.of(expression, this));
        } catch (CompilationException e) {
            throw new IllegalArgumentException("Unable to compile expression '" + expression + "'", e);
        }
    }

    default FieldReference[] resolve(String[] args) {
        return getRuntime().resolveFieldReferences(args, this);
    }
}
