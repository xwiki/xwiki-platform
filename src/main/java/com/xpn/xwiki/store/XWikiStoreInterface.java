/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 00:58:55
 */

package com.xpn.xwiki.store;

import java.util.List;

import org.apache.commons.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;

public interface XWikiStoreInterface {
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException;
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException;
    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException;
    public List getClassList(XWikiContext context) throws XWikiException;
    public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException;
    public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, boolean distinctbyname, int nb, int start, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;
    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;
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
}
