package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.*;

public class HelloWorldType {
    private static final String HELLO_WORLD_CONST = "Hello World";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge();
        TypeResolver typeResolver = knowledge.getTypeResolver();

        // Declare a new Type named "Hello World"
        Type helloType = typeResolver
                .declare(HELLO_WORLD_CONST);

        HelloWorldResolver myResolver = new HelloWorldResolver(typeResolver);
        knowledge.wrapTypeResolver(myResolver);
        // Declare a new int field which equals the squared length of the input String
        helloType.<String>declareField(
                "lenSquared",
                int.class,
                s -> s.length() * s.length()
        );


        StatefulSession session = knowledge
                .newRule()
                .forEach("$hw", HELLO_WORLD_CONST)
                .where("$hw.lenSquared > 1")
                .execute(context -> {
                    RuntimeFact fact = context.getFact("$hw");
                    String str = fact.getDelegate();
                    int myFieldValue = fact.getValue(0);
                    System.out.println(str + ", lenSquared = " + myFieldValue);
                })
                .createSession();


        session.insertAndFire("a", "bb", "ccc", "dddd");
        /*
            Expected output:
            =====================
            bb, lenSquared = 4
            ccc, lenSquared = 9
            dddd, lenSquared = 16
         */

        session.close();
        service.shutdown();
    }

    static class HelloWorldResolver extends TypeResolverWrapper {
        HelloWorldResolver(TypeResolver delegate) {
            super(delegate);
        }

        @Override
        public Type resolve(Object o) {
            if (o instanceof String) {
                return getType(HELLO_WORLD_CONST);
            } else {
                return super.resolve(o);
            }
        }
    }
}