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
package com.xpn.xwiki.internal.mandatory;

import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Abstract class for XWiki.XWikiComments document initializer.
 *
 * @version $Id$
 * @since 9.11.9
 * @since 10.8.2
 * @since 10.10RC1
 */
public abstract class AbstractCommentsDocumentInitializer extends AbstractMandatoryClassInitializer
{
    public AbstractCommentsDocumentInitializer(EntityReference reference)
    {
        super(reference);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addUsersField("author", "Author", 30, false);
        xclass.addDateField("date", "Date");
    }
}
