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
package org.xwiki.skinx.internal;

import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;

/**
 * Base class to initialize skin extensions classes.
 * 
 * @version $Id$
 * @since 7.3M1
 */
public abstract class AbstractSkinExtensionDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     */
    public AbstractSkinExtensionDocumentInitializer(EntityReference reference)
    {
        super(reference);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("name", "Name", 30);
        xclass.addTextAreaField("code", "Code", 50, 20, ContentType.PURE_TEXT);
        xclass.addStaticListField("use", "Use this extension", "currentPage|onDemand|always");
        xclass.addBooleanField("parse", "Parse content", "yesno");
        xclass.addStaticListField("cache", "Caching policy", "long|short|default|forbid", "long");
    }
}
