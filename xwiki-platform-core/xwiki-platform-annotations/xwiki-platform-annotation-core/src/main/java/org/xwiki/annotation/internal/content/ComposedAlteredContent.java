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

import org.xwiki.annotation.content.AlteredContent;


/**
 * {@link AlteredContent} implementation built as the composition of two content alterers. {@code initial} is the
 * original altered content, whose {@link AlteredContent#getContent()} was altered and the {@code altered} altered
 * content was obtained.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class ComposedAlteredContent implements AlteredContent
{
    /**
     * The original altered content.
     */
    private AlteredContent initial;

    /**
     * The altered content representing the alteration of {@link #initial}'s content.
     */
    private AlteredContent altered;

    /**
     * Builds a composed content alterer for the original altered content, {@code initial} and its content's alteration,
     * {@code altered}.
     * 
     * @param initial the original altered content
     * @param altered the altering of the initial altered content
     */
    public ComposedAlteredContent(AlteredContent initial, AlteredContent altered)
    {
        this.initial = initial;
        this.altered = altered;
    }

    @Override
    public int getInitialOffset(int i)
    {
        int tmp = altered.getInitialOffset(i);
        int rez = initial.getInitialOffset(tmp);
        return rez;
    }

    @Override
    public int getInitialLength()
    {
        return initial.getInitialLength();
    }

    @Override
    public CharSequence getContent()
    {
        return altered.getContent();
    }

    @Override
    public int getAlteredOffset(int i)
    {
        int tmp = initial.getAlteredOffset(i);
        int rez = altered.getAlteredOffset(tmp);
        return rez;
    }
}
