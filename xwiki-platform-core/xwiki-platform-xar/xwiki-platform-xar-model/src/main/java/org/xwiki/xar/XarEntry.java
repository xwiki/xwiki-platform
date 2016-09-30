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
package org.xwiki.xar;

import java.beans.Transient;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.internal.model.XarModel;

/**
 * An entry (wiki page) in a XAR package.
 * <p>
 * Reuse LocalDocumentReference equals and hashCode implementation so that the entry can be used as a
 * {@link LocalDocumentReference} in a map.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
public class XarEntry extends LocalDocumentReference
{
    /**
     * @see #getEntryName()
     */
    private String entryName;

    /**
     * @see #getDefaultAction()
     */
    private int defaultAction;

    /**
     * @param reference the reference of the document
     */
    public XarEntry(LocalDocumentReference reference)
    {
        this(reference, null);
    }

    /**
     * @param reference the reference of the document
     * @param name the name of the entry (ZIP style)
     */
    public XarEntry(LocalDocumentReference reference, String name)
    {
        this(reference, name, XarModel.ACTION_OVERWRITE);
    }

    /**
     * @param reference the reference of the document
     * @param defaultAction the default action associated to a XAR entry
     * @since 7.2M1
     */
    public XarEntry(LocalDocumentReference reference, int defaultAction)
    {
        this(reference, null, defaultAction);
    }

    /**
     * @param reference the reference of the document
     * @param name the name of the entry (ZIP style)
     * @param defaultAction the default action associated to a XAR entry (not used at the moment)
     */
    public XarEntry(LocalDocumentReference reference, String name, int defaultAction)
    {
        super(reference);

        this.entryName = name;
    }

    /**
     * @return the name of the entry in the ZIP (XAR) package
     */
    public String getEntryName()
    {
        return this.entryName;
    }

    /**
     * @return the default action associated to the entry
     */
    public int getDefaultAction()
    {
        return this.defaultAction;
    }

    /**
     * @return the space of the document
     * @deprecated since 7.2M1, does not make much sense anymore with nested space
     */
    @Deprecated
    @Transient
    public String getSpaceName()
    {
        return TOSTRING_SERIALIZER.serialize(extractReference(EntityType.SPACE));
    }

    /**
     * @return the name of the document
     */
    @Transient
    public String getDocumentName()
    {
        return getName();
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder(super.toString());

        if (getEntryName() != null) {
            str.append(' ');
            str.append('(');
            str.append(getEntryName());
            str.append(')');
        }

        return str.toString();
    }
}
