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

import org.w3c.dom.Document;
import org.xwiki.xml.html.HTMLUtils;

/**
 * A {@link OfficeDocument} backed by a w3c {@link Document} containing xhtml markup.
 *
 * @version $Id$
 * @since 2.1M1
 */
public class XHTMLOfficeDocument implements OfficeDocument
{
    /**
     * Document holding xhtml corresponding to office document content.
     */
    private Document document;

    /**
     * Artifacts for this office document.
     */
    private Map<String, byte[]> artifacts;

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     * 
     * @param document the w3c dom representing the office document.
     * @param artifacts artifacts for this office document.
     */
    public XHTMLOfficeDocument(Document document, Map<String, byte[]> artifacts)
    {
        this.document = document;
        this.artifacts = artifacts;
    }

    @Override
    public Document getContentDocument()
    {
        return this.document;
    }

    @Override
    public String getContentAsString()
    {
        return HTMLUtils.toString(this.document);
    }

    @Override
    public Map<String, byte[]> getArtifacts()
    {
        return this.artifacts;
    }
}
