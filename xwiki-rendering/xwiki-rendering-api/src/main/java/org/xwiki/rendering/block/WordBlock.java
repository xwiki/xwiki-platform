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
package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

/**
 * Represents a word.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class WordBlock extends AbstractBlock
{
    /**
     * @see #getWord()
     */
    private String word;

    /**
     * @param word the word wrapped by this block. Note that this is supposed to be a single word
     *        and space or special symbols should be represented by other blocks
     */
    public WordBlock(String word)
    {
        this.word = word;
    }

    /**
     * {@inheritDoc}
     * @see AbstractBlock#traverse(Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onWord(getWord());
    }

    /**
     * @return the word wrapped by this block
     */
    public String getWord()
    {
        return this.word;
    }
}
