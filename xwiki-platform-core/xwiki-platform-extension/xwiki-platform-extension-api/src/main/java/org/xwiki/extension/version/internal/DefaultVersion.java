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
package org.xwiki.extension.version.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.extension.version.Version;

/**
 * Default implementation of {@link Version}. Note each repositories generally provide their own implementation based on
 * their own version standard.
 * <p>
 * Based on AETHER rules which is itself based on Maven specifications.
 * <p>
 * org.sonatype.aether.util.version.GenericVersion has been rewritten because it's impossible to extends it or even
 * access its details to properly implements {@link #getType()} for example.
 * 
 * @version $Id$
 */
public class DefaultVersion implements Version
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The original version string representation.
     */
    private String rawVersion;

    /**
     * The version cut in peaces for easier comparison.
     */
    private List<Element> elements;

    /**
     * @see #getType()
     */
    private Type type = Type.STABLE;

    /**
     * Used to parse the string representation of the version.
     * 
     * @version $Id$
     */
    static final class Tokenizer
    {
        /**
         * The string representation of the version.
         */
        private final String rawVersion;

        /**
         * The current index in the parsed version.
         */
        private int index;

        /**
         * @see #isNumber()
         */
        private boolean number;

        /**
         * @see #getToken()
         */
        private String token;

        /**
         * @param rawVersion the string representation of the version
         */
        public Tokenizer(String rawVersion)
        {
            this.rawVersion = (rawVersion.length() > 0) ? rawVersion : "0";
        }

        /**
         * @return the token
         */
        public String getToken()
        {
            return this.token;
        }

        /**
         * @return indicate if the token is a number
         */
        public boolean isNumber()
        {
            return this.number;
        }

        /**
         * @return move to the next token
         */
        public boolean next()
        {
            final int n = this.rawVersion.length();
            if (this.index >= n) {
                return false;
            }

            int state = -2;

            int start = this.index;
            int end = n;

            for (; this.index < n; this.index++) {
                char c = this.rawVersion.charAt(this.index);

                if (c == '.' || c == '-') {
                    end = this.index;
                    this.index++;
                    break;
                } else {
                    int digit = Character.digit(c, 10);
                    if (digit >= 0) {
                        if (state == -1) {
                            end = this.index;
                            break;
                        }
                        if (state == 0) {
                            // normalize numbers and strip leading zeros
                            start++;
                        }
                        state = (state > 0 || digit > 0) ? 1 : 0;
                    } else {
                        if (state >= 0) {
                            end = this.index;
                            break;
                        }
                        state = -1;
                    }
                }
            }

            if (start < end) {
                this.token = this.rawVersion.substring(start, end);
                this.number = state >= 0;
            } else {
                this.token = "0";
                this.number = true;
            }

            return true;
        }

        @Override
        public String toString()
        {
            return this.token.toString();
        }
    }

    /**
     * A peace of the version.
     * 
     * @version $Id$
     */
    static final class Element implements Comparable<Element>
    {
        /**
         * Message used in the exception produced when one of the {@link ElementType} is unknown.
         */
        private static final String ERROR_UNKNOWNKIND = "Unknown version element kind ";

        /**
         * The kind of element.
         * 
         * @version $Id$
         */
        enum ElementType
        {
            /**
             * An integer.
             */
            INT,

            /**
             * A known qualifier id.
             */
            QUALIFIER,

            /**
             * An unknown literal string.
             */
            STRING
        }

        /**
         * The list of known qualifiers.
         */
        private static final Map<String, Integer> QUALIFIERS;

        static {
            QUALIFIERS = new HashMap<String, Integer>();
            QUALIFIERS.put("alpha", Integer.valueOf(-5));
            QUALIFIERS.put("a", Integer.valueOf(-5));
            QUALIFIERS.put("beta", Integer.valueOf(-4));
            QUALIFIERS.put("b", Integer.valueOf(-4));
            QUALIFIERS.put("milestone", Integer.valueOf(-3));
            QUALIFIERS.put("cr", Integer.valueOf(-2));
            QUALIFIERS.put("rc", Integer.valueOf(-2));
            QUALIFIERS.put("snapshot", Integer.valueOf(-1));
            QUALIFIERS.put("ga", Integer.valueOf(0));
            QUALIFIERS.put("final", Integer.valueOf(0));
            QUALIFIERS.put("", Integer.valueOf(0));
            QUALIFIERS.put("sp", Integer.valueOf(1));
        }

        /**
         * The kind of element.
         */
        private final ElementType elementType;

        /**
         * The value of the element.
         */
        private final Object value;

        /**
         * @see #getVersionType()
         */
        private Type versionType = Type.STABLE;

        /**
         * @param tokenizer the token from which to create the version element
         */
        public Element(Tokenizer tokenizer)
        {
            String token = tokenizer.getToken();
            if (tokenizer.isNumber()) {
                try {
                    this.elementType = ElementType.INT;
                    this.value = Integer.valueOf(token);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                String lowerCaseToken = token.toLowerCase(Locale.ENGLISH);
                Integer qualifier = QUALIFIERS.get(lowerCaseToken);
                if (qualifier != null) {
                    this.elementType = ElementType.QUALIFIER;
                    this.value = qualifier;
                    if (qualifier.intValue() == -1) {
                        this.versionType = Type.SNAPSHOT;
                    } else if (qualifier < 0) {
                        this.versionType = Type.BETA;
                    }
                } else {
                    this.elementType = ElementType.STRING;
                    this.value = lowerCaseToken;
                }
            }
        }

        /**
         * @return indicate of the element is a number
         */
        public boolean isNumber()
        {
            return this.elementType == ElementType.INT || this.elementType == ElementType.QUALIFIER;
        }

        /**
         * @return the type of the version element
         */
        public Type getVersionType()
        {
            return this.versionType;
        }

        @Override
        public int compareTo(Element that)
        {
            int rel;

            if (that == null) {
                // null in this context denotes the pad element (0 or "ga")
                switch (this.elementType) {
                    case STRING:
                        rel = 1;
                        break;
                    case INT:
                    case QUALIFIER:
                        rel = (Integer) this.value;
                        break;
                    default:
                        throw new IllegalStateException(ERROR_UNKNOWNKIND + this.elementType);
                }
            } else {
                rel = this.elementType.compareTo(that.elementType);
                if (rel == 0) {
                    switch (this.elementType) {
                        case INT:
                        case QUALIFIER:
                            rel = (Integer) this.value - (Integer) that.value;
                            break;
                        case STRING:
                            rel = ((String) this.value).compareToIgnoreCase((String) that.value);
                            break;
                        default:
                            throw new IllegalStateException(ERROR_UNKNOWNKIND + this.elementType);
                    }
                }
            }

            return rel;
        }

        @Override
        public boolean equals(Object obj)
        {
            return (obj instanceof Element) && compareTo((Element) obj) == 0;
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(this.value);
            builder.append(this.elementType);

            return builder.toHashCode();
        }

        @Override
        public String toString()
        {
            return this.value.toString();
        }

    }

    /**
     * @param rawVersion the original string representation of the version
     */
    public DefaultVersion(String rawVersion)
    {
        setVersion(rawVersion);
    }

    /**
     * Create a new {@link DefaultVersion} by cloning the provided version.
     * 
     * @param version the version to copy
     */
    public DefaultVersion(Version version)
    {
        this(version.getValue());
    }

    /**
     * Make sure the version has been parsed.
     */
    private void initElements()
    {
        if (this.elements == null) {
            parse();
        }
    }

    /**
     * @param rawVersion the string representation to parse
     */
    private void setVersion(String rawVersion)
    {
        this.rawVersion = rawVersion;
    }

    /**
     * Parse the string representation of the version into separated elements.
     */
    private void parse()
    {
        this.elements = new ArrayList<Element>();

        for (Tokenizer tokenizer = new Tokenizer(this.rawVersion); tokenizer.next();) {
            Element element = new Element(tokenizer);
            this.elements.add(element);
            if (element.getVersionType() != Type.STABLE) {
                this.type = element.getVersionType();
            }
        }

        trimPadding(this.elements);
    }

    /**
     * Remove empty elements.
     * 
     * @param elements the list of clean
     */
    private static void trimPadding(List<Element> elements)
    {
        for (ListIterator<Element> it = elements.listIterator(elements.size()); it.hasPrevious();) {
            Element element = it.previous();

            if (element.compareTo(null) == 0) {
                it.remove();
            } else {
                break;
            }
        }
    }

    @Override
    public Type getType()
    {
        initElements();

        return this.type;
    }

    // Version

    @Override
    public String getValue()
    {
        return this.rawVersion;
    }

    // Object

    @Override
    public String toString()
    {
        return getValue();
    }

    @Override
    public int hashCode()
    {
        initElements();

        return this.elements.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        boolean equals;

        if (obj instanceof DefaultVersion) {
            equals = equals((DefaultVersion) obj);
        } else if (obj instanceof Version) {
            equals = equals(new DefaultVersion(((Version) obj).getValue()));
        } else {
            equals = false;
        }

        return equals;
    }

    /**
     * @param version the version
     * @return true if the provided version is equals to this version
     */
    public boolean equals(DefaultVersion version)
    {
        return compareTo(version) == 0;
    }

    @Override
    public int compareTo(Version version)
    {
        if (version == this) {
            return 0;
        }

        if (version instanceof DefaultVersion) {
            return compareTo((DefaultVersion) version);
        } else {
            return compareTo(new DefaultVersion(version.getValue()));
        }
    }

    /**
     * @param version the version to compare
     * @return a negative integer, zero, or a positive integer as this version is less than, equal to, or greater than
     *         the specified version
     */
    public int compareTo(DefaultVersion version)
    {
        initElements();
        version.initElements();

        final List<Element> otherElements = version.elements;

        boolean number = true;

        int rel;

        for (int index = 0;; index++) {
            if (index >= this.elements.size() && index >= otherElements.size()) {
                return 0;
            } else if (index >= this.elements.size()) {
                return -comparePadding(otherElements, index, null);
            } else if (index >= otherElements.size()) {
                return comparePadding(this.elements, index, null);
            }

            Element thisElement = this.elements.get(index);
            Element thatElement = otherElements.get(index);

            if (thisElement.isNumber() != thatElement.isNumber()) {
                if (number == thisElement.isNumber()) {
                    rel = comparePadding(this.elements, index, Boolean.valueOf(number));
                } else {
                    rel = -comparePadding(otherElements, index, Boolean.valueOf(number));
                }

                break;
            } else {
                rel = thisElement.compareTo(thatElement);
                if (rel != 0) {
                    break;
                }
                number = thisElement.isNumber();
            }
        }

        return rel;
    }

    /**
     * Compare the end of the version with 0.
     * 
     * @param elements the elements to compare to 0
     * @param index the index where to start comparing with 0
     * @param number indicate of the previous element is a number
     * @return the comparison result
     */
    private static int comparePadding(List<Element> elements, int index, Boolean number)
    {
        int rel = 0;

        for (Iterator<Element> it = elements.listIterator(index); it.hasNext();) {
            Element element = it.next();
            if (number != null && number.booleanValue() != element.isNumber()) {
                break;
            }

            rel = element.compareTo(null);
            if (rel != 0) {
                break;
            }
        }

        return rel;
    }

    // Serializable

    /**
     * @param out the stream
     * @throws IOException error when serializing the version
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeObject(getValue());
    }

    /**
     * @param in the stream
     * @throws IOException error when unserializing the version
     * @throws ClassNotFoundException error when unserializing the version
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        setVersion((String) in.readObject());
    }
}
