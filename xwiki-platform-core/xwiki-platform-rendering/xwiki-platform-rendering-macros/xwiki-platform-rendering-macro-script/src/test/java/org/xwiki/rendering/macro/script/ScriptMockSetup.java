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
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.jmock.MockingComponentManager;

/**
 * Dynamic mock setup for script macros.
 *
 * @version $Id$
 * @since 2.0RC1
 */
public class ScriptMockSetup
{
    public ContextualAuthorizationManager authorizationManager;

    public AttachmentReferenceResolver<String> attachmentReferenceResolver;

    public DocumentReferenceResolver<String> documentReferenceResolver;

    public SpaceReferenceResolver<String> spaceReferenceResolver;

    public WikiModel wikiModel;

    public ScriptMockSetup(MockingComponentManager componentManager) throws Exception
    {
        this(new JUnit4Mockery(), componentManager);
    }

    public ScriptMockSetup(Mockery mockery, MockingComponentManager cm) throws Exception
    {
        // Document Access Bridge Mock setup
        // This mock is required as it's injected in component, but does not need specific mocking.
        cm.registerMockComponent(mockery, DocumentAccessBridge.class);
        // Contextual Authorization Manager Mock setup
        this.authorizationManager = cm.registerMockComponent(mockery, ContextualAuthorizationManager.class);
        mockery.checking(new Expectations() {{
            allowing(authorizationManager).hasAccess(Right.SCRIPT); will(returnValue(true));
            allowing(authorizationManager).hasAccess(Right.PROGRAM); will(returnValue(true));
        }});

        // Register a WikiModel mock so that we're in wiki mode (otherwise links will be considered as URLs for ex).
        this.wikiModel = cm.registerMockComponent(mockery, WikiModel.class);

        // Use a mock for the AttachmentReference Resolver
        this.attachmentReferenceResolver =
            cm.registerMockComponent(mockery, AttachmentReferenceResolver.TYPE_STRING, "current");

        // Use a mock for the DocumentReference Resolver
        this.documentReferenceResolver =
            cm.registerMockComponent(mockery, DocumentReferenceResolver.TYPE_STRING, "current");

        // Use a mock for the SpaceReference Resolver
        this.spaceReferenceResolver = cm.registerMockComponent(mockery, SpaceReferenceResolver.TYPE_STRING, "current");
    }
}
