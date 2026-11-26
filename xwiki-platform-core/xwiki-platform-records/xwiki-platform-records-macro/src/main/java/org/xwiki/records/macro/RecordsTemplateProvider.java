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
package org.xwiki.records.macro;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Template provider the entries created from a Records table start from.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public class RecordsTemplateProvider extends DocumentReference
{
    /**
     * The template provider meaning that the new entries start from no template, rather than from the one named after
     * the data type. No such page exists: the Live Data {@code liveTable} source recognizes this reference, in any
     * wiki.
     */
    public static final LocalDocumentReference NONE = new LocalDocumentReference("XWiki", "NoTemplateProvider");

    /**
     * @param reference the reference of the template provider, for example the one of
     *     {@code xwiki:Clients.ProjectTemplateProvider}
     */
    public RecordsTemplateProvider(EntityReference reference)
    {
        super(reference);
    }
}
