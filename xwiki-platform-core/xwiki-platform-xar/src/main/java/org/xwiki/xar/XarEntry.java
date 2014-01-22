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

import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.xar.internal.model.XarModel;

/**
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public class XarEntry extends LocalDocumentReference
{
    /**
     * The name of the entry in the ZIP stream.
     */
    private String entryName;

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
        this(reference, name, XarModel.ACTION_OVERWRITE);
    }

    public XarEntry(LocalDocumentReference reference, String name, int defaultAction)
    {
        super(reference);

        this.entryName = name;
    }

    public String getEntryName()
    {
        return this.entryName;
    }

    public int getDefaultAction()
    {
        return this.defaultAction;
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
