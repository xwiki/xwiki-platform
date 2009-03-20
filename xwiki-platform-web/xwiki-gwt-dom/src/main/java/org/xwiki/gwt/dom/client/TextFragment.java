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
package org.xwiki.gwt.dom.client;

/**
 * A text fragment is a substring of a text node from a DOM tree. It is uniquely identified by the source text node, the
 * index of the first character and the index of the last character.
 * 
 * @version $Id$
 */
public class TextFragment
{
    /**
     * The source text node.
     */
    private final Text text;

    /**
     * The index of fragment's first character, in the source text node.
     */
    private final int startIndex;

    /**
     * The index of fragment's last character, in the source text node.
     */
    private final int endIndex;

    /**
     * Creates a new fragment of the given text node.
     * 
     * @param text the source text node.
     * @param startIndex the index of fragment's first character, in the source text node.
     * @param endIndex the index of fragment's last character, in the source text node.
     */
    public TextFragment(Text text, int startIndex, int endIndex)
    {
        this.text = text;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    /**
     * @return the source text node.
     */
    public Text getText()
    {
        return text;
    }

    /**
     * @return the index of fragment's first character, in the source text node.
     */
    public int getStartIndex()
    {
        return startIndex;
    }

    /**
     * @return the index of fragment's last character, in the source text node.
     */
    public int getEndIndex()
    {
        return endIndex;
    }
}
