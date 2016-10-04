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
package org.xwiki.officeimporter.internal.filter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * Base class for HTML filter tests.
 * 
 * @version $Id$
 * @since 7.4.6
 * @since 8.2.2
 * @since 8.3
 */
public abstract class AbstractHTMLFilterTest
{
    protected HTMLFilter filter;

    /**
     * Helper object for manipulating DOM Level 3 Load and Save APIs.
     */
    private DOMImplementationLS lsImpl;

    @Before
    public void configure() throws Exception
    {
        this.lsImpl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
    }

    protected Document filter(String content)
    {
        return filter(content, Collections.<String, String>emptyMap());
    }

    protected Document filter(String content, Map<String, String> cleaningParameters)
    {
        LSInput input = this.lsImpl.createLSInput();
        String actualContent = "<root>" + content + "</root>";
        input.setByteStream(new ByteArrayInputStream(actualContent.getBytes()));
        Document document = XMLUtils.parse(input);
        this.filter.filter(document, cleaningParameters);
        return document;
    }

    protected Document filterAndAssertOutput(String input, String expectedOuput)
    {
        return filterAndAssertOutput(input, Collections.<String, String>emptyMap(), expectedOuput);
    }

    protected Document filterAndAssertOutput(String input, Map<String, String> cleaningParameters, String expectedOuput)
    {
        Document document = filter(input, cleaningParameters);
        String output = XMLUtils.serialize(document, false);
        output = StringUtils.removeStart(output, "<root>");
        output = StringUtils.removeEnd(output, "</root>");
        Assert.assertEquals(expectedOuput, output);
        return document;
    }
}
