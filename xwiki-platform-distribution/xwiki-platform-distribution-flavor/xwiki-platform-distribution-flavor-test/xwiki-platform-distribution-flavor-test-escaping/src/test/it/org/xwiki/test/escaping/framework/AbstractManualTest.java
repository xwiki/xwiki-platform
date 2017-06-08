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
package org.xwiki.test.escaping.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;

/**
 * Abstract base class for manual tests. Provides several helper methods and default implementation for less likely
 * needed superclass methods.
 * <p>
 * The test methods should call {@link #skipIfIgnored(String)} at the beginning to allow skipping the tests using
 * -Dpattern=regex maven command line option.
 * </p>
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractManualTest extends AbstractEscapingTest
{
    /**
     * List of URLs that should be deleted in {@link #tearDown()}.
     * 
     * @see #createPage(String, String, String, String)
     */
    private List<String> toDeleteURLs = new LinkedList<String>();

    /**
     * Create new {@link AbstractManualTest}. JUnit needs a default constructor, we provide a default implementation
     * that accepts all file names.
     */
    public AbstractManualTest()
    {
        super(Pattern.compile(".*"));
    }

    /**
     * {@inheritDoc} Parsing should not be needed in manual tests, we return an empty set.
     * 
     * @see AbstractEscapingTest#parse(java.io.Reader)
     */
    @Override
    protected Set<String> parse(Reader reader)
    {
        Set<String> set = new HashSet<String>();
        // the set cannot be completely empty, otherwise all tests will be ignored
        set.add("unused");
        return set;
    }

    /**
     * {@inheritDoc} The default implementation for manual tests returns false.
     * 
     * @param fileName file name to check
     * @return true if the file should be excluded, false otherwise
     * @see AbstractAutomaticTest#isExcludedFile(String)
     * @see AbstractEscapingTest#isExcludedFile(java.lang.String)
     */
    @Override
    protected boolean isExcludedFile(String fileName)
    {
        return false;
    }

    /**
     * Skip the test if the file name does not match ignore pattern (-Dpattern=regex on command line). Should be called
     * at the beginning of all tests.
     * 
     * @param testedFileName tested file name
     */
    protected void skipIfIgnored(String testedFileName)
    {
        Assume.assumeTrue(initialize(testedFileName, null));
    }

    /**
     * Check that there is no error trace on the given URL. TODO do not download the same URL twice (usually
     * {@link #checkUnderEscaping(String, String)} is also used)
     * 
     * @param url the URL to download
     * @throws IOException on connection errors
     */
    protected void checkForErrorTrace(String url) throws IOException
    {
        BufferedReader reader = new BufferedReader(AbstractEscapingTest.getUrlContent(url).getContentReader());
        String line;
        while ((line = reader.readLine()) != null) {
            Assert.assertFalse("The page contains a error trace", line.matches("^.*<pre\\s+class=\"xwikierror\">.*$"));
        }
    }

    /**
     * Create a page with the given data. This page is automatically deleted in {@link #tearDown()}.
     * 
     * @param space space name
     * @param page page name
     * @param title document title
     * @param content document content
     */
    protected void createPage(String space, String page, String title, String content)
    {
        // create
        String url = createUrl("save", space, page,
            params(kv("title", title), kv("content", content), kv("action_save", "Save+%26+View")));
        AbstractEscapingTest.getUrlContent(url);
        // schedule for deletion
        deleteAfterwards(space, page);
    }

    /**
     * Schedule a page for deletion in {@link #tearDown()}.
     * 
     * @param space space name
     * @param page page name
     */
    protected void deleteAfterwards(String space, String page)
    {
        this.toDeleteURLs.add(createUrl("delete", space, page, params(kv("confirm", "1"))));
    }

    /**
     * Create a parameter map from the given list of key-value pairs. Does NOT URL-escapes values (
     * {@link #createUrl(String, String, String, Map)} does this).
     * 
     * @param keysAndValues list of key-value pairs
     * @return parameter map
     * @see AbstractManualTest#kv(String, String)
     * @see AbstractManualTest#test(String)
     */
    protected static Map<String, String> params(String[]... keysAndValues)
    {
        Map<String, String> params = new HashMap<String, String>();
        for (String[] kv : keysAndValues) {
            if (kv != null && kv.length > 0 && kv[0] != null) {
                String key = kv[0];
                String value = "";
                if (kv.length > 1 && kv[1] != null) {
                    value = kv[1];
                }
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Create one key-value pair with the given key name and value.
     * 
     * @param key key name
     * @param value unescaped value
     * @return a key-value pair
     * @see AbstractManualTest#params(String[][])
     * @see AbstractManualTest#test(String)
     */
    protected static String[] kv(String key, String value)
    {
        return new String[] {key, value};
    }

    /**
     * Create one key-value pair with the given key name and value set to the test string.
     * 
     * @param key key name
     * @return a key-value-pair
     * @see AbstractManualTest#params(String[][])
     * @see AbstractManualTest#kv(String, String)
     */
    protected static String[] test(String key)
    {
        return kv(key, XMLEscapingValidator.getTestString());
    }

    /**
     * Create one key-value pair that would add a parameter redirecting to the given template name.
     * 
     * @param templateName key name
     * @return a key-value-pair
     * @see AbstractManualTest#params(String[][])
     * @see AbstractManualTest#kv(String, String)
     */
    protected static String[] template(String templateName)
    {
        return kv("xpage", templateName);
    }

    /**
     * Clean up.
     */
    @After
    public void tearDown()
    {
        // delete all created pages
        for (String url : this.toDeleteURLs) {
            AbstractEscapingTest.getUrlContent(url);
        }
    }
}
