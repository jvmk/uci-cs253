import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercise 26.1 in "Exercises in Programming Style" by Professor C. Lopes. First part of the homework of
 * week 6 of UCI CS253 (Fall 2018 edition). In this exercise, we're to obey a "spreadsheet" style of programming.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TwentySix {

    /**
     * Models a column in the spreadsheet.
     * @param <V> The collection type for the cells in this column.
     */
    private static class Column<V extends Collection<?>> {

        /**
         * The cells (rows) in this column.
         */
        private V mCells;

        /**
         * The formula that produces the contents of the column (on each update to the spreadsheet).
         */
        private Supplier<V> mFormula;

        private Column(Supplier<V> formula) {
            mFormula = formula;
        }

        private V getCells() {
            return mCells;
        }

        /**
         * Refreshes this column by reevaluating {@link #mFormula} and assigning its output to {@link #mCells}.
         */
        private void refresh() {
            mCells = mFormula.get();
        }
    }

    /**
     * Basic helper pair/tuple.
     *
     * @param <T1>
     *         Type of element 1 of the pair.
     * @param <T2>
     *         Type of element 2 of the pair.
     */
    private static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;

        private Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }

        private T1 first() {
            return mItem1;
        }

        private T2 second() {
            return mItem2;
        }
    }

    /**
     * Models the entire spreadsheet.
     */
    private static class Spreadsheet {

        private final Column<List<String>> allWordsColumn = new Column<>(() -> {
            try (Stream<String> lines = Files.lines(Paths.get(Spreadsheet.this.mFilepath))) {
                // Make all lowercase, convert non-alphanumeric chars to spaces, and split into separate words.
                // Note as we stream on each line of the file, we just first join all lines (using a space as a
                // delimiter) before we invoke split() on the resulting joined string.
                String[] words = lines.map(l -> l.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(" ")).split("\\s+");
                return Arrays.asList(words);
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception.
                throw new RuntimeException(ioe);
            }
        });

        private final Column<Set<String>> stopWordsColumn = new Column<>(() -> {
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Collect stop words to set.
                // Note: explicitly use LinkedHashSet to preserve insertion order and get O(1) complexity contains().
                Set<String> stopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                        collect(Collectors.toCollection(LinkedHashSet::new));
                // Add all 1-character words to stop words set.
                // Source: 'alphabet' courtesy of https://stackoverflow.com/a/17575926/1214974
                char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                for (char c : alphabet) {
                    stopWords.add(Character.toString(c));
                }
                return stopWords;
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception
                throw new RuntimeException(ioe);
            }
        });

        private final Column<List<String>> nonStopWordsColumn = new Column<>(() -> {
            return allWordsColumn.getCells().stream().
                    filter(word -> !stopWordsColumn.getCells().contains(word)).
                    collect(Collectors.toList());
        });

        // Use LinkedHashSet to preserve insertion order since we map elements in this column to their respective counts
        // in the counts column by corresponding indices.
        private final Column<LinkedHashSet<String>> uniqueWordsColumn =
                new Column<>(() -> new LinkedHashSet<>(nonStopWordsColumn.getCells()));

        private final Column<List<Long>> countsColumn = new Column<>(() -> {
            return uniqueWordsColumn.getCells().stream()
                    // Count number of occurrences of each unique word.
                    // Note: this is super inefficient since we traverse the full list of words for each unique word.
                    .map(uniqueWord ->
                            nonStopWordsColumn.getCells().stream().
                                    filter(nonStopWord -> nonStopWord.equals(uniqueWord)).count()
                    ).collect(Collectors.toList());
        });

        private final Column<List<Pair<String, Long>>> sortedDataColumn = new Column<>(() -> {
            List<Pair<String, Long>> result = new ArrayList<>();
            int index = 0;
            // IMPORTANT: Relies on the fact that uniqueWordsColumn uses a LinkedHashSet so that iteration order is
            // equal to insertion order.
            for (String uniqueWord : uniqueWordsColumn.getCells()) {
                result.add(new Pair<>(uniqueWord, countsColumn.getCells().get(index)));
                index++;
            }
            Collections.sort(result, (p1, p2) -> -p1.second().compareTo(p2.second()));
            return result;
        });

        private final List<Column<?>> mAllColumns = new ArrayList<>();

        private final String mFilepath;

        private Spreadsheet(String filepath) {
            mFilepath = filepath;
            mAllColumns.add(allWordsColumn);
            mAllColumns.add(stopWordsColumn);
            mAllColumns.add(nonStopWordsColumn);
            mAllColumns.add(uniqueWordsColumn);
            mAllColumns.add(countsColumn);
            mAllColumns.add(sortedDataColumn);
        }

        /**
         * The active procedure over the columns of data.
         * Call this every time the input changes, or periodically.
         */
        private void update() {
            for(Column<?> column : mAllColumns) {
//                column.setCells(column.getFormula().get());
                // TODO is this allowed according to style constraints? Or must we explicitly set the variable?
                column.refresh();
            }
        }

    }

    public static void main(String[] args) {
        Spreadsheet spreadsheet = new Spreadsheet(args[0]);
        spreadsheet.update();
        spreadsheet.sortedDataColumn.getCells().subList(0, 25).forEach(p ->
                System.out.println(String.format("%s  -  %d", p.first(), p.second())));
    }

}
