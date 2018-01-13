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
package org.xwiki.refactoring.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Common code for all refactoring unit tests.
 *  
 * @version $Id$
 * @since 2.0.1
 */
public class AbstractRefactoringTestCase extends AbstractComponentTestCase
{
    protected Mockery mockery = new Mockery();
    
    /**
     * The {@link DocumentAccessBridge} component.
     */
    protected DocumentAccessBridge docBridge;

    /**
     * The {@link Parser} component.
     */
    protected Parser xwikiParser;

    @Override
    protected void registerComponents() throws Exception
    {
        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge = this.mockery.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRoleType(DocumentAccessBridge.class);
        getComponentManager().registerComponent(descriptorDAB, mockDocumentAccessBridge);
        
        mockery.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).exists(with(any(String.class))); will(returnValue(false));
        }});

        this.docBridge = getComponentManager().getInstance(DocumentAccessBridge.class, "default");
        this.xwikiParser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");
    }
}
