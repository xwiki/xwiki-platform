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

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

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
    private final Map<String, OfficeDocumentArtifact> artifactsMap;

    /**
     * {@link ComponentManager} used to lookup for various renderers.
     */
    private ComponentManager componentManager;

    private OfficeConverterResult converterResult;

    /**
     * Creates a new {@link XDOMOfficeDocument}.
     *
     * @param xdom {@link XDOM} corresponding to office document content.
     * @param artifacts artifacts for this office document.
     * @param componentManager {@link ComponentManager} used to lookup for various renderers.
     * @param converterResult the {@link OfficeConverterResult} used to build that object.
     * @since 14.10.8
     * @since 15.3RC1
     */
    @Unstable
    public XDOMOfficeDocument(XDOM xdom, Map<String, OfficeDocumentArtifact> artifacts,
        ComponentManager componentManager, OfficeConverterResult converterResult)
    {
        this.xdom = xdom;
        this.artifactsMap = artifacts;
        this.componentManager = componentManager;
        this.converterResult = converterResult;
    }

    @Override
    public XDOM getContentDocument()
    {
        return this.xdom;
    }

    @Override
    public String getContentAsString()
    {
        return getContentAsString(Syntax.XWIKI_2_1.toIdString());
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
    public Map<String, OfficeDocumentArtifact> getArtifactsMap()
    {
        return this.artifactsMap;
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
            // TODO: the value should be asked to the store API instead of being hardcoded
            if (title.length() > 768) {
                title = title.substring(0, 768);
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

    @Override
    public void close() throws IOException
    {
        if (this.converterResult != null) {
            this.converterResult.close();
        }
    }

    @Override
    public OfficeConverterResult getConverterResult()
    {
        return this.converterResult;
    }
}
