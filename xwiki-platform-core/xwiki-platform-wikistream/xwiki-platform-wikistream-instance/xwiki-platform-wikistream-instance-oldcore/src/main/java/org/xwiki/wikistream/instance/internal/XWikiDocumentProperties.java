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
package org.xwiki.wikistream.instance.internal;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XWikiDocumentProperties extends XWikiAttachmentProperties implements BaseObjectProperties
{
    private boolean withWikiDocumentRevisions = true;

    private boolean withWikiObjects = true;

    private boolean withWikiClass = true;

    private boolean withWikiAttachments = true;

    private boolean withWikiDocumentContentHTML = false;

    public boolean isWithWikiDocumentRevisions()
    {
        return this.withWikiDocumentRevisions;
    }

    public void setWithWikiDocumentRevisions(boolean withWikiDocumentRevisions)
    {
        this.withWikiDocumentRevisions = withWikiDocumentRevisions;
    }

    public boolean isWithWikiAttachments()
    {
        return this.withWikiAttachments;
    }

    public void setWithWikiAttachments(boolean withWikiAttachments)
    {
        this.withWikiAttachments = withWikiAttachments;
    }

    public boolean isWithWikiClass()
    {
        return this.withWikiClass;
    }

    public void setWithWikiClass(boolean withWikiClass)
    {
        this.withWikiClass = withWikiClass;
    }

    public boolean isWithWikiObjects()
    {
        return this.withWikiObjects;
    }

    public void setWithWikiObjects(boolean withWikiObjects)
    {
        this.withWikiObjects = withWikiObjects;
    }

    public boolean isWithWikiDocumentContentHTML()
    {
        return this.withWikiDocumentContentHTML;
    }

    public void setWithWikiDocumentContentHTML(boolean withWikiDocumentContentHTML)
    {
        this.withWikiDocumentContentHTML = withWikiDocumentContentHTML;
    }
}
