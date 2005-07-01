/**
 * ===================================================================
 *
 * Copyright (c) 2005 Jérémi Joslin, XpertNet, All rights reserved.
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
 */

package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;


public class DocumentInfoAPI extends Api
{
    private DocumentInfo       doc;

    public DocumentInfoAPI(DocumentInfo doc, XWikiContext context)
    {
        super(context);
        this.doc = doc;
    }

    public DocumentInfo getDocInfo()
    {
        if (hasProgrammingRights())
            return doc;
        return null;
    }

    public boolean isNew()
    {
        return doc.isNew();
    }

    public int getFileType() {
        return doc.getFileType();
    }

    public void setFileType(int fileType) {
        doc.setFileType(fileType);
    }

    public String getFullName()
    {
        return (doc.getFullName());
    }

    public int isInstallable()
    {
        return doc.isInstallable();
    }

    public int getAction() {
        return doc.getAction();
    }

   public void setAction(int action) {
        doc.setAction(action);
    }

    public int testInstall(XWikiContext context)
    {
        return doc.testInstall(context);
    }

    public static String installStatusToString(int status)
    {
        return DocumentInfo.installStatusToString(status);
    }

    public static String actionToString(int status)
    {
        return DocumentInfo.actionToString(status);
    }

    public static int actionToInt(String status)
    {
        return DocumentInfo.actionToInt(status);
    }
}
