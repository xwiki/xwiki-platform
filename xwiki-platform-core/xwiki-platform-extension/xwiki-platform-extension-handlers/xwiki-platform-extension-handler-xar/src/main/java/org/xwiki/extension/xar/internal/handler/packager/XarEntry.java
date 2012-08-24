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
package org.xwiki.extension.xar.internal.handler.packager;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 * @since 4.0M1
 */
public class XarEntry
{
    private EntityReference documentReference;

    private String language;

    private String entryName;

    public XarEntry()
    {
    }

    public XarEntry(String space, String page, String language)
    {
        this.documentReference =
            new EntityReference(page, EntityType.DOCUMENT, new EntityReference(space, EntityType.SPACE));
        this.language = language;
    }

    public XarEntry(EntityReference documentReference, String language)
    {
        this.documentReference = documentReference;
        this.language = language;
    }

    public EntityReference getDocumentReference()
    {
        return this.documentReference;
    }

    public void setDocumentReference(EntityReference documentReference)
    {
        this.documentReference = documentReference;
    }

    public String getLanguage()
    {
        return this.language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getEntryName()
    {
        return this.entryName;
    }

    public void setEntryName(String entryName)
    {
        this.entryName = entryName;
    }

    // Object

    @Override
    public String toString()
    {
        return this.documentReference + ", language = [" + getLanguage() + "]";
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;

        if (obj == this) {
            equals = true;
        } else if (obj instanceof XarEntry) {
            XarEntry xarEntry = (XarEntry) obj;

            equals =
                getDocumentReference().equals(xarEntry.getDocumentReference())
                    && getLanguage().equals(xarEntry.getLanguage());
        }

        return equals;
    }
}
