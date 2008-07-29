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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of SuperDocument. This class manage an XWiki document containing provided
 * XWiki class. It add some specifics methods, getters and setters for this type of object and
 * fields. It also override {@link com.xpn.xwiki.api.Document} (and then {@link XWikiDocument})
 * isNew concept considering as new a document that does not contains an XWiki object of the
 * provided XWiki class.
 * 
 * @version $Id: $
 * @see SuperDocument
 * @see SuperClass
 * @future XA2 : rename to DefaultDocumentObject.
 * @deprecated Use {@link DefaultXObjectDocument} since 1.0 RC1
 */
@Deprecated
public class DefaultSuperDocument extends DefaultXObjectDocument implements SuperDocument
{
    /**
     * Create instance of DefaultSuperDocument from provided XWikiDocument.
     * 
     * @param sclass the class manager for this document.
     * @param xdoc the XWikiDocument to manage.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @throws XWikiException error when calling {@link #reload(XWikiContext)}.
     */
    public DefaultSuperDocument(SuperClass sclass, XWikiDocument xdoc, int objectId,
        XWikiContext context) throws XWikiException
    {
        super(sclass, xdoc, objectId, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperDocument#getSuperClass()
     */
    public SuperClass getSuperClass()
    {
        return (SuperClass) this.sclass;
    }
}
