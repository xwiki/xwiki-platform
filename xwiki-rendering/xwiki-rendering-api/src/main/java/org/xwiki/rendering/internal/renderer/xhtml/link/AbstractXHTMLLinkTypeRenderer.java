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

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.link.URILabelGenerator;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common code for XHTML Link Type Renderer implementations.
 *  
 * @version $Id$
 * @since 2.5M2
 */
public abstract class AbstractXHTMLLinkTypeRenderer implements XHTMLLinkTypeRenderer
{
    /**
     * The XHTML element <code>class</code> parameter.
     */
    protected static final String CLASS = "class";

    /**
     * The name of the XHTML format element.
     */
    protected static final String SPAN = "span";

    /**
     * Used to look for {@link org.xwiki.rendering.renderer.reference.link.URILabelGenerator} component implementations
     * when computing labels.
     */
    @Requirement
    protected ComponentManager componentManager;

    /**
     * The XHTML printer to use to output links as XHTML.
     */
    private XHTMLWikiPrinter xhtmlPrinter;

    /**
     * @see #setHasLabel(boolean)
     */
    private boolean hasLabel;

    /**
     * @return See {@link #setHasLabel(boolean)}
     */
    protected boolean hasLabel()
    {
        return this.hasLabel;
    }

    /**
     * {@inheritDoc}
     *
     * @see XHTMLLinkTypeRenderer#setHasLabel(boolean)
     */
    public void setHasLabel(boolean hasLabel)
    {
        this.hasLabel = hasLabel;
    }

    /**
     * {@inheritDoc}
     *
     * @see XHTMLLinkTypeRenderer#setXHTMLWikiPrinter(org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     *
     * @see XHTMLLinkTypeRenderer#getXHTMLWikiPrinter()
     */
    public XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.xhtmlPrinter;
    }

    /**
     * Hook called when rendering the beginning of a link to allow implementation classes to augment the passed span and
     * anchor attributes as they see fit.
     *
     * @param reference the reference of the link being rendered
     * @param spanAttributes the HTML attributes for the SPAN HTML element added around the ANCHOR HTML element
     * @param anchorAttributes the HTML attributes for the ANCHOR element
     */
    protected abstract void beginLinkExtraAttributes(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> anchorAttributes);

    /**
     * Default implementation for computing a link label when no label has been specified. Can be overwritten by
     * implementations to provide a different algorithm.
     *
     * @param reference the reference of the link for which to compute the label
     * @return the computed label
     */
    protected String computeLabel(ResourceReference reference)
    {
        // Look for a component implementing URILabelGenerator with a role hint matching the link scheme.
        // If not found then use the full reference as the label.
        // If there's no scheme separator then use the full reference as the label. Note that this can happen
        // when we're not in wiki mode (since all links are considered URIs when not in wiki mode).
        String label;
        try {
            URILabelGenerator uriLabelGenerator = this.componentManager.lookup(URILabelGenerator.class,
                reference.getType().getScheme());
            label = uriLabelGenerator.generateLabel(reference);
        } catch (ComponentLookupException e) {
            label = reference.getReference();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     *
     * @see XHTMLLinkTypeRenderer#
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> anchorAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        anchorAttributes.putAll(parameters);

        spanAttributes.put(CLASS, "wikiexternallink");
        if (isFreeStandingURI) {
            anchorAttributes.put(CLASS, "wikimodel-freestanding");
        }

        beginLinkExtraAttributes(reference, spanAttributes, anchorAttributes);

        getXHTMLWikiPrinter().printXMLStartElement(SPAN, spanAttributes);
        getXHTMLWikiPrinter().printXMLStartElement(XHTMLLinkRenderer.ANCHOR, anchorAttributes);
    }

    /**
     * {@inheritDoc}
     *
     * @see XHTMLLinkRenderer#endLink(org.xwiki.rendering.listener.reference.ResourceReference , boolean, Map)
     */
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // If there was no link content then generate it based on the passed reference
        if (!hasLabel()) {
            getXHTMLWikiPrinter().printXMLStartElement(SPAN, new String[][]{{CLASS, "wikigeneratedlinkcontent"}});
            getXHTMLWikiPrinter().printXML(computeLabel(reference));
            getXHTMLWikiPrinter().printXMLEndElement(SPAN);
        }

        getXHTMLWikiPrinter().printXMLEndElement(XHTMLLinkRenderer.ANCHOR);
        getXHTMLWikiPrinter().printXMLEndElement(SPAN);
    }
}
