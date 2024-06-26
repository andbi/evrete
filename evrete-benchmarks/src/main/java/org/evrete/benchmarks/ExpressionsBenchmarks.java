package org.evrete.benchmarks;

import org.evrete.benchmarks.jmh.Expressions;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class ExpressionsBenchmarks {

    public static void main(String[] args) throws RunnerException{
        TimeValue duration = TimeValue.milliseconds(500L);
        int iterations = 8;
        Options opt = new OptionsBuilder()
                .include(Expressions.class.getSimpleName())
                //.jvmArgsPrepend("--add-opens=java.base/java.io=ALL-UNNAMED")
                .warmupIterations(iterations)
                .warmupTime(duration)
                .measurementIterations(iterations)
                .measurementTime(duration)
                .build();

        new Runner(opt).run();
    }

}
