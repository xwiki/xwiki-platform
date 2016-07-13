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
package org.xwiki.panels.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;

/**
 * Allows to build {@link PanelWikiUIExtension} components.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Singleton
@Named("panels")
public class PanelWikiUIExtensionComponentBuilder implements WikiComponentBuilder
{
    /**
     * The query manager, used to search for documents defining panels.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * The component manager.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Document access bridge.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Content parser used to parse the panel content.
     */
    @Inject
    private ContentParser parser;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> references = new ArrayList<DocumentReference>();

        try {
            Query query =
                queryManager.createQuery("select doc.fullName from Document doc, doc.object(Panels.PanelClass) "
                    + "as panel", Query.XWQL);
            List<String> results = query.execute();
            for (String fullName : results) {
                references.add(this.currentResolver.resolve(fullName));
            }
        } catch (Exception e) {
            // Fail "silently"
            e.printStackTrace();
        }

        return references;
    }

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        List<WikiComponent> components = new ArrayList<WikiComponent>();
        DocumentReference panelXClass = new DocumentReference(xcontext.getWikiId(), "Panels", "PanelClass");
        String content = (String) documentAccessBridge.getProperty(reference, panelXClass, "content");
        Syntax syntax = null;
        DocumentReference authorReference;

        try {
            syntax = documentAccessBridge.getDocument(reference).getSyntax();
            authorReference = xcontext.getWiki().getDocument(reference, xcontext).getAuthorReference();
            XDOM xdom = parser.parse(content, syntax, reference);

            components.add(new PanelWikiUIExtension(reference, authorReference, xdom, syntax, componentManager));
        } catch (WikiComponentException e) {
            throw e;
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to initialize Panel UI extension [%s]", reference),
                e);
        } catch (Exception e) {
            throw new WikiComponentException(String.format("Failed to retrieve panel document [%s]", reference), e);
        }

        return components;
    }
}
