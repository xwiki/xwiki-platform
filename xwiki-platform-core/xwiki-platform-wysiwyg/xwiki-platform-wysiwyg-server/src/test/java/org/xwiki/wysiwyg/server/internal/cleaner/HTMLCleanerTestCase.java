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
package org.xwiki.wysiwyg.server.internal.cleaner;

import junit.framework.TestCase;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;

/**
 * Generic test case for {@link HTMLCleaner}.
 * 
 * @version $Id$
 */
public class HTMLCleanerTestCase extends TestCase
{
    /**
     * The component manager used to get an {@link HTMLCleaner} instance.
     */
    private ComponentManager componentManager;

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
     * @param testName the test name
     * @param input the HTML fragment to be cleaned
     * @param expected the expected clean HTML
     */
    public HTMLCleanerTestCase(String testName, String input, String expected)
    {
        super(testName);

        this.input = input;
        this.expected = expected;
    }

    @Override
    protected void runTest() throws Exception
    {
        HTMLCleaner cleaner = getComponentManager().getInstance(HTMLCleaner.class);
        assertEquals(xhtmlFragment(expected), cleaner.clean(input));
    }

    /**
     * Adds the XHTML envelope to the given XHTML fragment.
     * 
     * @param fragment the content to be placed inside the {@code body} tag
     * @return the given XHTML fragment wrapped in the XHTML envelope
     */
    protected String xhtmlFragment(String fragment)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" + "<html><head></head><body>" + fragment
            + "</body></html>\n";
    }

    /**
     * Sets the component manager.
     * 
     * @param componentManager the component manager to use for getting an {@link HTMLCleaner} instance
     */
    public void setComponentManager(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * @return the component manager used to get an instance of {@link HTMLCleaner}
     */
    public ComponentManager getComponentManager()
    {
        return componentManager;
    }
}
