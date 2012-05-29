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
package com.xpn.xwiki.internal.diff;

/**
 * Represents a line from a unified diff.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class UnifiedDiffLine
{
    /**
     * The possible types of lines you can find within a unified diff.
     */
    public static enum LineType
    {
        /** A line that has been added. */
        ADDED('+'),

        /** A line that has been removed. */
        DELETED('-'),

        /** A line that shows the context where a change has been made. */
        CONTEXT(' ');

        /**
         * The symbol associated with this line type.
         */
        private final char symbol;

        /**
         * Creates a new line type that has the given symbol associated.
         * 
         * @param symbol the symbol associated with this line type
         */
        LineType(char symbol)
        {
            this.symbol = symbol;
        }

        /**
         * @return the symbol associated with this line type
         */
        public char getSymbol()
        {
            return symbol;
        }
    }

    /**
     * The line number.
     */
    private final int number;

    /**
     * The line type.
     */
    private final LineType type;

    /**
     * The line content.
     */
    private final Object content;

    /**
     * Creates a new line in a unified diff.
     * 
     * @param number the line number
     * @param type the line type
     * @param content the line content
     */
    public UnifiedDiffLine(int number, LineType type, Object content)
    {
        this.number = number;
        this.type = type;
        this.content = content;
    }

    /**
     * @return the line number
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * @return the line type
     */
    public LineType getType()
    {
        return type;
    }

    /**
     * @return the line content
     */
    public Object getContent()
    {
        return content;
    }

    /**
     * @return {@code true} if this line was added, {@code false} otherwise
     */
    public boolean isAdded()
    {
        return type == LineType.ADDED;
    }

    /**
     * @return {@code true} if this line was deleted, {@code false} otherwise
     */
    public boolean isDeleted()
    {
        return type == LineType.DELETED;
    }

    @Override
    public String toString()
    {
        return type.getSymbol() + String.valueOf(content) + '\n';
    }
}
