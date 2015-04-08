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
package com.xpn.xwiki.internal.render;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.content.Link;
import com.xpn.xwiki.content.parsers.DocumentParser;
import com.xpn.xwiki.content.parsers.RenamePageReplaceLinkHandler;
import com.xpn.xwiki.content.parsers.ReplacementResultCollection;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.web.Utils;

/**
 * Override and extends {@link DefaultOldRendering} with xwiki/1.0 and old rendering framework based implementation
 * specific behaviors.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
public class XWiki10OldRendering extends DefaultOldRendering
{
    @Inject
    private XWikiRenderingEngine engine;

    @Inject
    private ContextualLocalizationManager localization;

    @Inject
    private Execution execution;

    @Override
    public void flushCache()
    {
        this.engine.flushCache();

        super.flushCache();
    }

    @Override
    public void resetRenderingEngine(XWikiContext xcontext) throws XWikiException
    {
        if (this.engine instanceof Initializable) {
            try {
                ((Initializable) engine).initialize();
            } catch (InitializationException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Failed to initialize rendering engine", e);
            }
        }

        super.resetRenderingEngine(xcontext);
    }

    @Override
    public void renameLinks(XWikiDocument backlinkDocument, DocumentReference oldReference,
        DocumentReference newReference, XWikiContext context) throws XWikiException
    {
        if (backlinkDocument.is10Syntax()) {
            Link oldLink = createLink(oldReference);
            Link newLink = createLink(newReference);

            // For each backlink to rename, parse the backlink document and replace the links with the new name.
            // Note: we ignore invalid links here. Invalid links should be shown to the user so
            // that they fix them but the rename feature ignores them.
            DocumentParser documentParser = new DocumentParser();

            // This link handler recognizes that 2 links are the same when they point to the same
            // document (regardless of query string, target or alias). It keeps the query string,
            // target and alias from the link being replaced.
            RenamePageReplaceLinkHandler linkHandler = new RenamePageReplaceLinkHandler();

            // Note: Here we cannot do a simple search/replace as there are several ways to point
            // to the same document. For example [Page], [Page?param=1], [currentwiki:Page],
            // [CurrentSpace.Page] all point to the same document. Thus we have to parse the links
            // to recognize them and do the replace.
            ReplacementResultCollection result =
                documentParser.parseLinksAndReplace(backlinkDocument.getContent(), oldLink, newLink, linkHandler,
                    backlinkDocument.getDocumentReference().getLastSpaceReference().getName());

            backlinkDocument.setContent((String) result.getModifiedContent());
        } else if (Utils.getContextComponentManager().hasComponent(BlockRenderer.class,
            backlinkDocument.getSyntax().toIdString())) {
            super.renameLinks(backlinkDocument, oldReference, newReference, context);
        }

        // Save if content changed
        if (backlinkDocument.isContentDirty()) {
            String saveMessage =
                localizePlainOrKey("core.comment.renameLink",
                    this.compactEntityReferenceSerializer.serialize(newReference));
            backlinkDocument.setAuthorReference(context.getUserReference());
            context.getWiki().saveDocument(backlinkDocument, saveMessage, true, context);
        }
    }

    private Link createLink(DocumentReference documentReference)
    {
        Link link = new Link();

        link.setVirtualWikiAlias(documentReference.getWikiReference().getName());
        link.setSpace(documentReference.getLastSpaceReference().getName());
        link.setPage(documentReference.getName());

        return link;
    }

    @Override
    public String renderText(String text, XWikiDocument doc, XWikiContext xcontext)
    {
        return this.engine.renderText(text, doc, xcontext);
    }

    @Override
    public String renderTemplate(String template, XWikiContext xcontext)
    {
        return this.engine.getRenderer("wiki").render(xcontext.getWiki().parseTemplate(template, xcontext),
            xcontext.getDoc(), xcontext.getDoc(), xcontext);
    }

    @Override
    public String renderTemplate(String template, String skin, XWikiContext xcontext)
    {
        return this.engine.getRenderer("wiki").render(xcontext.getWiki().parseTemplate(template, skin, xcontext),
            xcontext.getDoc(), xcontext.getDoc(), xcontext);
    }

    @Override
    public String parseContent(String content, XWikiContext xcontext)
    {
        String parsedContent;

        if (StringUtils.isNotEmpty(content)) {
            parsedContent = this.engine.interpretText(content, xcontext.getDoc(), xcontext);
        } else {
            parsedContent = "";
        }

        return parsedContent;
    }

    private String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(this.localization.getTranslationPlain(key, parameters), key);
    }

    @Override
    public Set<XWikiLink> extractLinks(XWikiDocument doc, XWikiContext xcontext) throws XWikiException
    {
        if (doc.is10Syntax()) {
            // call to RenderEngine and converting the list of links into a list of backlinks
            // Note: We need to set the passed document as the current document as the "wiki"
            // renderer uses context.getDoc().getSpace() to find out the space name if no
            // space is specified in the link. A better implementation would be to pass
            // explicitly the current space to the render() method.
            ExecutionContext econtext = this.execution.getContext();

            List<String> linkReferences;
            try {
                // Create new clean context to avoid wiki module requests in same session
                XWikiContext renderContext = xcontext.clone();

                renderContext.setDoc(doc);
                econtext.setProperty("xwikicontext", renderContext);

                setSession(null, renderContext);
                setTransaction(null, renderContext);

                XWikiRenderer renderer = this.engine.getRenderer("wiki");
                renderer.render(doc.getContent(), doc, doc, renderContext);

                linkReferences = (List<String>) renderContext.get("links");
            } catch (Exception e) {
                // If the rendering fails lets forget backlinks without errors
                linkReferences = Collections.emptyList();
            } finally {
                econtext.setProperty("xwikicontext", xcontext);
            }

            if (linkReferences != null) {
                Set<XWikiLink> links = new LinkedHashSet<>(linkReferences.size());
                for (String reference : linkReferences) {
                    // XWikiLink is the object declared in the Hibernate mapping
                    XWikiLink link = new XWikiLink();
                    link.setDocId(doc.getId());
                    link.setFullName(doc.getFullName());
                    link.setLink(reference);

                    links.add(link);
                }

                return links;
            }

            return Collections.emptySet();
        }

        return super.extractLinks(doc, xcontext);
    }

    /**
     * Allows to set the current session in the context This is set in beginTransaction
     *
     * @param session
     * @param context
     */
    private void setSession(Session session, XWikiContext context)
    {
        if (session == null) {
            context.remove("hibsession");
        } else {
            context.put("hibsession", session);
        }
    }

    /**
     * Allows to set the current transaction This is set in beginTransaction
     *
     * @param transaction
     * @param context
     */
    private void setTransaction(Transaction transaction, XWikiContext context)
    {
        if (transaction == null) {
            context.remove("hibtransaction");
        } else {
            context.put("hibtransaction", transaction);
        }
    }
}
