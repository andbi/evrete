# Evrete

![GitHub repo size](https://img.shields.io/github/repo-size/evrete/evrete)
![GitHub stars](https://img.shields.io/github/stars/evrete/evrete?style=social)
[![libs.tech recommends](https://libs.tech/project/285368988/badge.svg)](https://libs.tech/project/285368988/evrete)

> **Important!**
>
> Check out what's new in version 4.0.0 and read the migration 
> guides at https://www.evrete.org/docs#migration-guide 
> 

> **Note: Java 8 support is being retired**
>
> As Java 8 is nearing the end of its life, Evrete will drop Java 8 compatibility
> starting with version 4.1.0, which will require Java 17 or later.
> Projects still on Java 8 should stay on the 4.0.x line.


Evrete is a forward-chaining Java rule engine that implements the RETE algorithm and is fully compliant with the Java
Rule Engine specification (JSR 94).

Historically designed as a fast and lightweight alternative to full-scale rule management systems, the engine also
brings its own mix of features:


**Rule authoring**

- Rules can be authored both externally and inline as plain Java code.
- The engine allows rules to be authored as **annotated Java** sources, classes, or archives.
- The library itself is a flexible tool for creating custom domain-specific rule languages (DSL).

**Intuitive and developer-friendly**

- Library's type system allows it to seamlessly process any kinds of objects, including JSON and XML.
- Fluent builders, Java functional interfaces, and other best practices keep developers' code concise and clear.
- Key components are exposed as Service Provider Interfaces and can be customized.

**Performance and security**

- The engine's algorithm and memory are optimized for large-scale and labeled data.
- Built-in support of Java Security Manager protects against unwanted or potentially malicious code in the rules.

## Project home

The official project description, documentation, and usage examples can be found at https://www.evrete.org

## Prerequisites

Evrete is Java 8+ compatible and ships with zero dependencies.

## Installation

Maven:

```xml

<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-core</artifactId>
    <version>4.0.3</version>
</dependency>
```

Gradle:

```groovy
implementation 'org.evrete:evrete-core:4.0.3'
```

### Support for annotated rules (optional)

Maven:

```xml

<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-dsl-java</artifactId>
    <version>4.0.3</version>
</dependency>
```

Gradle:

```groovy
implementation 'org.evrete:evrete-dsl-java:4.0.3'
```

## Quick start

Below is a simple example of rule that removes from session memory every integer except prime numbers.

As inline Java code:

```java
public class PrimeNumbersInline {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(ctx -> ctx.deleteFact("$i3"))
                .build();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }

            // Execute rules
            session.fire();

            // Print current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }
        service.shutdown();
    }
}
```

As annotated Java source file:

```java
public class PrimeNumbersDSLUrl {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .importRules(
                        "JAVA-SOURCE",
                        URI.create("https://www.evrete.org/examples/PrimeNumbersSource.java").toURL());

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }
            // Execute rules
            session.fire();
            // Printout current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }
    }
}
```

where the rule itself is stored externally
as [PrimeNumbersSource.java](https://www.evrete.org/examples/PrimeNumbersSource.java)

For further details see the official [documentation](https://www.evrete.org/docs/)

## License

<!--- If you're not sure which open license to use see https://choosealicense.com/--->

This project uses the following license: [MIT](https://opensource.org/licenses/MIT)

