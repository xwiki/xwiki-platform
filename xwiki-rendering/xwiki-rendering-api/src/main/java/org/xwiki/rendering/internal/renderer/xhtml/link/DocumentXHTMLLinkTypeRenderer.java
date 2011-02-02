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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Handle XHTML rendering for links to documents.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("doc")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentXHTMLLinkTypeRenderer extends AbstractXHTMLLinkTypeRenderer implements Initializable
{
    /**
     * The class attribute 'wikilink'.
     */
    private static final String WIKILINK = "wikilink";

    /**
     * Used to generate the link targeting a local document.
     */
    private WikiModel wikiModel;

    /**
     * Used to generate a link label.
     */
    @Requirement
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Try to find a WikiModel implementation and set it if it can be found. If not it means we're in
        // non wiki mode (i.e. no attachment in wiki documents and no links to documents for example).
        try {
            this.wikiModel = this.componentManager.lookup(WikiModel.class);
        } catch (ComponentLookupException e) {
            // There's no WikiModel implementation available. this.wikiModel stays null.
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkTypeRenderer#beginLink(org.xwiki.rendering.listener.reference.ResourceReference , boolean, Map)
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        if (this.wikiModel == null) {
            super.beginLink(reference, isFreeStandingURI, parameters);
        } else {
            beginInternalLink(reference, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * @see AbstractXHTMLLinkTypeRenderer#computeLabel(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    @Override
    protected String computeLabel(ResourceReference reference)
    {
        return this.linkLabelGenerator.generate(reference);
    }

    /**
     * {@inheritDoc}
     * @see AbstractXHTMLLinkTypeRenderer#beginLinkExtraAttributes(org.xwiki.rendering.listener.reference.ResourceReference , java.util.Map, java.util.Map)
     */
    @Override
    protected void beginLinkExtraAttributes(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> anchorAttributes)
    {
        if (StringUtils.isEmpty(reference.getReference())) {
            renderAutoLink(reference, spanAttributes, anchorAttributes);
        } else {
            anchorAttributes.put(XHTMLLinkRenderer.HREF, reference.getReference());
        }
    }

    /**
     * Start of an internal link.
     *
     * @param reference the reference to the link
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    private void beginInternalLink(ResourceReference reference, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> anchorAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        anchorAttributes.putAll(parameters);

        if (StringUtils.isEmpty(reference.getReference()) || this.wikiModel.isDocumentAvailable(reference)) {
            spanAttributes.put(CLASS, WIKILINK);
            anchorAttributes.put(XHTMLLinkRenderer.HREF, this.wikiModel.getDocumentViewURL(reference));
        } else {
            // The wiki document doesn't exist
            spanAttributes.put(CLASS, "wikicreatelink");
            anchorAttributes.put(XHTMLLinkRenderer.HREF, this.wikiModel.getDocumentEditURL(reference));
        }

        getXHTMLWikiPrinter().printXMLStartElement(SPAN, spanAttributes);
        getXHTMLWikiPrinter().printXMLStartElement(XHTMLLinkRenderer.ANCHOR, anchorAttributes);
    }

    /**
     * @param reference the reference to the link
     * @param spanAttributes the span element where to put the class
     * @param aAttributes the anchor element where to put the reference
     */
    private void renderAutoLink(ResourceReference reference, Map<String, String> spanAttributes,
        Map<String, String> aAttributes)
    {
        spanAttributes.put(CLASS, WIKILINK);

        StringBuilder buffer = new StringBuilder();
        String queryString = reference.getParameter(DocumentResourceReference.QUERY_STRING);
        if (queryString != null) {
            buffer.append('?');
            buffer.append(queryString);
        }
        buffer.append('#');
        String anchor = reference.getParameter(DocumentResourceReference.ANCHOR);
        if (anchor != null) {
            buffer.append(anchor);
        }

        aAttributes.put(XHTMLLinkRenderer.HREF, buffer.toString());
    }
}
