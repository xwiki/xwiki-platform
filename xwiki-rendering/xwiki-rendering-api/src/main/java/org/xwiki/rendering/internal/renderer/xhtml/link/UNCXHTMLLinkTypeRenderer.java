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

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Handle XHTML rendering for UNC links (Universal Naming Convention).
 *
 * @version $Id$
 * @since 2.7M1
 */
@Component("unc")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class UNCXHTMLLinkTypeRenderer extends AbstractXHTMLLinkTypeRenderer
{
    /**
     * {@inheritDoc}
     *
     * @see AbstractXHTMLLinkTypeRenderer#beginLinkExtraAttributes(ResourceReference, java.util.Map, java.util.Map)
     */
    @Override
    protected void beginLinkExtraAttributes(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> anchorAttributes)
    {
        // Transform the UNC reference into a file URL of the format: file://///myserver/myshare/mydoc.txt
        // i.e. replace all "\" chars by "/" and prefix with "file:///".
        String fileURL = "file:///" + reference.getReference().replaceAll("\\\\", "/");

        anchorAttributes.put(XHTMLLinkRenderer.HREF, fileURL);
    }
}
