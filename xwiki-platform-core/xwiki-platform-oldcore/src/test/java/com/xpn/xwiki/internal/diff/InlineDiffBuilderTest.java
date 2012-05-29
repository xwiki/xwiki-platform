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

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;

import com.xpn.xwiki.internal.diff.InlineDiffWord.WordType;

/**
 * Unit tests for {@link InlineDiffBuilder}.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class InlineDiffBuilderTest
{
    @Test
    public void testBothEmpty() throws DifferentiationFailedException
    {
        execute("", "", "");
    }

    @Test
    public void testNoChange() throws DifferentiationFailedException
    {
        execute("xwiki", "xwiki", "xwiki");
    }

    @Test
    public void testOriginalEmpty() throws DifferentiationFailedException
    {
        execute("", "xwiki", "+xwiki+");
    }

    @Test
    public void testRevisedEmpty() throws DifferentiationFailedException
    {
        execute("xwiki", "", "-xwiki-");
    }

    @Test
    public void testAddCharacter() throws DifferentiationFailedException
    {
        execute("xwki", "xwiki", "xw+i+ki");
    }

    @Test
    public void testRemoveCharacter() throws DifferentiationFailedException
    {
        execute("xwiki", "xwki", "xw-i-ki");
    }

    @Test
    public void testChangeCharacter() throws DifferentiationFailedException
    {
        execute("xwiki", "xwIki", "xw-i-+I+ki");
    }

    @Test
    public void testEndPoints() throws DifferentiationFailedException
    {
        execute("wiki", "xwik", "+x+wik-i-");
    }

    @Test
    public void testAddWord() throws DifferentiationFailedException
    {
        execute("123abc", "123XYZabc", "123+XYZ+abc");
    }

    @Test
    public void testRemoveWord() throws DifferentiationFailedException
    {
        execute("123xyzABC", "123ABC", "123-xyz-ABC");
    }

    @Test
    public void testChangeWord() throws DifferentiationFailedException
    {
        execute("123 xyz abc", "123 XYZ abc", "123 -xyz-+XYZ+ abc");
    }

    @Test
    public void testChangeWords() throws DifferentiationFailedException
    {
        execute("123 456 789", "abc 456 xyz", "-123-+abc+ 456 -789-+xyz+");
    }

    /**
     * Builds the in-line diff between the given versions and asserts if the result meets the expectation.
     * 
     * @param original the original version
     * @param revised the revised version
     * @param expected the expected in-line diff
     * @throws DifferentiationFailedException if creating the diff fails
     */
    private void execute(String original, String revised, String expected) throws DifferentiationFailedException
    {
        Character[] originalChars = ArrayUtils.toObject(original.toCharArray());
        Character[] revisedChars = ArrayUtils.toObject(revised.toCharArray());

        InlineDiffBuilder builder = new InlineDiffBuilder(originalChars);
        Diff.diff(originalChars, revisedChars).accept(builder);

        Map<WordType, String> separators = new HashMap<WordType, String>();
        separators.put(WordType.ADDED, "+");
        separators.put(WordType.DELETED, "-");
        separators.put(WordType.CONTEXT, "");

        StringBuilder actual = new StringBuilder();
        for (InlineDiffWord word : builder.getResult()) {
            String separator = separators.get(word.getType());
            actual.append(separator).append(word).append(separator);
        }

        Assert.assertEquals(expected, actual.toString());
    }
}
