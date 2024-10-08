package org.evrete.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.evrete.helper.TestUtils.listOf;

class CombinationIteratorTest {

    static Iterator<Integer[]> iterator(Source[] sources) {
        return new CombinationIterator<>(Integer.class, sources, Source::iterator);
    }

    @Test
    void test0() {
        Source[] sources = new Source[]{
                new Source(13),
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });

        assert combinations.size() == 1;
        assert combinations.contains(listOf(13));
    }


    @Test
    void test1() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(3, 4),
                new Source(100)
        };


        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(asList(combination.clone()));
        });

        assert combinations.size() == 2;
        assert combinations.contains(listOf(1, 3, 100));
        assert combinations.contains(listOf(1, 4, 100));

    }

    @Test
    void test2() {
        Source[] sources = new Source[]{
                new Source(1, 2),
                new Source(10),
                new Source(100),
                new Source(1000, 2000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 4;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 100, 2000));
        assert combinations.contains(listOf(2, 10, 100, 1000));
        assert combinations.contains(listOf(2, 10, 100, 2000));
    }

    @Test
    void test3() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(10),
                new Source(100),
                new Source(1000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });
        assert combinations.size() == 1;
        assert combinations.contains(listOf(1, 10, 100, 1000));
    }

    @Test
    void test4() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(10, 20),
                new Source(100),
                new Source(1000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(asList(combination.clone()));
        });
        assert combinations.size() == 2;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 20, 100, 1000));
    }


    @Test
    void test5() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(10),
                new Source(100, 200),
                new Source(1000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 2;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 200, 1000));
    }


    @Test
    void test6() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(10),
                new Source(100),
                new Source(1000, 2000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 2;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 100, 2000));
    }

    @Test
    void test7() {
        Source[] sources = new Source[]{
                new Source(1, 2),
                new Source(10),
                new Source(100),
                new Source(1000, 2000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 4;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 100, 2000));
        assert combinations.contains(listOf(2, 10, 100, 1000));
        assert combinations.contains(listOf(2, 10, 100, 2000));
    }


    @Test
    void test8() {
        Source[] sources = new Source[]{
                new Source(1, 2)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 2;
        assert combinations.contains(listOf(1));
        assert combinations.contains(listOf(2));
    }


    @Test
    void test9() {
        Source[] sources = new Source[]{
                new Source(7)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });
        assert combinations.size() == 1;
        assert combinations.contains(listOf(7));
    }


    @Test
    void test10() {
        Source[] sources = new Source[]{
                new Source(1, 2),
                new Source(10),
                new Source(100, 200),
                new Source(1000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 4;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 200, 1000));
        assert combinations.contains(listOf(2, 10, 100, 1000));
        assert combinations.contains(listOf(2, 10, 200, 1000));
    }

    @Test
    void test11() {
        Source[] sources = new Source[]{
                new Source(1),
                new Source(10, 20),
                new Source(100, 200),
                new Source(1000)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        assert combinations.size() == 4;
        assert combinations.contains(listOf(1, 10, 100, 1000));
        assert combinations.contains(listOf(1, 10, 200, 1000));
        assert combinations.contains(listOf(1, 20, 100, 1000));
        assert combinations.contains(listOf(1, 20, 200, 1000));
    }

    private static List<Integer> randomSource(int size) {
        List<Integer> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new Random().nextInt(100));
        }
        return result;
    }


    @Test
    void testRandom() {
        int maxSize = 5;
        for (int size1 = 0; size1 < maxSize; size1++) {
            List<Integer> l1 = randomSource(size1);
            for (int size2 = 0; size2 < maxSize; size2++) {
                List<Integer> l2 = randomSource(size2);
                for (int size3 = 0; size3 < maxSize; size3++) {
                    List<Integer> l3 = randomSource(size3);
                    for (int size4 = 0; size4 < maxSize; size4++) {
                        List<Integer> l4 = randomSource(size4);
                        testRandom(l1, l2, l3, l4);
                    }
                }
            }
        }
    }

    private void testRandom(List<Integer> l1, List<Integer> l2, List<Integer> l3, List<Integer> l4) {
        int expectedSize = l1.size() * l2.size() * l3.size() * l4.size();
        Source[] sources = new Source[]{
                new Source(l1),
                new Source(l2),
                new Source(l3),
                new Source(l4)
        };

        Iterator<Integer[]> iterator = iterator(sources);

        List<List<Integer>> combinations = new LinkedList<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination.clone()));
        });
        //System.out.println("Passed !!");
        assert combinations.size() == expectedSize: expectedSize + " vs " + combinations.size() + "\n\n " + Arrays.toString(sources) + "\n\n" + combinations;

        if(expectedSize > 0) {
            int count = 0;
            for(Integer i1: l1) {
                for(Integer i2: l2) {
                    for(Integer i3: l3) {
                        for(Integer i4: l4) {
                            List<Integer> entry = listOf(i1, i2, i3, i4);
                            assert combinations.contains(entry);
                            count++;
                        }
                    }
                }
            }
            assert count == expectedSize;
        }
    }



    @Test
    void testEmpty1() {
        Source[] sources = new Source[]{
                new Source(listOf()),
                new Source(listOf(10)),
                new Source(listOf(100)),
                new Source(listOf(1000, 2000))
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });
        assert combinations.isEmpty();
    }

    @Test
    void testEmpty2() {
        Source[] sources = new Source[]{
                new Source(listOf(1)),
                new Source(listOf()),
                new Source(listOf(100)),
                new Source(listOf(1000, 2000))
        };

        Iterator<Integer[]> iterator = iterator(sources);

        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });
        assert combinations.isEmpty();
    }

    @Test
    void testEmpty3() {
        Source[] sources = new Source[]{
                new Source(listOf(1)),
                new Source(listOf(10)),
                new Source(listOf(100)),
                new Source(listOf())
        };

        Iterator<Integer[]> iterator = iterator(sources);
        Set<List<Integer>> combinations = new HashSet<>();
        iterator.forEachRemaining(combination -> {
            // Save to a set
            combinations.add(listOf(combination));
        });
        assert combinations.isEmpty();
    }


    static class Source {
        private final List<Integer> list;

        public Source(Integer... list) {
            this.list = asList(list);
        }

        public Source(List<Integer> list) {
            this.list = list;
        }

        public Iterator<Integer> iterator() {
            return list.iterator();
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

}
