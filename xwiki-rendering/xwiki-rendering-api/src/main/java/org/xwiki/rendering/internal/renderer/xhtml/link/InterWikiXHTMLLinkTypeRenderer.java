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
package org.xwiki.rendering.internal.renderer.xhtml.link;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.reference.InterWikiResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;

import java.util.Map;
import java.util.Properties;

/**
 * Handle XHTML rendering for interwiki links.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("interwiki")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class InterWikiXHTMLLinkTypeRenderer extends AbstractXHTMLLinkTypeRenderer
{
    /**
     * Used to get access to the InterWiki definitions.
     */
    @Requirement
    private RenderingConfiguration renderingConfiguration;

    /**
     * {@inheritDoc}
     *
     * @see AbstractXHTMLLinkTypeRenderer#beginLinkExtraAttributes(org.xwiki.rendering.listener.reference.ResourceReference ,
     *      java.util.Map, java.util.Map)
     */
    @Override
    protected void beginLinkExtraAttributes(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> anchorAttributes)
    {
        // Look for an InterWiki definition for the passed Link. If not found then simply use the InterWiki Path.
        String interWikiAlias = reference.getParameter(InterWikiResourceReference.INTERWIKI_ALIAS);
        Properties definitions = this.renderingConfiguration.getInterWikiDefinitions();
        if (definitions.containsKey(interWikiAlias)) {
            anchorAttributes.put(XHTMLLinkRenderer.HREF, definitions.getProperty(interWikiAlias)
                + reference.getReference());
        } else {
            anchorAttributes.put(XHTMLLinkRenderer.HREF, reference.getReference());
        }
    }
}
