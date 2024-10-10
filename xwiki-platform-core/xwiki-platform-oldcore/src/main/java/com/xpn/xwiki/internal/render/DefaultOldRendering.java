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

import java.io.StringWriter;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.internal.render.groovy.ParseGroovyFromString;

/**
 * Default implementation of {@link OldRendering} that try as much as possible to do something that makes sense without
 * using old rendering engine.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
public class DefaultOldRendering implements OldRendering
{
    @Inject
    protected Provider<RenderingCache> cache;

    @Inject
    @Named("context")
    protected Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    @Named("compact")
    protected EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    protected DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    protected EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @Inject
    protected Provider<VelocityManager> velocityManagerProvider;

    @Inject
    protected Provider<ParseGroovyFromString> parseGroovyFromString;

    @Inject
    protected Logger logger;

    @Inject
    protected ReferenceRenamer referenceRenamer;

    @Override
    public void renameLinks(XWikiDocument backlinkDocument, DocumentReference oldReference,
        DocumentReference newReference, XWikiContext context) throws XWikiException
    {
        // FIXME: Duplicate code. See org.xwiki.refactoring.internal.DefaultLinkRefactoring#renameLinks in
        // xwiki-platform-refactoring-default

        if (this.contextComponentManagerProvider.get().hasComponent(BlockRenderer.class,
            backlinkDocument.getSyntax().toIdString())) {
            refactorDocumentLinks(backlinkDocument, oldReference, newReference, context);
        }
    }

    @Override
    public void flushCache()
    {
        this.cache.get().flushWholeCache();
    }

    /**
     * @since 2.2M1
     */
    private void refactorDocumentLinks(XWikiDocument document, DocumentReference oldDocumentReference,
        DocumentReference newDocumentReference, XWikiContext context) throws XWikiException
    {
        this.referenceRenamer.renameReferences(document.getXDOM(), document.getDocumentReference(),
            oldDocumentReference, newDocumentReference, false, Set.of());
    }

    @Override
    public String parseContent(String content, XWikiContext xcontext)
    {
        try {
            if (StringUtils.isNotEmpty(content)) {
                VelocityManager velocityManager = this.velocityManagerProvider.get();

                VelocityContext velocityContext = velocityManager.getVelocityContext();
                VelocityEngine velocityEngine = velocityManager.getVelocityEngine();

                StringWriter writer = new StringWriter();
                velocityEngine.evaluate(velocityContext, writer, xcontext.getDoc().getPrefixedFullName(), content);
                return writer.toString();
            }
        } catch (XWikiVelocityException e) {
            this.logger.error("Faield to parse content [" + content + "]", e);
        }

        return "";
    }

    @Override
    public Set<XWikiLink> extractLinks(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return doc.getUniqueWikiLinkedPages(context);
    }

    @Override
    public void resetRenderingEngine(XWikiContext context) throws XWikiException
    {
        // xwiki/1.0 specific API
    }

    @Override
    public String renderText(String text, XWikiDocument doc, XWikiContext xcontext)
    {
        return doc.getRenderedContent(text, doc.getSyntaxId(), xcontext);
    }

    @Override
    public String renderTemplate(String template, String skin, XWikiContext xcontext)
    {
        return xcontext.getWiki().parseTemplate(template, skin, xcontext);
    }

    @Override
    public String renderTemplate(String template, XWikiContext xcontext)
    {
        return xcontext.getWiki().parseTemplate(template, xcontext);
    }
}
