package dev.jbaby.wicket.testkit;

import static java.util.stream.Collectors.joining;

import dev.jbaby.wicket.testkit.locator.Words;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// A table of cell texts, as [WicketTestkit#assertRows] compares a repeater's rows with: a header
/// of `wicket:id`s and a row of cell texts per repeater row.
record Table(List<String> header, List<List<String>> rows) {

    /// A `|` that isn't written as `\|`.
    private static final Pattern SEPARATOR = Pattern.compile("(?<!\\\\)\\|");

    /// Reads a table written as lines like `| ACME | open |`, blank lines aside.
    ///
    /// @throws IllegalArgumentException if it has no header, a line that doesn't start and end
    ///     with `|`, a blank id, or a row with another number of cells than the header
    static Table parse(String text) {
        List<String> header = null;
        var rows = new ArrayList<List<String>>();
        var lines = text.lines().toList();
        for (var index = 0; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            var number = index + 1;
            var cells = cells(line, number);
            if (header == null) {
                if (cells.contains("")) {
                    throw new IllegalArgumentException("line " + number + " of the table has a blank id");
                }
                header = cells;
            } else if (cells.size() != header.size()) {
                throw new IllegalArgumentException("line " + number + " of the table has " + cells.size()
                    + (cells.size() == 1 ? " cell" : " cells") + ", but the header has " + header.size());
            } else {
                rows.add(cells);
            }
        }
        if (header == null) {
            throw new IllegalArgumentException("the table has no header");
        }
        return new Table(header, rows);
    }

    private static List<String> cells(String line, int number) {
        if (line.length() < 2 || !line.startsWith("|") || !line.endsWith("|") || line.endsWith("\\|")) {
            throw new IllegalArgumentException("line " + number + " of the table doesn't start and end with |");
        }
        return Stream.of(SEPARATOR.split(line.substring(1, line.length() - 1), -1))
            .map(cell -> Words.normalized(cell.replace("\\|", "|")))
            .toList();
    }


    /// The width of every column: its widest cell, header included and `|` written as `\|`, in
    /// any of the tables, which have the same header.
    static int[] widths(Table... tables) {
        var widths = new int[tables[0].header.size()];
        for (var table : tables) {
            for (var line : table.lines()) {
                for (var column = 0; column < widths.length; column++) {
                    widths[column] = Math.max(widths[column], escaped(line.get(column)).length());
                }
            }
        }
        return widths;
    }

    /// One line per row, the header first, as `| <cell> | <cell> |` with every column padded to
    /// its width.
    String written(int[] widths) {
        return lines().stream()
            .map(line -> IntStream.range(0, widths.length)
                .mapToObj(column -> padded(escaped(line.get(column)), widths[column]))
                .collect(joining(" | ", "| ", " |")))
            .collect(joining("\n"));
    }

    private List<List<String>> lines() {
        var lines = new ArrayList<List<String>>();
        lines.add(header);
        lines.addAll(rows);
        return lines;
    }

    private static String escaped(String cell) {
        return cell.replace("|", "\\|");
    }

    private static String padded(String text, int width) {
        return text + " ".repeat(width - text.length());
    }
}
