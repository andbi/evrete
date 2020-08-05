package org.evrete.runtime.async;

import org.evrete.api.KeysStore;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.memory.BetaConditionNode;
import org.evrete.runtime.memory.BetaMemoryNode;
import org.evrete.runtime.structure.FactType;
import org.evrete.util.Bits;

public class NodeCleanTask extends Completer {
    private final BetaConditionNode node;
    private final RuntimeRule rule;
    private final FactType[][] grouping;
    private final KeysStore subject;
    private final Bits deleteMask;

    public NodeCleanTask(Completer completer, BetaConditionNode node, RuntimeRule rule, Bits deleteMask) {
        super(completer);
        this.node = node;
        this.rule = rule;
        this.grouping = node.getGrouping();
        this.subject = node.getMainStore();
        this.deleteMask = deleteMask;
    }

    private NodeCleanTask(NodeCleanTask parent, BetaConditionNode node) {
        this(parent, node, parent.rule, parent.deleteMask);
    }

    @Override
    protected void execute() {
        for (BetaMemoryNode<?> source : node.getSources()) {
            Bits sourceMask = source.getTypeMask();
            if (source.isConditionNode() && sourceMask.intersects(deleteMask)) {
                BetaConditionNode sourceNode = (BetaConditionNode) source;
                if (sourceNode.hasMainData()) {
                    forkNew(new NodeCleanTask(this, sourceNode));
                }
            }
        }

        // Local execution
        ValueRowPredicate[] predicates = ValueRowPredicate.predicates(grouping, rule.getDeletedKeys());// new ValueRowPredicate[grouping.length];
        subject.delete(predicates);
    }


}