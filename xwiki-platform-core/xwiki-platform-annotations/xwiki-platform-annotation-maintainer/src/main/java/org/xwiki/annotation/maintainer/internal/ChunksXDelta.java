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
package org.xwiki.annotation.maintainer.internal;

/**
 * String chunks storage based implementation of the difference interface. It stores the original text and the updated
 * one and returns them when functions are called.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class ChunksXDelta extends AbstractXDelta
{
    /**
     * The position where the modification takes place.
     */
    private int position;

    /**
     * The original chunk at position.
     */
    private String original;

    /**
     * The modified chunk at position.
     */
    private String edited;

    /**
     * Creates a new XDelta from the passed values.
     * 
     * @param position the position where the edit takes place
     * @param original the original string at {@code position}
     * @param edited the new string at {@code position}
     */
    public ChunksXDelta(int position, String original, String edited)
    {
        super();
        this.position = position;
        this.original = original;
        this.edited = edited;
    }

    @Override
    public String getChanged()
    {
        return edited;
    }

    @Override
    public String getOriginal()
    {
        return original;
    }

    @Override
    public int getOffset()
    {
        return position;
    }

}
