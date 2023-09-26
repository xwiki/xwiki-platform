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
package org.xwiki.export.pdf.internal.job;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * The rendering parameters.
 * 
 * @version $Id$
 * @since 14.10.17
 * @since 15.5.3
 * @since 15.8RC1
 */
public class DocumentRendererParameters
{
    private boolean withTitle;

    private ObjectPropertyReference metadataReference;

    /**
     * @return {@code true} if the document title is also rendered (before the document content), {@code false}
     *         otherwise
     */
    public boolean isWithTitle()
    {
        return this.withTitle;
    }

    /**
     * @param withTitle {@code true} to render also the document title (before the document content), {@code false}
     * @return this instance
     */
    public DocumentRendererParameters withTitle(boolean withTitle)
    {
        this.withTitle = withTitle;
        return this;
    }

    /**
     * @return the reference of the object property that is going to be rendered to produce the document metadata that
     *         could be displayed in the PDF header or footer
     */
    public ObjectPropertyReference getMetadataReference()
    {
        return this.metadataReference;
    }

    /**
     * @param metadataReference the reference of the object property that is going to be rendered to produce the
     *            document metadata that could be displayed in the PDF header or footer
     * @return this instance
     */
    public DocumentRendererParameters withMetadataReference(ObjectPropertyReference metadataReference)
    {
        this.metadataReference = metadataReference;
        return this;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(isWithTitle()).append(getMetadataReference()).toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DocumentRendererParameters that = (DocumentRendererParameters) obj;

        return new EqualsBuilder().append(isWithTitle(), that.isWithTitle())
            .append(getMetadataReference(), that.getMetadataReference()).isEquals();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("withTitle", isWithTitle())
            .append("metadataReference", getMetadataReference()).build();
    }
}
