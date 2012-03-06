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
package org.xwiki.rendering.macro.script;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Dynamic mock setup for script macros.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class ScriptMockSetup
{
    public DocumentAccessBridge bridge;
    
    public AttachmentReferenceResolver<String> attachmentReferenceResolver;
    
    public DocumentReferenceResolver<String> documentReferenceResolver;

    public WikiModel wikiModel;

    /**
     * @since 2.4RC1
     */
    public ScriptMockSetup(ComponentManager componentManager) throws Exception
    {
        this(new JUnit4Mockery(), componentManager);
    }

    public ScriptMockSetup(Mockery mockery, ComponentManager componentManager) throws Exception
    {
        // Document Access Bridge Mock setup
        bridge = mockery.mock(DocumentAccessBridge.class);
        mockery.checking(new Expectations() {{
            allowing(bridge).hasProgrammingRights(); will(returnValue(true));
        }});

        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, bridge);

        // Register a WikiModel mock so that we're in wiki mode (otherwise links will be considered as URLs for ex).
        wikiModel = mockery.mock(WikiModel.class);
        DefaultComponentDescriptor<WikiModel> descriptorWM =
            new DefaultComponentDescriptor<WikiModel>();
        descriptorWM.setRole(WikiModel.class);
        componentManager.registerComponent(descriptorWM, wikiModel);

        // Use a mock for the AttachmentReference Resolver
        attachmentReferenceResolver = mockery.mock(AttachmentReferenceResolver.class);
        DefaultComponentDescriptor<AttachmentReferenceResolver<String>> descriptorARF =
            new DefaultComponentDescriptor<AttachmentReferenceResolver<String>>();
        descriptorARF.setRoleType(AttachmentReferenceResolver.TYPE_STRING);
        descriptorARF.setRoleHint("current");
        componentManager.registerComponent(descriptorARF, attachmentReferenceResolver);

        // Use a mock for the DocumentReference Resolver
        documentReferenceResolver = mockery.mock(DocumentReferenceResolver.class);
        DefaultComponentDescriptor<DocumentReferenceResolver<String>> descriptorDRF =
            new DefaultComponentDescriptor<DocumentReferenceResolver<String>>();
        descriptorDRF.setRoleType(DocumentReferenceResolver.TYPE_STRING);
        descriptorDRF.setRoleHint("current");
        componentManager.registerComponent(descriptorDRF, documentReferenceResolver);
    }
}
