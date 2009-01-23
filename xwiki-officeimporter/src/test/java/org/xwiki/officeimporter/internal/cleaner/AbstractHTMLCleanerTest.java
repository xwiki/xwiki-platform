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
package org.xwiki.officeimporter.internal.cleaner;

import org.xwiki.officeimporter.internal.MockDocumentAccessBridge;

import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;


/**
 * Abstract class for all html cleaner tests.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class AbstractHTMLCleanerTest extends AbstractXWikiComponentTestCase
{
    /**
     * Beginning of the test html document.
     */
    protected String header = "<html><head><title>Title</title></head><body>";

    /**
     * Beginning of the test html document, which has a {@code <style> tag.}
     */
    protected String headerWithStyles =
        "<html><head><style type=\"text/css\">h1 {color:red} p {color:blue} </style><title>Title</title></head><body>";

    /**
     * Ending of the test html document..
     */
    protected String footer = "</body></html>";

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponentDescriptor(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
    }
}
