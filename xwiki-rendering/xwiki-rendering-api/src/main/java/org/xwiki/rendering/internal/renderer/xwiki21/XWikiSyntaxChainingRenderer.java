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
package org.xwiki.rendering.internal.renderer.xwiki21;

import org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxListenerChain;
import org.xwiki.rendering.internal.renderer.xwiki21.reference.XWikiSyntaxResourceRenderer;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;

/**
 * Convert listener events to XWiki Syntax 2.1 output.
 *
 * @version $Id$
 * @since 2.5M2
 */
public class XWikiSyntaxChainingRenderer
    extends org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxChainingRenderer
{
    /**
     * @param listenerChain the rendering listener chain
     * @param linkReferenceSerializer the serializer to use to serialize link references
     * @param imageReferenceSerializer the serializer to use to serialize image references
     * @since 2.5RC1
     */
    public XWikiSyntaxChainingRenderer(ListenerChain listenerChain,
        ResourceReferenceSerializer linkReferenceSerializer, ResourceReferenceSerializer imageReferenceSerializer)
    {
        super(listenerChain, linkReferenceSerializer, imageReferenceSerializer);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxChainingRenderer#createXWikiSyntaxLinkRenderer(
     *      ListenerChain, org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer)
     * @since 2.5RC1
     *
     */
    @Override
    protected XWikiSyntaxResourceRenderer
    createXWikiSyntaxLinkRenderer(ListenerChain listenerChain, ResourceReferenceSerializer linkReferenceSerializer)
    {
        return new XWikiSyntaxResourceRenderer((XWikiSyntaxListenerChain) listenerChain, linkReferenceSerializer);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxChainingRenderer#createXWikiSyntaxImageRenderer(
     *      ListenerChain, org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer)
     * @since 2.5RC1
     *
     */
    @Override
    protected XWikiSyntaxResourceRenderer
    createXWikiSyntaxImageRenderer(ListenerChain listenerChain, ResourceReferenceSerializer imageReferenceSerializer)
    {
        return new XWikiSyntaxResourceRenderer((XWikiSyntaxListenerChain) listenerChain, imageReferenceSerializer);
    }
}
