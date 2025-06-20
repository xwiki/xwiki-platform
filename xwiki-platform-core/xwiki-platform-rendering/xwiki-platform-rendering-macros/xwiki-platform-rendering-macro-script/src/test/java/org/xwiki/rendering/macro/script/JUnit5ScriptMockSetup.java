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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;

/**
 * Dynamic mock setup for script macros for JUnit 5 tests.
 *
 * @version $Id$
 * @since 17.0.0
 */
public class JUnit5ScriptMockSetup
{
    public DocumentAccessBridge bridge;

    public ContextualAuthorizationManager authorizationManager;

    public AttachmentReferenceResolver<String> attachmentReferenceResolver;

    public DocumentReferenceResolver<String> documentReferenceResolver;

    public SpaceReferenceResolver<String> spaceReferenceResolver;

    public WikiModel wikiModel;

    public JUnit5ScriptMockSetup(MockitoComponentManager cm) throws Exception
    {
        // Document Access Bridge Mock setup
        this.bridge = cm.registerMockComponent(DocumentAccessBridge.class);
        when(this.bridge.hasProgrammingRights()).thenReturn(true);

        // Contextual Authorization Manager Mock setup
        this.authorizationManager = cm.registerMockComponent(ContextualAuthorizationManager.class);
        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        // Register a WikiModel mock so that we're in wiki mode (otherwise links will be considered as URLs for ex).
        this.wikiModel = cm.registerMockComponent(WikiModel.class);

        // Use a mock for the AttachmentReference Resolver
        this.attachmentReferenceResolver = cm.registerMockComponent(AttachmentReferenceResolver.TYPE_STRING, "current");

        // Use a mock for the DocumentReference Resolver
        this.documentReferenceResolver = cm.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");

        // Use a mock for the SpaceReference Resolver
        this.spaceReferenceResolver = cm.registerMockComponent(SpaceReferenceResolver.TYPE_STRING, "current");
    }
}
