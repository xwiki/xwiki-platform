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
package org.xwiki.wysiwyg.internal.cleaner;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;

/**
 * A generic JUnit test used by {@link HTMLCleanerTestSuite} to clean some passed HTML content and verify it matches
 * some passed expectation.
 * 
 * @version $Id$
 */
public class HTMLCleanerTest
{
    /**
     * The HTML cleaner being tested.
     */
    private HTMLCleaner cleaner;

    /**
     * The HTML fragment to be cleaned.
     */
    private final String input;

    /**
     * The expected clean HTML.
     */
    private final String expected;

    /**
     * Creates a new test case that checks if the result of cleaning the given HTML input equals the expected HTML.
     * 
     * @param input the HTML fragment to be cleaned
     * @param expected the expected clean HTML
     * @param cleaner the HTML cleaner being tested
     */
    public HTMLCleanerTest(String input, String expected, HTMLCleaner cleaner)
    {
        this.input = input;
        this.expected = expected;
        this.cleaner = cleaner;
    }

    /**
     * The actual test.
     */
    @Test
    public void execute()
    {
        Assert.assertEquals(xhtmlFragment(expected), cleaner.clean(input));
    }

    /**
     * Adds the XHTML envelope to the given XHTML fragment.
     * 
     * @param fragment the content to be placed inside the {@code body} tag
     * @return the given XHTML fragment wrapped in the XHTML envelope
     */
    private String xhtmlFragment(String fragment)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html>\n" + "<html><head></head><body>" + fragment
            + "</body></html>\n";
    }
}
