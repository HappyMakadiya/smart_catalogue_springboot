package com.example.smartcatalog.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that serialises a Java {@code float[]} into the
 * PostgreSQL {@code vector} literal format and back.
 *
 * <p>pgvector stores vectors as the string {@code [v1,v2,...,vN]} in its wire
 * format, so JDBC sees the column as a plain {@code String}. This converter
 * handles the translation transparently, keeping the entity field strongly typed.
 *
 * <p>The converter is auto-applied to every {@code float[]} persistent attribute
 * whose {@code @Column} definition references the {@code vector} type, because
 * {@code autoApply = false} keeps the mapping explicit (annotate the field with
 * {@code @Convert(converter = VectorFloatConverter.class)}).
 */
@Converter
public class VectorFloatConverter implements AttributeConverter<float[], String> {

    /**
     * Converts a Java {@code float[]} to the pgvector string literal
     * {@code [v1,v2,...,vN]}.
     *
     * @param vector the embedding array; may be {@code null} for un-embedded products
     * @return the pgvector literal, or {@code null} if {@code vector} is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(float[] vector) {
        if (vector == null) return null;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Parses the pgvector literal {@code [v1,v2,...,vN]} back into a
     * {@code float[]}.
     *
     * @param dbData the raw string returned by JDBC; may be {@code null}
     * @return the parsed float array, or {@code null} if {@code dbData} is {@code null}
     */
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;

        // Strip surrounding brackets, split on comma
        String stripped = dbData.strip();
        if (stripped.startsWith("[")) stripped = stripped.substring(1);
        if (stripped.endsWith("]"))   stripped = stripped.substring(0, stripped.length() - 1);

        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].strip());
        }
        return result;
    }
}
