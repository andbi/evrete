package org.evrete;

import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

@SuppressWarnings("WeakerAccess")
public class LiteralRhsTests {
    private static KnowledgeService service;
    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
    }

    @Test
    void plainTest0() {
        knowledge.newRule()
                .addImport(RuleScope.RHS, "org.evrete.LiteralRhsTests.SystemOut")
                .addImport(RuleScope.RHS, SystemOut.class)
                .forEach("$n", Number.class)
                .execute("SystemOut.out($n);");


        StatefulSession session = knowledge.createSession();
        session.insertAndFire(10, 20);
        SystemOut.assertSize(2);
        assert SystemOut.collector.containsAll(Arrays.asList(10, 20));
        SystemOut.reset();
        session.close();
    }

    @Test
    void plainTest1() {
        RuleBuilder<Knowledge> builder = knowledge.newRule()
                .addImport(RuleScope.RHS, "org.evrete.LiteralRhsTests.SystemOut")
                .addImport(RuleScope.RHS, SystemOut.class)
                .forEach("$n", Number.class)
                .create();

        RuleDescriptor descriptor = knowledge.compileRule(builder);
        descriptor.setRhs("SystemOut.out($n);");

        StatefulSession session = knowledge.createSession();
        session.insertAndFire(10, 20);
        SystemOut.assertSize(2);
        assert SystemOut.collector.containsAll(Arrays.asList(10, 20));
        SystemOut.reset();
        session.close();
    }

    @Test
    void plainTest2() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("test")
                .addImport(RuleScope.RHS, "org.evrete.LiteralRhsTests.SystemOut")
                .addImport(RuleScope.RHS, SystemOut.class)
                .forEach("$n", Integer.class)
                .create();

        RuleDescriptor descriptor = knowledge.compileRule(builder);
        descriptor
                .addImport(RuleScope.RHS, "org.evrete.LiteralRhsTests.SystemOut")
                .addImport(RuleScope.RHS, SystemOut.class)
                .setRhs("SystemOut.out($n);");

        StatefulSession session = knowledge.createSession();

        session.insertAndFire(10, 20);
        SystemOut.assertSize(2);
        assert SystemOut.collector.containsAll(Arrays.asList(10, 20));
        SystemOut.reset();


        RuntimeRule rule = session.getRule("test");
        rule
                .addImport(RuleScope.RHS, "org.evrete.LiteralRhsTests.SystemOut")
                .addImport(RuleScope.RHS, SystemOut.class)
                .setRhs("SystemOut.out($n + 1);");

        session.insertAndFire(100, 200);
        SystemOut.assertSize(2);
        assert SystemOut.collector.containsAll(Arrays.asList(101, 201));
        SystemOut.reset();


        rule.setRhs("SystemOut.out($n + 2);");
        session.insertAndFire(1000, 2000);
        SystemOut.assertSize(2);
        assert SystemOut.collector.containsAll(Arrays.asList(1002, 2002));
        SystemOut.reset();

        session.close();
    }


    @SuppressWarnings("unused")
    public static class SystemOut {
        private static final NextIntSupplier counter = new NextIntSupplier();
        private static final Collection<Object> collector = new LinkedList<>();

        @SuppressWarnings("unused")
        public static void out(Object o) {
            counter.next();
            collector.add(o);
        }

        static void reset() {
            counter.set(0);
            collector.clear();
        }

        @SuppressWarnings("SameParameterValue")
        static void assertSize(int i) {
            assert collector.size() == i;
        }
    }
}
