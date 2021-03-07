package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBetaConditionNode implements BetaMemoryNode {
    private final ConditionNodeDescriptor descriptor;
    private final BetaMemoryNode[] sources;
    private final BetaConditionNode[] conditionSources;
    private final RuntimeRuleImpl rule;
    private final ZStoreI[] stores = new ZStoreI[KeyMode.values().length];

    private boolean mergeToMain = true;

    AbstractBetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode[] sources) {
        this.sources = sources;
        List<BetaConditionNode> conditionNodeList = new ArrayList<>(sources.length);
        for (BetaMemoryNode source : sources) {
            if (source.getDescriptor().isConditionNode()) {
                conditionNodeList.add((BetaConditionNode) source);
            }
        }
        this.conditionSources = conditionNodeList.toArray(BetaConditionNode.EMPTY_ARRAY);
        this.rule = rule;
        this.descriptor = descriptor;
        for (KeyMode keyMode : KeyMode.values()) {
            stores[keyMode.ordinal()] = new ZStore(descriptor.getTypes());
        }
    }


    void commitDelta1() {
        ZStoreI delta1 = getStore(KeyMode.UNKNOWN_UNKNOWN);
        ZStoreI delta2 = getStore(KeyMode.KNOWN_UNKNOWN);
        if (mergeToMain) {
            ZStoreI main = getStore(KeyMode.MAIN);
            ReIterator<MemoryKey[]> it = delta1.entries();
            while (it.hasNext()) {
                MemoryKey[] keys = it.next();
                for (MemoryKey key : keys) {
                    key.setMetaValue(KeyMode.MAIN.ordinal());
                }
            }
            main.append(delta1);
        }
        delta1.clear();
        delta2.clear();
    }

    void setMergeToMain(boolean mergeToMain) {
        this.mergeToMain = mergeToMain;
    }

    ZStoreI getStore(KeyMode mode) {
        return stores[mode.ordinal()];
    }

    AbstractKnowledgeSession<?> getRuntime() {
        return rule.getRuntime();
    }

    public BetaConditionNode[] getConditionSources() {
        return conditionSources;
    }

    BetaMemoryNode[] getSources() {
        return sources;
    }

    @Override
    public ConditionNodeDescriptor getDescriptor() {
        return descriptor;
    }


    public ReIterator<MemoryKey[]> iterator(KeyMode mode) {
        return getStore(mode).entries();
    }


    @Override
    public void clear() {
        for (ZStoreI s : stores) {
            s.clear();
        }
        for (BetaMemoryNode source : getSources()) {
            source.clear();
        }
    }
}
