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

package org.xwiki.annotation.internal.content;

import java.util.Map;

import org.xwiki.annotation.content.AlteredContent;

/**
 * Offsets maps based implementation of the {@link AlteredContent}.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class OffsetsMapAlteredContent implements AlteredContent
{
    /**
     * The actual character sequence representing the altered content.
     */
    private final CharSequence content;

    /**
     * The offsets map for translating initial offsets to altered offsets.
     */
    private final Map<Integer, Integer> initialToAltered;

    /**
     * The offsets map for translating the altered offsets to the initial offsets.
     */
    private final Map<Integer, Integer> alteredToInitial;

    /**
     * The initial size of the content.
     */
    private final int size;

    /**
     * Builds an altered content from the passed maps.
     * 
     * @param content actual character sequence representing the altered content
     * @param size initial size of the content
     * @param initialToAltered offsets map for translating initial offsets to altered offsets
     * @param alteredToInitial offsets map for translating the altered offsets to the initial offsets
     */
    public OffsetsMapAlteredContent(CharSequence content, int size, Map<Integer, Integer> initialToAltered,
        Map<Integer, Integer> alteredToInitial)
    {
        this.content = content;
        this.initialToAltered = initialToAltered;
        this.alteredToInitial = alteredToInitial;
        this.size = size;
    }

    @Override
    public CharSequence getContent()
    {
        return content;
    }

    @Override
    public int getInitialOffset(int i)
    {
        Integer result = alteredToInitial.get(i);
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    @Override
    public int getAlteredOffset(int i)
    {
        Integer result = initialToAltered.get(i);
        if (result == null) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    @Override
    public int getInitialLength()
    {
        return size;
    }
}
