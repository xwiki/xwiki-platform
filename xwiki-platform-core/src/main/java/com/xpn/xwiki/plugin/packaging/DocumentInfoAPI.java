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

package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class DocumentInfoAPI extends Api
{
    private DocumentInfo doc;

    public DocumentInfoAPI(DocumentInfo doc, XWikiContext context)
    {
        super(context);
        this.doc = doc;
    }

    public DocumentInfo getDocInfo()
    {
        if (hasProgrammingRights()) {
            return doc;
        }
        return null;
    }

    public boolean isNew()
    {
        return doc.isNew();
    }

    public int getFileType()
    {
        return doc.getFileType();
    }

    public void setFileType(int fileType)
    {
        doc.setFileType(fileType);
    }

    public String getFullName()
    {
        return doc.getFullName();
    }

    /**
     * @return the language of the described document
     * 
     * @since 2.2M1
     */
    public String getLanguage()
    {
        return doc.getLanguage();
    }
    
    public int isInstallable()
    {
        return doc.isInstallable();
    }

    public int getAction()
    {
        return doc.getAction();
    }

    public void setAction(int action)
    {
        doc.setAction(action);
    }

    public int testInstall(XWikiContext context)
    {
        return doc.testInstall(false, context);
    }

    public int testInstall(boolean isAdmin, XWikiContext context)
    {
        return doc.testInstall(isAdmin, context);
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
