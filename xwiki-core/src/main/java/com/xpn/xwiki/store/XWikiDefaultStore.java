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
 *
 */

package com.xpn.xwiki.store;

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

public abstract class XWikiDefaultStore implements XWikiStoreInterface
{
    public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return searchDocumentsNames(wheresql, 0, 0, "", context);
    }

    public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return searchDocumentsNames(wheresql, nb, start, "", context);
    }

    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, true, 0, 0, context);
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, XWikiContext context)
        throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, 0, 0, context);
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, 0, 0, context);
    }

    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return searchDocuments(wheresql, true, nb, start, context);
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbyname, false, nb, start, context);
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException
    {
        return false;
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext xWikiContext) throws XWikiException
    {
        return false;
    }

    public void injectCustomMappings(XWikiContext context) throws XWikiException
    {
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
    }

    public List getCustomMappingPropertyList(BaseClass bclass)
    {
        return new ArrayList();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb,
        int start, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbyname, customMapping, true, nb, start, context);
    }
}
