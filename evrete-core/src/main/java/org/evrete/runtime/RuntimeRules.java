package org.evrete.runtime;

import org.evrete.api.EvaluationListener;
import org.evrete.api.EvaluationListeners;
import org.evrete.api.Rule;
import org.evrete.api.RuntimeRule;

import java.util.*;

public class RuntimeRules implements Iterable<RuntimeRuleImpl>, EvaluationListeners {
    private final List<RuntimeRuleImpl> list = new ArrayList<>();
    //private final SessionMemory runtime;


    private void add(RuntimeRuleImpl rule) {
        this.list.add(rule);
    }

    RuntimeRuleImpl addRule(RuleDescriptor ruleDescriptor, AbstractRuleSession<?> session) {
        RuntimeRuleImpl r = new RuntimeRuleImpl(ruleDescriptor, session);
        this.add(r);
        return r;
    }

    void sort(Comparator<Rule> comparator) {
        this.list.sort(comparator);
    }

    @Override
    public Iterator<RuntimeRuleImpl> iterator() {
        return list.iterator();
    }

    public List<RuntimeRule> asList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public void addListener(EvaluationListener listener) {
        for (RuntimeRuleImpl rule : list) {
            rule.addListener(listener);
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (RuntimeRuleImpl rule : list) {
            rule.removeListener(listener);
        }
    }
}
