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
package org.xwiki.extension.version;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

/**
 * Default implementation of {@link Version}.
 * <p>
 * Mostly follow Maven comparison rules.
 * 
 * @version $Id$
 */
public class DefaultVersion implements Version
{
    private final String value;

    private final String canonical;

    private final ListItem items;

    private interface Item
    {
        public static final int INTEGER_ITEM = 0;

        public static final int STRING_ITEM = 1;

        public static final int LIST_ITEM = 2;

        public int compareTo(Item item);

        public int getType();

        public boolean isNull();
    }

    /**
     * Represents a numeric item in the version item list.
     */
    private static class IntegerItem implements Item
    {

        private final BigInteger value;

        public static final IntegerItem ZERO = new IntegerItem();

        private IntegerItem()
        {
            this.value = BigInteger.ZERO;
        }

        public IntegerItem(String str)
        {
            this.value = new BigInteger(str);
        }

        public int getType()
        {
            return INTEGER_ITEM;
        }

        public boolean isNull()
        {
            return BigInteger.ZERO.equals(value);
        }

        public int compareTo(Item item)
        {
            if (item == null) {
                return BigInteger.ZERO.equals(value) ? 0 : 1; // 1.0 == 1, 1.1 > 1
            }

            switch (item.getType()) {
                case INTEGER_ITEM:
                    return value.compareTo(((IntegerItem) item).value);

                case STRING_ITEM:
                    return 1; // 1.1 > 1-sp

                case LIST_ITEM:
                    return 1; // 1.1 > 1-1

                default:
                    throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        public String toString()
        {
            return value.toString();
        }
    }

    /**
     * Represents a string in the version item list, usually a qualifier.
     */
    private static class StringItem implements Item
    {
        private final static Map<String, String> QUALIFIERS = new HashMap<String, String>();

        private final static Map<String, String> ALIASES = new HashMap<String, String>();

        static {
            String[] qualifiers = {"alpha", "beta", "milestone", "rc", "snapshot", "", "sp"};
            for (int i = 0; i < qualifiers.length; i++) {
                QUALIFIERS.put(qualifiers[i], String.valueOf(i));
            }

            ALIASES.put("ga", "");
            ALIASES.put("final", "");
            ALIASES.put("cr", "rc");
        }

        /**
         * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
         * the version older than one without a qualifier, or more recent.
         */
        private static final String RELEASE_VERSION_INDEX = QUALIFIERS.get("");

        private static final String LEXICAL_PREFIX = String.valueOf(QUALIFIERS.size());

        private final String value;

        public StringItem(String value, boolean followedByDigit)
        {
            if (followedByDigit && value.length() == 1) {
                // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                switch (value.charAt(0)) {
                    case 'a':
                        value = "alpha";
                        break;
                    case 'b':
                        value = "beta";
                        break;
                    case 'm':
                        value = "milestone";
                        break;
                }
            }
            String alias = ALIASES.get(value);
            this.value = (alias != null) ? alias : value;
        }

        public int getType()
        {
            return STRING_ITEM;
        }

        public boolean isNull()
        {
            return value.length() <= 0;
        }

        /**
         * Returns a comparable value for a qualifier. This method both takes into account the ordering of known
         * qualifiers as well as lexical ordering for unknown qualifiers. just returning an Integer with the index here
         * is faster, but requires a lot of if/then/else to check for -1 or QUALIFIERS.size and then resort to lexical
         * ordering. Most comparisons are decided by the first character, so this is still fast. If more characters are
         * needed then it requires a lexical sort anyway.
         * 
         * @param qualifier
         * @return an equivalent value that can be used with lexical comparison
         */
        private static String comparableQualifier(String qualifier)
        {
            String i = QUALIFIERS.get(qualifier);

            return i != null ? i : LEXICAL_PREFIX + qualifier;
        }

        public int compareTo(Item item)
        {
            if (item == null) {
                // 1-rc < 1, 1-ga > 1
                return comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
                case INTEGER_ITEM:
                    return -1; // 1.any < 1.1 ?

                case STRING_ITEM:
                    return comparableQualifier(value).compareTo(comparableQualifier(((StringItem) item).value));

                case LIST_ITEM:
                    return -1; // 1.any < 1-1

                default:
                    throw new IllegalStateException("invalid item: " + item.getClass());
            }
        }

        public String toString()
        {
            return value;
        }
    }

    /**
     * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
     * with '-(number)' in the version specification).
     */
    private static class ListItem extends ArrayList<Item> implements Item
    {
        public int getType()
        {
            return LIST_ITEM;
        }

        public boolean isNull()
        {
            return (size() == 0);
        }

        void normalize()
        {
            for (ListIterator<Item> iterator = listIterator(size()); iterator.hasPrevious();) {
                Item item = iterator.previous();
                if (item.isNull()) {
                    iterator.remove(); // remove null trailing items: 0, "", empty list
                } else {
                    break;
                }
            }
        }

        public int compareTo(Item item)
        {
            if (item == null) {
                if (size() == 0) {
                    return 0; // 1-0 = 1- (normalize) = 1
                }
                Item first = (Item) get(0);
                return first.compareTo(null);
            }
            switch (item.getType()) {
                case INTEGER_ITEM:
                    return -1; // 1-1 < 1.0.x

                case STRING_ITEM:
                    return 1; // 1-1 > 1-sp

                case LIST_ITEM:
                    Iterator<Item> left = iterator();
                    Iterator<Item> right = ((ListItem) item).iterator();

                    while (left.hasNext() || right.hasNext()) {
                        Item l = left.hasNext() ? left.next() : null;
                        Item r = right.hasNext() ? right.next() : null;

                        // if this is shorter, then invert the compare and mul with -1
                        int result = l == null ? -r.compareTo(l) : l.compareTo(r);

                        if (result != 0) {
                            return result;
                        }
                    }

                    return 0;

                default:
                    throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        public String toString()
        {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append('(');
            for (Iterator<Item> iter = iterator(); iter.hasNext();) {
                buffer.append(iter.next());
                if (iter.hasNext()) {
                    buffer.append(',');
                }
            }
            buffer.append(')');
            return buffer.toString();
        }
    }

    /**
     * Creates a Maven version from the specified string.
     * 
     * @param version The version string to parse, must not be {@code null}.
     */
    public DefaultVersion(String version)
    {
        this.value = version;

        items = new ListItem();

        version = version.toLowerCase(Locale.ENGLISH);

        ListItem list = items;

        Stack<Item> stack = new Stack<Item>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                if (isDigit) {
                    list.normalize(); // 1.0-* = 1-*

                    if ((i + 1 < version.length()) && Character.isDigit(version.charAt(i + 1))) {
                        // new ListItem only if previous were digits and new char is a digit,
                        // ie need to differentiate only 1.1 from 1-1
                        list.add(list = new ListItem());

                        stack.push(list);
                    }
                }
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(new StringItem(version.substring(startIndex, i), true));
                    startIndex = i;
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            list.add(parseItem(isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }

        canonical = items.toString();
    }

    private static Item parseItem(boolean isDigit, String buf)
    {
        return isDigit ? new IntegerItem(buf) : new StringItem(buf, false);
    }

    public int compareTo(Version o)
    {
        return items.compareTo(((DefaultVersion) o).items);
    }

    public boolean equals(Object o)
    {
        return (o instanceof DefaultVersion) && canonical.equals(((DefaultVersion) o).canonical);
    }

    public int hashCode()
    {
        return canonical.hashCode();
    }

    public String toString()
    {
        return value;
    }
}
