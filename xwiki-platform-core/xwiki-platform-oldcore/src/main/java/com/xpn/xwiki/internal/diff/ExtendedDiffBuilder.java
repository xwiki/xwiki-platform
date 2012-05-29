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
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.suigeneris.jrcs.diff.DiffAlgorithm;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;

import com.xpn.xwiki.internal.diff.InlineDiffWord.WordType;

/**
 * Extends the {@link UnifiedDiffBuilder} with the ability to provide character-level changes when a line is modified.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class ExtendedDiffBuilder extends UnifiedDiffBuilder
{
    /**
     * The diff algorithm used to determine character-level changes inside a modified line.
     */
    private final DiffAlgorithm diffAlgorithm;

    /**
     * Creates a new instance.
     * 
     * @param original the original version
     * @param revised the revised version
     * @param diffAlgorithm the diff algorithm to be used to determine character-level changes inside a modified line
     */
    public ExtendedDiffBuilder(String[] original, String[] revised, DiffAlgorithm diffAlgorithm)
    {
        super(original, revised);
        this.diffAlgorithm = diffAlgorithm;
    }

    @Override
    public void visit(ChangeDelta delta)
    {
        super.visit(delta);

        // A line is modified when it is replaced by a single line.
        if (delta.getOriginal().first() == delta.getOriginal().last()
            && delta.getRevised().first() == delta.getRevised().last()) {
            UnifiedDiffBlock lastBlock = blocks.peek();
            UnifiedDiffLine original = lastBlock.get(lastBlock.size() - 2);
            UnifiedDiffLine revised = lastBlock.get(lastBlock.size() - 1);
            UnifiedDiffLine[] extendedLines = buildInlineDiff(original, revised);
            lastBlock.set(lastBlock.size() - 2, extendedLines[0]);
            lastBlock.set(lastBlock.size() - 1, extendedLines[1]);
        }
    }

    /**
     * Builds the in-line diff between two versions of a line.
     * 
     * @param original the original version
     * @param revised the revised version
     * @return the given lines extended with information about character-level changes
     */
    private UnifiedDiffLine[] buildInlineDiff(UnifiedDiffLine original, UnifiedDiffLine revised)
    {
        Character[] originalChars = ArrayUtils.toObject(String.valueOf(original.getContent()).toCharArray());
        Character[] revisedChars = ArrayUtils.toObject(String.valueOf(revised.getContent()).toCharArray());
        InlineDiffBuilder builder = new InlineDiffBuilder(originalChars);
        try {
            diffAlgorithm.diff(originalChars, revisedChars).accept(builder);
            List<InlineDiffWord> words = builder.getResult();
            List<InlineDiffWord> originalWords = new ArrayList<InlineDiffWord>();
            List<InlineDiffWord> revisedWords = new ArrayList<InlineDiffWord>();
            for (InlineDiffWord word : words) {
                if (word.getType() != WordType.ADDED) {
                    originalWords.add(word);
                }
                if (word.getType() != WordType.DELETED) {
                    revisedWords.add(word);
                }
            }
            return new ExtendedDiffLine[] {new ExtendedDiffLine(original, originalWords),
                new ExtendedDiffLine(revised, revisedWords)};
        } catch (DifferentiationFailedException e) {
            return new UnifiedDiffLine[] {original, revised};
        }
    }
}
