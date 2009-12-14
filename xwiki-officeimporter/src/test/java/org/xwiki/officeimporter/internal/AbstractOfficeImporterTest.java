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
package org.xwiki.officeimporter.internal;

import org.jmock.Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.DocumentNameFactory;
import org.xwiki.model.DocumentNameSerializer;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * An abstract test case to be used by all officeimporter tests.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public abstract class AbstractOfficeImporterTest extends AbstractComponentTestCase
{
    /**
     * Mockery for creating mock objects.
     */
    protected Mockery context = new Mockery();

    /**
     * Mock document access bridge.
     */
    protected DocumentAccessBridge mockDocumentAccessBridge;
    
    /**
     * Mock document name serializer.
     */
    protected DocumentNameSerializer mockDocumentNameSerializer;
    
    /**
     * Mock document name factory.
     */
    protected DocumentNameFactory mockDocumentNameFactory;

    /**
     * {@inheritDoc}
     */
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Document Access Bridge Mock
        mockDocumentAccessBridge = context.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        getComponentManager().registerComponent(descriptorDAB, mockDocumentAccessBridge);

        // Document name serializer.
        mockDocumentNameSerializer = this.context.mock(DocumentNameSerializer.class);
        DefaultComponentDescriptor<DocumentNameSerializer> descriptorDNS =
            new DefaultComponentDescriptor<DocumentNameSerializer>();
        descriptorDNS.setRole(DocumentNameSerializer.class);
        getComponentManager().registerComponent(descriptorDNS, mockDocumentNameSerializer);

        // Document name factory.
        mockDocumentNameFactory = this.context.mock(DocumentNameFactory.class);
        DefaultComponentDescriptor<DocumentNameFactory> descriptorDNF =
            new DefaultComponentDescriptor<DocumentNameFactory>();
        descriptorDNF.setRole(DocumentNameFactory.class);
        getComponentManager().registerComponent(descriptorDNF, mockDocumentNameFactory);
    }
}
