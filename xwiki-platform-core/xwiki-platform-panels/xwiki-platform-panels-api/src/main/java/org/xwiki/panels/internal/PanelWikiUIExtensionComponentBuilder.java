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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Allows to build {@link PanelWikiUIExtension} components.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
@Named(PanelClassDocumentInitializer.CLASS_REFERENCE_STRING)
public class PanelWikiUIExtensionComponentBuilder implements WikiBaseObjectComponentBuilder
{
    /**
     * The component manager.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Content parser used to parse the panel content.
     */
    @Inject
    private ContentParser parser;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentResolver;

    @Override
    public EntityReference getClassReference()
    {
        return PanelClassDocumentInitializer.CLASS_REFERENCE;
    }

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        String content = baseObject.getStringValue("content");
        Syntax syntax = baseObject.getOwnerDocument().getSyntax();
        DocumentReference documentReference = baseObject.getOwnerDocument().getDocumentReference();
        DocumentReference authorReference = baseObject.getOwnerDocument().getAuthorReference();

        XDOM xdom = this.parser.parse(content, syntax, documentReference);

        try {
            return Collections.<WikiComponent>singletonList(new PanelWikiUIExtension(baseObject.getReference(),
                authorReference, xdom, syntax, this.componentManager));
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to initialize Panel UI extension [%s]", baseObject),
                e);
        }
    }
}
