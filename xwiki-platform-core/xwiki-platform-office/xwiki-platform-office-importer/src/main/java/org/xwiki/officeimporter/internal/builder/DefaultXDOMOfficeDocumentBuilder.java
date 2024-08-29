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
package org.xwiki.officeimporter.internal.builder;

import java.io.InputStream;
import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Default implementation of {@link XDOMOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
@Singleton
public class DefaultXDOMOfficeDocumentBuilder implements XDOMOfficeDocumentBuilder
{
    /**
     * Xhtml office document builder used internally.
     */
    @Inject
    private XHTMLOfficeDocumentBuilder xhtmlOfficeDocumentBuilder;

    /**
     * XHTML/1.0 syntax parser used to build an XDOM from an XHTML input.
     */
    @Inject
    @Named("xhtml/1.0")
    private Parser xHtmlParser;

    /**
     * Component manager to be passed into XDOMOfficeDocument.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Used to serialize the target document reference.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference,
        boolean filterStyles) throws OfficeImporterException
    {
        XDOMOfficeDocument xdomOfficeDocument =
            build(this.xhtmlOfficeDocumentBuilder.build(officeFileStream, officeFileName, reference, filterStyles));
        // Make sure references are resolved relative to the target document reference.
        xdomOfficeDocument.getContentDocument().getMetaData()
            .addMetaData(MetaData.BASE, entityReferenceSerializer.serialize(reference));
        return xdomOfficeDocument;
    }

    @Override
    public XDOMOfficeDocument build(XHTMLOfficeDocument xhtmlOfficeDocument) throws OfficeImporterException
    {
        Document xhtmlDoc = xhtmlOfficeDocument.getContentDocument();
        HTMLUtils.stripHTMLEnvelope(xhtmlDoc);
        XDOM xdom = null;
        try {
            xdom = this.xHtmlParser.parse(new StringReader(HTMLUtils.toString(xhtmlDoc)));
        } catch (ParseException ex) {
            throw new OfficeImporterException("Error: Could not parse xhtml office content.", ex);
        }
        return new XDOMOfficeDocument(xdom, xhtmlOfficeDocument.getArtifactsMap(), this.componentManager,
            xhtmlOfficeDocument.getConverterResult());
    }
}
