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
package org.xwiki.wikistream.xar.internal;

import org.xwiki.model.reference.LocalDocumentReference;

public class XarEntry
{
    /**
     * The reference of the entry.
     */
    private LocalDocumentReference reference;

    /**
     * The name of the entry in the ZIP stream.
     */
    private String name;

    /**
     * The default action to set in package.xml.
     */
    private int defaultAction;

    public XarEntry(LocalDocumentReference reference)
    {
        this(reference, null);
    }

    public XarEntry(LocalDocumentReference reference, String name)
    {
        this(reference, name, XARModel.ACTION_OVERWRITE);
    }

    public XarEntry(LocalDocumentReference reference, String name, int defaultAction)
    {
        this.reference = reference;
        this.name = name;
    }

    public LocalDocumentReference getReference()
    {
        return this.reference;
    }

    public String getName()
    {
        return this.name;
    }

    public int getDefaultAction()
    {
        return this.defaultAction;
    }

    @Override
    public int hashCode()
    {
        return getReference().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof XarEntry && getReference().equals(((XarEntry) obj).getReference());
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder(getReference().toString());

        if (getName() != null) {
            str.append(' ');
            str.append('(');
            str.append(getName());
            str.append(')');
        }

        return str.toString();
    }
}
