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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import org.apache.commons.jrcs.rcs.Version;

import java.util.List;

public interface XWikiStoreInterface {
    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException;
    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context, boolean bTransaction) throws XWikiException;
    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException;
    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, String version, XWikiContext context) throws XWikiException;
    public void deleteXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException;
    public Version[] getXWikiDocVersions(XWikiDocInterface doc, XWikiContext context) throws XWikiException;
    public List getClassList(XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException;
    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;

    void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;

    List search(String sql, int nb, int start, XWikiContext context) throws XWikiException;

    void cleanUp(XWikiContext context);

}
