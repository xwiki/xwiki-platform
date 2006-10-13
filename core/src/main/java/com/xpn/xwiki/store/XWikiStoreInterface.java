/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author sdumitriu
 * @author thomas
 */


package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;

import java.util.List;


public interface XWikiStoreInterface {
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException;
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public List getClassList(XWikiContext context) throws XWikiException;
    public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException;
    public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, int nb, int start, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;
    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;
    public List loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;
    public List loadBacklinks(String fullName, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;
    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException;
    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context) throws XWikiException;
    public void cleanUp(XWikiContext context);
    public void createWiki(String wikiName, XWikiContext context) throws XWikiException;
    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public List searchDocumentsNames(String wheresql, int nb, int start, String selectColumns, XWikiContext context) throws XWikiException;
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context) throws XWikiException;
    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException;
    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public List getCustomMappingPropertyList(BaseClass bclass);
    public void injectCustomMappings(XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException;
    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException;
    public List getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException;
}
