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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.RevisionVisitor;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;
import org.suigeneris.jrcs.diff.delta.DeleteDelta;
import org.suigeneris.jrcs.diff.delta.Delta;

import com.xpn.xwiki.internal.diff.InlineDiffWord.WordType;

/**
 * Builds an in-line diff by visiting a {@link Revision} and its {@link Delta}s.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class InlineDiffBuilder implements RevisionVisitor
{
    /**
     * The last change processed by this builder.
     */
    private Delta lastDelta;

    /**
     * The original version.
     */
    private final Object[] original;

    /**
     * The collection of in-line diff words.
     */
    private final List<InlineDiffWord> words = new ArrayList<InlineDiffWord>();

    /**
     * Creates a new instance.
     * 
     * @param original the original version
     */
    public InlineDiffBuilder(Object[] original)
    {
        this.original = original;
    }

    @Override
    public void visit(Revision revision)
    {
        // Do nothing.
    }

    @Override
    public void visit(DeleteDelta delta)
    {
        maybeAddContextWordBefore(delta);
        words.add(new InlineDiffWord(WordType.DELETED, delta.getOriginal().chunk().toArray()));
    }

    @Override
    public void visit(ChangeDelta delta)
    {
        maybeAddContextWordBefore(delta);
        words.add(new InlineDiffWord(WordType.DELETED, delta.getOriginal().chunk().toArray()));
        words.add(new InlineDiffWord(WordType.ADDED, delta.getRevised().chunk().toArray()));
    }

    @Override
    public void visit(AddDelta delta)
    {
        maybeAddContextWordBefore(delta);
        words.add(new InlineDiffWord(WordType.ADDED, delta.getRevised().chunk().toArray()));
    }

    /**
     * @return the in-line diff
     */
    public List<InlineDiffWord> getResult()
    {
        // Add the final context word.
        maybeAddContextWordBefore(original.length);

        return words;
    }

    /**
     * Adds a context word between two changes if they are not consecutive.
     * 
     * @param delta the change
     */
    private void maybeAddContextWordBefore(Delta delta)
    {
        maybeAddContextWordBefore(delta.getOriginal().first());
        lastDelta = delta;
    }

    /**
     * Adds a context word if its length is greater than zero.
     * 
     * @param charIndex the character index where the context word ends
     */
    private void maybeAddContextWordBefore(int charIndex)
    {
        int start = lastDelta == null ? 0 : lastDelta.getOriginal().last() + 1;
        if (start < charIndex) {
            words.add(new InlineDiffWord(WordType.CONTEXT, Arrays.copyOfRange(original, start, charIndex)));
        }
    }
}
