package faces.apps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**Simple CSV writer.*/
class CSVWriter {
    private static final Pattern CSVEscape = Pattern.compile("[\n,]");
    private final StringBuilder _buffer = new StringBuilder();

    /**Save to file and close.
     @throws IOException if fail to write to file.*/
    public void save(String filepath) throws IOException {
        try (PrintWriter fout = new PrintWriter(filepath)) {
            fout.print(_buffer.toString());
        }
    }
    /**Clear allocated buffer.*/
    public void clear() { _buffer.setLength(0); }

    /**Create RowBuilder instance for writing new row content.*/
    public RowBuilder newRow() { return new RowBuilder(); }
    /**Create RowBuilder instance with initial item capacity for writing new row content.*/
    public RowBuilder newRow(int capacity) { return new RowBuilder(capacity); }

    /**Internal builder class for creating new row content.*/
    public class RowBuilder {
        private final List<String> _content;

        private RowBuilder() { this(0); }
        private RowBuilder(int capacity) { _content = new ArrayList<>(capacity); }

        /**Add item to row.
         @apiNote added item will be converted to string by {@link Object#toString()} method. */
        public RowBuilder add(Object item) {
            final String str = _escape(item.toString(), "\"", "\"\"");
            if (CSVEscape.matcher(str).find())
                _content.add("\"" + str + "\"");
            else
                _content.add(str);
            return this;
        }
        /**Clear all added items.*/
        public RowBuilder clear() { _content.clear(); return this; }

        /**Put added items as new row to buffer.
         <p>If there's no item in this row, nothing will be written to buffer.</p> */
        public void build() {
            if (_content.isEmpty()) return;
            for (int i = 0; i < _content.size() - 1; ++i)
                _buffer.append(_content.get(i)).append(",");
            _buffer.append(_content.get(_content.size() - 1)).append("\n");
        }
    } // ! class CSVWriter.RowBuilder

    /**Escape all quote and back-slash characters and return a new string instance.
     @see <a href="http://hg.openjdk.java.net/jdk9/jdk9/jdk/rev/db30d5179fe7">JDK 9's patch.</a> */
    private static String _escape(String data, String target, String replacement) {
        String starget = target.toString();
        String srepl = replacement.toString();
        int j = data.indexOf(starget);
        if (j < 0) {
            return data;
        }
        int targLen = starget.length();
        int targLen1 = Math.max(targLen, 1);
        final char[] value = data.toCharArray();
        final char[] replValue = srepl.toCharArray();
        int newLenHint = value.length - targLen + replValue.length;
        if (newLenHint < 0) {
            throw new OutOfMemoryError();
        }
        StringBuilder sb = new StringBuilder(newLenHint);
        int i = 0;
        do {
            sb.append(value, i, j - i)
                .append(replValue);
            i = j + targLen;
        } while (j < value.length && (j = data.indexOf(starget, j + targLen1)) > 0);

        return sb.append(value, i, value.length - i).toString();
    }
} // ! class CSVWriter
