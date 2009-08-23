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

import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.test.AbstractComponentTestCase;
import org.jmock.Mockery;

/**
 * Abstract class for all HTML cleaner tests.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class AbstractHTMLCleaningTest extends AbstractComponentTestCase
{
    /**
     * Beginning of the test html document.
     */
    protected String header = "<html><head><title>Title</title></head><body>";

    /**
     * Ending of the test html document..
     */
    protected String footer = "</body></html>";

    /**
     * {@link OpenOfficeHTMLCleaner} used for tests.
     */
    protected HTMLCleaner openOfficeHTMLCleaner;

    /**
     * {@link WysiwygHTMLCleaner} used for tests.
     */
    protected HTMLCleaner wysiwygHTMLCleaner;

    protected Mockery context = new Mockery();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        
        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge = context.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        getComponentManager().registerComponent(descriptorDAB, mockDocumentAccessBridge);

        this.openOfficeHTMLCleaner = getComponentManager().lookup(HTMLCleaner.class, "openoffice");
        this.wysiwygHTMLCleaner = getComponentManager().lookup(HTMLCleaner.class, "wysiwyg");
    }
}
