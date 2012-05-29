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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.myers.MyersDiff;

import com.xpn.xwiki.internal.diff.InlineDiffWord.WordType;

/**
 * Unit tests for {@link ExtendedDiffBuilder}.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class ExtendedDiffBuilderTest
{
    @Test
    public void testLineAdded() throws DifferentiationFailedException
    {
        String[] original = new String[] {"one", "three"};
        String[] revised = new String[] {original[0], "two", original[1]};
        execute(original, revised, "@@ -1,2 +1,3 @@\n one\n+two\n three\n");
    }

    @Test
    public void testLineRemoved() throws DifferentiationFailedException
    {
        String[] original = new String[] {"one", "two", "three"};
        String[] revised = new String[] {original[0], original[2]};
        execute(original, revised, "@@ -1,3 +1,2 @@\n one\n-two\n three\n");
    }

    @Test
    public void testLineChanged() throws DifferentiationFailedException
    {
        String[] original = new String[] {"one", "two", "three"};
        String[] revised = new String[] {original[0], "tWo", original[2]};
        execute(original, revised, "@@ -1,3 +1,3 @@\n one\n-t-w-o\n+t+W+o\n three\n");
    }

    @Test
    public void testLineReplaced() throws DifferentiationFailedException
    {
        String[] original = new String[] {"one", "two", "three"};
        String[] revised = new String[] {original[0], "tWo", "extra", original[2]};
        execute(original, revised, "@@ -1,3 +1,4 @@\n one\n-two\n+tWo\n+extra\n three\n");
    }

    private void execute(String[] original, String[] revised, String expected) throws DifferentiationFailedException
    {
        Map<WordType, String> separators = new HashMap<WordType, String>();
        separators.put(WordType.ADDED, "+");
        separators.put(WordType.DELETED, "-");
        separators.put(WordType.CONTEXT, "");

        ExtendedDiffBuilder builder = new ExtendedDiffBuilder(original, revised, new MyersDiff());
        Diff.diff(original, revised).accept(builder);
        StringBuilder actual = new StringBuilder();
        for (UnifiedDiffBlock block : builder.getResult()) {
            actual.append(String.format("@@ -%s,%s +%s,%s @@\n", block.getOriginalStart() + 1, block.getOriginalSize(),
                block.getRevisedStart() + 1, block.getRevisedSize()));
            for (UnifiedDiffLine line : block) {
                if (line instanceof ExtendedDiffLine) {
                    actual.append(line.getType().getSymbol());
                    for (InlineDiffWord word : ((ExtendedDiffLine) line).getWords()) {
                        String separator = separators.get(word.getType());
                        actual.append(separator).append(word).append(separator);
                    }
                    actual.append('\n');
                } else {
                    actual.append(line);
                }
            }
        }
        Assert.assertEquals(expected, actual.toString());
    }
}
