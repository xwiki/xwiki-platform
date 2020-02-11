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

import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.internal.server.DefaultOfficeServer;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * An abstract test case to be used by all office-importer tests.
 *
 * @version $Id$
 * @since 2.1M1
 */
@AllComponents(excludes = {
    DefaultStringDocumentReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultOfficeServer.class
})
public abstract class AbstractOfficeImporterTest
{
    /**
     * Mock document access bridge.
     */
    @MockComponent
    protected DocumentAccessBridge mockDocumentAccessBridge;

    /**
     * Mock (default) string document reference serializer.
     */
    @MockComponent
    protected EntityReferenceSerializer<String> mockDefaultStringEntityReferenceSerializer;

    /**
     * Mock (compactwiki) string document reference serializer.
     */
    @MockComponent
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> mockCompactWikiStringEntityReferenceSerializer;

    /**
     * Mock document name factory.
     */
    @MockComponent
    @Named("currentmixed")
    protected DocumentReferenceResolver<String> mockDocumentReferenceResolver;

    /**
     * Mock office server.
     */
    @MockComponent
    protected OfficeServer mockOfficeServer;

    /**
     * Component Manager for the tests.
     */
    @InjectComponentManager
    protected MockitoComponentManager componentManager;
}
