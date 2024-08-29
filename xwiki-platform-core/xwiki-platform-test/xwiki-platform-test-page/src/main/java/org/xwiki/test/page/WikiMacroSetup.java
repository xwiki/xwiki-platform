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
package org.xwiki.test.page;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.when;

/**
 * Set up a wiki macro for the page test.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public final class WikiMacroSetup
{
    private WikiMacroSetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Load a given wiki macro for a page test. The page test must be annotated with
     * {@code org.xwiki.rendering.wikimacro.internal.WikiMacroFactoryComponentClass}.
     *
     * @param pageTest the page test where the wiki macro is loaded
     * @param componentManager the component manager used for the page test
     * @param documentReference the document reference containing  the wiki macro to load
     * @throws Exception in case of error during the load of the wiki macro
     */
    public static void loadWikiMacro(PageTest pageTest, MockitoComponentManager componentManager,
        DocumentReference documentReference) throws Exception
    {
        XWikiDocument xWikiDocument = pageTest.loadPage(documentReference);

        // Make the wiki component manager point to the default component manager.
        componentManager.registerComponent(ComponentManager.class, "wiki",
            componentManager.getInstance(ComponentManager.class));
        when(componentManager.<AuthorizationManager>getInstance(AuthorizationManager.class)
            .hasAccess(Right.ADMIN, new DocumentReference("xwiki", "XWiki", "Admin"),
                xWikiDocument.getDocumentReference().getWikiReference())).thenReturn(true);
        // Simulate the wikimacrolistener event.
        componentManager.<EventListener>getInstance(EventListener.class, "wikimacrolistener")
            .onEvent(new DocumentCreatedEvent(), xWikiDocument, null);
    }
}
