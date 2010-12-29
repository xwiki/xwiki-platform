/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.properties.converter;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.properties.ConverterManager;

/**
 * Base class for all {@link java.util.Collection} converters.
 * 
 * @version $Id$
 * @since 3.0M1
 */
public abstract class AbstractCollectionConverter extends AbstractConverter
{
    /**
     * Quote char.
     */
    protected static final char QUOTECHAR = '"';

    /**
     * Quote string.
     */
    protected static final String QUOTESTRING = "\"";

    /**
     * Used to convert collection elements.
     */
    @Requirement
    private ConverterManager converterManager;

    /**
     * @see #setDelimiter(char)
     */
    private char delimiter = ',';

    /**
     * @see #setAllowedChars(char)
     */
    private char[] allowedChars = new char[] {'.', '-'};

    /**
     * @return the converter manager.
     */
    public ConverterManager getConverterManager()
    {
        return converterManager;
    }

    /**
     * Set the delimiter to be used for parsing a delimited String.
     * 
     * @param delimiter The delimiter [default ',']
     */
    public void setDelimiter(char delimiter)
    {
        this.delimiter = delimiter;
    }

    /**
     * @return the delimiter
     */
    public char getDelimiter()
    {
        return delimiter;
    }

    /**
     * Set the allowed characters to be used for parsing a delimited String.
     * 
     * @param allowedChars Characters which are to be considered as part of the tokens when parsing a delimited String
     *            [default is '.' and '-']
     */
    public void setAllowedChars(char[] allowedChars)
    {
        this.allowedChars = allowedChars;
    }

    @Override
    protected <T> T convertToType(Type targetType, Object value)
    {
        Type genericType = null;
        if (targetType instanceof ParameterizedType) {
            genericType = ((ParameterizedType) targetType).getActualTypeArguments()[0];
        }

        return (T) parseElements(value.toString(), genericType);
    }

    /**
     * <p>
     * Parse an incoming String of the form similar to an array initializer in the Java language into a
     * <code>List</code> individual Strings for each element, according to the following rules.
     * </p>
     * <ul>
     * <li>The string is expected to be a comma-separated list of values.</li>
     * <li>The string may optionally have matching '{' and '}' delimiters around the list.</li>
     * <li>Whitespace before and after each element is stripped.</li>
     * <li>Elements in the list may be delimited by single or double quotes. Within a quoted elements, the normal Java
     * escape sequences are valid.</li>
     * </ul>
     * 
     * @param value String value to be parsed
     * @param genericType the generic type
     * @return List of parsed elements.
     * @throws ConversionException if the syntax of <code>value</code> is not syntactically valid
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     */
    protected Collection parseElements(String value, Type genericType)
    {
        String cleanedValue = cleanValue(value);

        try {
            // Set up a StreamTokenizer on the characters in this String
            StreamTokenizer st = createStreamTokenizer(cleanedValue);

            // Split comma-delimited tokens into a List
            Collection list = newCollection();
            while (true) {
                int ttype = st.nextToken();
                if (ttype == StreamTokenizer.TT_WORD || ttype > 0) {
                    if (st.sval != null) {
                        Object objValue = st.sval;
                        if (genericType != null && genericType != String.class) {
                            objValue = this.converterManager.convert(genericType, objValue);
                        }

                        list.add(objValue);
                    }
                } else if (ttype == StreamTokenizer.TT_EOF) {
                    break;
                } else {
                    throw new ConversionException("Encountered token of type " + ttype + " parsing elements.");
                }
            }

            // Return the completed list
            return list;
        } catch (IOException e) {
            throw new ConversionException("Error converting from String: " + e.getMessage(), e);
        }
    }

    /**
     * @return the modifiable {@link Collection} to fill
     */
    protected Collection newCollection()
    {
        return new ArrayList();
    }

    /**
     * @param value the string to cleanup
     * @return the clean version of the string
     */
    private String cleanValue(String value)
    {
        // Trim any matching '{' and '}' delimiters
        String cleanedValue = value.trim();
        if (cleanedValue.startsWith("{") && cleanedValue.endsWith("}")) {
            cleanedValue = cleanedValue.substring(1, cleanedValue.length() - 1);
        }

        return cleanedValue;
    }

    /**
     * Create and initializer a {@link StreamTokenizer} to parse the value.
     * 
     * @param value the string to parse
     * @return the {@link StreamTokenizer} used to parse the string
     */
    protected StreamTokenizer createStreamTokenizer(String value)
    {
        // Set up a StreamTokenizer on the characters in this String
        StreamTokenizer st = new StreamTokenizer(new StringReader(value));
        // Set the delimiters
        st.whitespaceChars(getDelimiter(), getDelimiter());
        // Needed to turn off numeric flag
        st.ordinaryChars('0', '9');
        // Needed to make part of tokens
        st.wordChars('0', '9');
        for (int i = 0; i < allowedChars.length; i++) {
            st.ordinaryChars(allowedChars[i], allowedChars[i]);
            st.wordChars(allowedChars[i], allowedChars[i]);
        }

        return st;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.converter.AbstractConverter#convertToString(java.lang.Object)
     */
    @Override
    protected String convertToString(Object value)
    {
        StringBuilder sb = new StringBuilder();

        Collection collection = (Collection) value;

        for (Object element : collection) {
            if (sb.length() > 0) {
                sb.append(getDelimiter());
            }

            String elementString = getConverterManager().convert(String.class, element);

            if (elementString != null) {
                boolean containsDelimiter = StringUtils.contains(elementString, getDelimiter());

                if (containsDelimiter) {
                    sb.append(QUOTESTRING);
                }
                sb.append(elementString != null ? elementString.replace("\\", "\\\\").replace(QUOTESTRING, "\\\"")
                    .replace("'", "\\'") : null);
                if (containsDelimiter) {
                    sb.append(QUOTESTRING);
                }
            }
        }

        return sb.toString();
    }
}
