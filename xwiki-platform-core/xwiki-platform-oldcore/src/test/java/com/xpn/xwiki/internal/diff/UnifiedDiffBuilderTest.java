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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;

/**
 * Unit tests for {@link UnifiedDiffBuilder}.
 * 
 * @version $Id$
 * @since 4.1M2
 */
@RunWith(Parameterized.class)
public class UnifiedDiffBuilderTest
{
    /**
     * The original version.
     */
    private final Object[] original;

    /**
     * The revised version.
     */
    private final Object[] revised;

    /**
     * The expected unified diff.
     */
    private final String expected;

    /**
     * Creates a new test with the given input and the specified expected output.
     * 
     * @param original the original version
     * @param revised the revised version
     * @param expected the expected unified diff
     */
    public UnifiedDiffBuilderTest(Object[] original, Object[] revised, String expected)
    {
        this.original = original;
        this.revised = revised;
        this.expected = expected;
    }

    /**
     * The actual test.
     */
    @Test
    public void execute() throws DifferentiationFailedException
    {
        UnifiedDiffBuilder builder = new UnifiedDiffBuilder(original, revised);
        Diff.diff(original, revised).accept(builder);
        StringBuilder actual = new StringBuilder();
        for (UnifiedDiffBlock block : builder.getResult()) {
            actual.append(block);
        }
        Assert.assertEquals(expected, actual.toString());
    }

    /**
     * @return the collection of test parameters
     */
    @Parameters
    public static Collection<Object[]> data() throws IOException
    {
        Collection<Object[]> data = new ArrayList<Object[]>();

        //
        // Add special tests.
        //

        // Both original and revised are empty.
        data.add(new Object[] {new String[0], new String[0], ""});

        // Original and revised are equal.
        String[] lines = new String[] {"one", "two", "three"};
        data.add(new Object[] {lines, lines, ""});

        // Original is empty.
        data.add(new Object[] {new String[0], lines, "@@ -1,0 +1,3 @@\n+one\n+two\n+three\n"});

        // Revised is empty.
        data.add(new Object[] {lines, new String[0], "@@ -1,3 +1,0 @@\n-one\n-two\n-three\n"});

        // Line removed.
        data.add(new Object[] {lines, new String[] {lines[0], lines[2]}, "@@ -1,3 +1,2 @@\n one\n-two\n three\n"});

        // Line added.
        data.add(new Object[] {lines, new String[] {lines[0], lines[1], "between", lines[2]},
        "@@ -1,3 +1,4 @@\n one\n two\n+between\n three\n"});

        // Line changed.
        data.add(new Object[] {lines, new String[] {lines[0], "Two", lines[2]},
        "@@ -1,3 +1,3 @@\n one\n-two\n+Two\n three\n"});

        //
        // Add tests from files.
        //

        Object[] original = readLines("original.txt");
        String[] testNames = new String[] {"twoContexts", "sharedContext"};
        for (String testName : testNames) {
            data.add(new Object[] {original, readLines(testName + ".txt"), readContent(testName + ".diff")});
        }

        return data;
    }

    /**
     * Reads the lines from the specified file.
     * 
     * @param fileName the file name
     * @return the lines from the specified file
     * @throws IOException if reading the file fails
     */
    private static Object[] readLines(String fileName) throws IOException
    {
        InputStream stream = UnifiedDiffBuilderTest.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return IOUtils.readLines(reader).toArray();
    }

    /**
     * Reads the content of the specified file.
     * 
     * @param fileName the file name
     * @return the content of the specified file
     * @throws IOException if reading the fail fails
     */
    private static String readContent(String fileName) throws IOException
    {
        InputStream stream = UnifiedDiffBuilderTest.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return IOUtils.toString(reader);
    }
}
