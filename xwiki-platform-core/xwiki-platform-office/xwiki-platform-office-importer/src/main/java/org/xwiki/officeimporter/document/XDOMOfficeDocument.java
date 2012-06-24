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
package org.xwiki.officeimporter.document;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * An {@link OfficeDocument} backed by an {@link XDOM} document.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class XDOMOfficeDocument implements OfficeDocument
{
    /**
     * {@link XDOM} corresponding to office document content.
     */
    private XDOM xdom;

    /**
     * Artifacts for this office document.
     */
    private Map<String, byte[]> artifacts;

    /**
     * {@link ComponentManager} used to lookup for various renderers.
     */
    private ComponentManager componentManager;

    /**
     * Creates a new {@link XDOMOfficeDocument}.
     * 
     * @param xdom {@link XDOM} corresponding to office document content.
     * @param artifacts artifacts for this office document.
     * @param componentManager {@link ComponentManager} used to lookup for various renderers.
     */
    public XDOMOfficeDocument(XDOM xdom, Map<String, byte[]> artifacts, ComponentManager componentManager)
    {
        this.xdom = xdom;
        this.artifacts = artifacts;
        this.componentManager = componentManager;
    }

    @Override
    public XDOM getContentDocument()
    {
        return this.xdom;
    }

    @Override
    public String getContentAsString()
    {
        return getContentAsString("xwiki/2.0");
    }

    /**
     * Renders the XDOM encapsulated by this document into the given syntax.
     * 
     * @param syntaxId string identifier of the syntax.
     * @return content of this document in the given syntax or null if the syntax is invalid.
     */
    public String getContentAsString(String syntaxId)
    {
        try {
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, syntaxId);
            renderer.render(this.xdom, printer);
            return printer.toString();
        } catch (ComponentLookupException ex) {
            // Nothing to do here.
        }
        return null;
    }

    @Override
    public Map<String, byte[]> getArtifacts()
    {
        return this.artifacts;
    }

    /**
     * Tries to extract a title suitable for this document. This is done by navigating the internal {@link XDOM} and
     * finding a matching header block.
     * 
     * @return a title suitable for this document or null if no title can be found.
     */
    public String getTitle()
    {
        String title = getTitle(this.xdom);
        if (null != title) {
            // Strip line-feed and new-line characters if present.
            title = title.replaceAll("[\n\r]", "");
            // Truncate long titles.
            if (title.length() > 255) {
                title = title.substring(0, 255);
            }
        }
        return title;
    }

    /**
     * Utility method for recursively traversing the XDOM and extracting a title.
     * 
     * @param parent parent block.
     * @return a title if found or null.
     */
    private String getTitle(Block parent)
    {
        for (Block block : parent.getChildren()) {
            if (block instanceof HeaderBlock) {
                String title = renderTitle((HeaderBlock) block);
                if (!StringUtils.isBlank(title)) {
                    return title;
                }
            } else if (block instanceof SectionBlock) {
                return getTitle(block);
            }
        }
        return null;
    }

    /**
     * Utility method for rendering a title.
     * 
     * @param header header block which contains the title.
     * @return header block content rendered as a string.
     */
    private String renderTitle(HeaderBlock header)
    {
        try {
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, "plain/1.0");
            renderer.render(header, printer);
            return printer.toString();
        } catch (ComponentLookupException ex) {
            // Ignore.
        }
        return null;
    }
}
