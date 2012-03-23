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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
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
     * Mock document access bridge.
     */
    protected DocumentAccessBridge mockDocumentAccessBridge;

    /**
     * Mock (default) string document reference serializer.
     */
    protected EntityReferenceSerializer<String> mockDefaultStringEntityReferenceSerializer;

    /**
     * Mock (compactwiki) string document reference serializer.
     */
    protected EntityReferenceSerializer<String> mockCompactWikiStringEntityReferenceSerializer;

    /**
     * Mock document name factory.
     */
    protected DocumentReferenceResolver<String> mockDocumentReferenceResolver;

    /**
     * Mock {@link OpenOfficeManager} component.
     */
    protected OpenOfficeManager mockOpenOfficeManager;

    @Override
    @SuppressWarnings("unchecked")
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Document Access Bridge Mock
        this.mockDocumentAccessBridge = registerMockComponent(DocumentAccessBridge.class);

        // Mock (default) string document name serializer.
        this.mockDefaultStringEntityReferenceSerializer = getMockery().mock(EntityReferenceSerializer.class, "s1");
        DefaultComponentDescriptor<EntityReferenceSerializer> descriptorDSRS =
            new DefaultComponentDescriptor<EntityReferenceSerializer>();
        descriptorDSRS.setRoleType(EntityReferenceSerializer.TYPE_STRING);
        getComponentManager().registerComponent(descriptorDSRS, mockDefaultStringEntityReferenceSerializer);

        // Mock (compactwiki) string document name serializer.
        mockCompactWikiStringEntityReferenceSerializer = getMockery().mock(EntityReferenceSerializer.class, "s2");
        DefaultComponentDescriptor<EntityReferenceSerializer> descriptorCWSRS =
            new DefaultComponentDescriptor<EntityReferenceSerializer>();
        descriptorCWSRS.setRoleType(EntityReferenceSerializer.TYPE_STRING);
        descriptorCWSRS.setRoleHint("compactwiki");
        getComponentManager().registerComponent(descriptorCWSRS, mockCompactWikiStringEntityReferenceSerializer);

        // Document name factory.
        this.mockDocumentReferenceResolver = registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        // Mock OpenOffice manager.
        this.mockOpenOfficeManager = registerMockComponent(OpenOfficeManager.class);
    }
}
