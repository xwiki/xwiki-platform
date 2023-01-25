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
package org.xwiki.internal.macro.source;

import javax.inject.Inject;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.macro.source.MacroContentWikiSourceFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide content coming from XWiki model entities.
 * 
 * @version $Id$
 * @since 15.1RC1
 * @since 14.10.5
 */
public abstract class AbstractEntityMacroContentWikiSourceFactory
    extends AbstractEntityMacroContentSourceFactory<MacroContentWikiSource> implements MacroContentWikiSourceFactory
{
    @Inject
    private ComponentManager componentManager;

    @Override
    protected MacroContentWikiSource getContent(XWikiDocument document, EntityReference entityReference,
        MacroContentSourceReference reference, XWikiContext xcontext) throws MacroExecutionException
    {
        if (!this.componentManager.hasComponent(EntityMacroContentWikiSourceLoader.class,
            entityReference.getType().name())) {
            throw new MacroExecutionException(
                "Unsupported entity type [" + entityReference.getType() + "] for reference [" + entityReference + "]");
        }

        EntityMacroContentWikiSourceLoader loader;
        try {
            loader = this.componentManager.getInstance(EntityMacroContentWikiSourceLoader.class,
                entityReference.getType().name());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Unexpected error when initializing the content loader for entity type ["
                + entityReference.getType() + "]", e);
        }

        return loader.load(document, entityReference, reference, xcontext);
    }
}
