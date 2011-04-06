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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class DocumentInfo
{
    private static final Log log = LogFactory.getLog(DocumentInfo.class);

    private XWikiDocument doc;

    private int installable = INSTALL_IMPOSSIBLE;

    private int action = ACTION_NOT_DEFINED;

    private int fileType;

    public final static int TYPE_SAMPLE = 0;

    public final static int TYPE_NORMAL = 1;

    public final static int ACTION_NOT_DEFINED = -1;

    public final static int ACTION_OVERWRITE = 0;

    public final static int ACTION_SKIP = 1;

    public final static int ACTION_MERGE = 2;

    public final static int INSTALL_IMPOSSIBLE = 0;

    public final static int INSTALL_ALREADY_EXIST = 1;

    public final static int INSTALL_OK = 2;

    public final static int INSTALL_ERROR = 4;

    public DocumentInfo(XWikiDocument doc)
    {
        this.doc = doc;
    }

    public XWikiDocument getDoc()
    {
        return doc;
    }

    public boolean isNew()
    {
        return doc.isNew();
    }

    public void changeSpace(String Space)
    {
        if (doc.getSpace().compareTo("XWiki") != 0) {
            return;
        }
        doc.setSpace(Space);
        installable = INSTALL_IMPOSSIBLE;
    }

    public int getFileType()
    {
        return fileType;
    }

    public void setFileType(int fileType)
    {
        this.fileType = fileType;
    }

    public String getFullName()
    {
        return (doc.getFullName());
    }

    public String getLanguage()
    {
        return (doc.getLanguage());
    }

    public int isInstallable()
    {
        return installable;
    }

    public int testInstall(boolean isAdmin, XWikiContext context)
    {
        if (log.isDebugEnabled()) {
            log.debug("Package test install document " + ((doc == null) ? "" : getFullName())
                + " " + ((doc == null) ? "" : getLanguage()));
        }

        installable = INSTALL_IMPOSSIBLE;

        try {
            if (this.doc == null) {
                return installable;
            }
            try {
                if ((!isAdmin) && (!context.getWiki().checkAccess("edit", this.doc, context))) {
                    return installable;
                }
                XWikiDocument doc1 = context.getWiki().getDocument(doc.getFullName(), context);
                boolean isNew = doc1.isNew();
                if (!isNew) {
                    if ((doc.getLanguage() != null) && (!doc.getLanguage().equals(""))) {
                        isNew = !doc1.getTranslationList(context).contains(doc.getLanguage());
                    }
                }

                if (!isNew) {
                    installable = INSTALL_ALREADY_EXIST;
                    return installable;
                }
            } catch (XWikiException e) {
                installable = INSTALL_IMPOSSIBLE;
                return installable;
            }
            installable = INSTALL_OK;
            return installable;
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Package test install document " + ((doc == null) ? "" : getFullName())
                    + " " + ((doc == null) ? "" : getLanguage()) + " result " + installable);
            }
        }
    }

    public static String installStatusToString(int status)
    {
        if (status == INSTALL_IMPOSSIBLE) {
            return "Impossible";
        } else if (status == INSTALL_ERROR) {
            return "Error";
        } else if (status == INSTALL_OK) {
            return "Ok";
        } else if (status == INSTALL_ALREADY_EXIST) {
            return "Already exist";
        }
        return "Unknown Status";
    }

    public static String actionToString(int status)
    {
        if (status == ACTION_MERGE) {
            return "merge";
        } else if (status == ACTION_OVERWRITE) {
            return "overwrite";
        } else if (status == ACTION_SKIP) {
            return "skip";
        }
        return "Not defined";
    }

    public static int actionToInt(String status)
    {
        if (status.compareTo("merge") == 0) {
            return ACTION_MERGE;
        } else if (status.compareTo("overwrite") == 0) {
            return ACTION_OVERWRITE;
        } else if (status.compareTo("skip") == 0) {
            return ACTION_SKIP;
        }
        return ACTION_NOT_DEFINED;
    }

    public int getAction()
    {
        return action;
    }

    public void setAction(int action)
    {
        this.action = action;
    }

    public void setDoc(XWikiDocument doc)
    {
        this.doc = doc;
    }
}
