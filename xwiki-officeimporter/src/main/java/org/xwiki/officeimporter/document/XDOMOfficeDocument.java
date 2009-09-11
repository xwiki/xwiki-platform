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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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

    /**
     * {@inheritDoc}
     */
    public XDOM getContentDocument()
    {
        return this.xdom;
    }        

    /**
     * {@inheritDoc}
     */
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
            BlockRenderer renderer = this.componentManager.lookup(BlockRenderer.class, syntaxId);
            renderer.render(this.xdom, printer);
            return printer.toString();
        } catch (ComponentLookupException ex) {
            // Nothing to do here.
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, byte[]> getArtifacts()
    {
        return this.artifacts;
    }        
}
