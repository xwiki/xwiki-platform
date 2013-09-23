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
package org.xwiki.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for all DOM tests. It returns the name of the module in {@link #getModuleName()} so you don't have to do
 * it in each test.
 * 
 * @version $Id$
 */
public class DOMTestCase extends GWTTestCase
{
    /**
     * Greater-than sign.
     */
    private static final String GT = ">";

    /**
     * The document in which we run the tests.
     */
    private Document document;

    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        document = Document.get().cast();
        container = document.createDivElement().cast();
        document.getBody().appendChild(container);
    }

    @Override
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        container.getParentNode().removeChild(container);
    }

    /**
     * @return the document in which we run the tests
     */
    protected Document getDocument()
    {
        return document;
    }

    /**
     * @return the DOM element in which we run the tests
     */
    protected Element getContainer()
    {
        return container;
    }

    @Override
    public String getModuleName()
    {
        return "org.xwiki.gwt.dom.DOMTest";
    }

    /**
     * Normalizes the given HTML fragment so that it can be asserted on different browsers.
     * 
     * @param html the HTML fragment to be normalized
     * @return the result of normalizing the given HTML fragment
     */
    protected String normalizeHTML(String html)
    {
        return html.trim().toLowerCase().replace("></br>", GT).replace("></img>", GT);
    }
}
