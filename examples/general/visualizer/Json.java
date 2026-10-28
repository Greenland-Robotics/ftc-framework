package visualizer;

/**
 * Just enough of a JSON writer for the visualizer's state, so it needs no libraries. Also used to
 * describe a {@link TreeVisualizer#scene scene}.
 */
public final class Json {
    private final StringBuilder out = new StringBuilder();
    private boolean needsComma;

    public Json begin() {
        comma();
        out.append('{');
        needsComma = false;
        return this;
    }

    public Json end() {
        out.append('}');
        needsComma = true;
        return this;
    }

    public Json beginArray() {
        comma();
        out.append('[');
        needsComma = false;
        return this;
    }

    public Json endArray() {
        out.append(']');
        needsComma = true;
        return this;
    }

    public Json name(String name) {
        comma();
        string(name);
        out.append(':');
        needsComma = false;
        return this;
    }

    public Json field(String name, Object value) {
        return name(name).value(value);
    }

    public Json nullValue() {
        return value(null);
    }

    public Json value(Object value) {
        comma();
        if (value == null) {
            out.append("null");
        } else if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            // Two decimals is plenty for drawing, and keeps the scene small
            out.append(Double.isFinite(number) ? String.valueOf(Math.round(number * 100) / 100.0) : "null");
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else {
            string(value.toString());
        }
        needsComma = true;
        return this;
    }

    private void comma() {
        if (needsComma) out.append(',');
    }

    private void string(String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        out.append('"');
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
