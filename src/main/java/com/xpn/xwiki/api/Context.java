/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

import java.util.List;

public class Context extends Api
{
    public Context(XWikiContext context)
    {
        super(context);
    }

    /**
     * @return an object which contains the Request context
     */
    public XWikiRequest getRequest()
    {
        return getXWikiContext().getRequest();
    }

    /**
     * @return an object which contains the response object
     */
    public XWikiResponse getResponse()
    {
        return getXWikiContext().getResponse();
    }

    public int getMode()
    {
        return getXWikiContext().getMode();
    }

    /**
     * @return the current database name
     */
    public String getDatabase()
    {
        return getXWikiContext().getDatabase();
    }

    /**
     * @return the original database
     */
    public String getOriginalDatabase()
    {
        return getXWikiContext().getOriginalDatabase();
    }

    /**
     * set the database if you have the programming right
     *
     * @param database the data name
     */
    public void setDatabase(String database)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().setDatabase(database);
        }
    }

    /**
     * @return the url Factory
     */
    public XWikiURLFactory getURLFactory()
    {
        return getXWikiContext().getURLFactory();
    }

    /**
     * @return true if the server is in virtual mode (ie host more than one wiki)
     */
    public boolean isVirtual()
    {
        return getXWikiContext().isVirtual();
    }

    /**
     * @return the requested action
     */
    public String getAction()
    {
        return getXWikiContext().getAction();
    }

    /**
     * @return the language of the current user
     */
    public String getLanguage()
    {
        return getXWikiContext().getLanguage();
    }

    /**
     * @return the interface language preference of the current user
     */
    public String getInterfaceLanguage()
    {
        return getXWikiContext().getInterfaceLanguage();
    }

    /**
     * @return the XWiki object if you have the programming right
     */
    public com.xpn.xwiki.XWiki getXWiki()
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().getWiki();
        } else {
            return null;
        }
    }

    /**
     * @return the current requested document
     */
    public XWikiDocument getDoc()
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().getDoc();
        } else {
            return null;
        }
    }

    /**
     * @return the current user which made the request
     */
    public String getUser()
    {
        return getXWikiContext().getUser();
    }

    /**
     * @return the local username of the current user which made the request
     */
    public String getLocalUser()
    {
        return getXWikiContext().getLocalUser();
    }

    /**
     * set the document if you have the programming right
     */
    public void setDoc(XWikiDocument doc)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().setDoc(doc);
        }
    }

    /**
     * @return the unwrapped version of the context if you have the programming right
     */
    public XWikiContext getContext()
    {
        if (hasProgrammingRights()) {
            return super.getXWikiContext();
        } else {
            return null;
        }
    }

    protected XWikiContext getProtectedContext()
    {
        return getXWikiContext();
    }

    public java.lang.Object get(String key)
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().get(key);
        } else {
            return null;
        }
    }

    public java.lang.Object getEditorWysiwyg()
    {
        return getXWikiContext().getEditorWysiwyg();
    }

    public void put(String key, java.lang.Object value)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().put(key, value);
        }
    }

    public void setFinished(boolean finished)
    {
        getXWikiContext().setFinished(finished);
    }

    /**
     * @return the cache duration
     */
    public int getCacheDuration()
    {
        return getXWikiContext().getCacheDuration();
    }

    /**
     * @param duration in second
     */
    public void setCacheDuration(int duration)
    {
        getXWikiContext().setCacheDuration(duration);
    }

    public void setLinksAction(String action)
    {
        getXWikiContext().setLinksAction(action);
    }

    public void unsetLinksAction()
    {
        getXWikiContext().unsetLinksAction();
    }

    public String getLinksAction()
    {
        return getXWikiContext().getLinksAction();
    }

    public void setLinksQueryString(String value)
    {
        getXWikiContext().setLinksQueryString(value);
    }

    public void unsetLinksQueryString()
    {
        getXWikiContext().unsetLinksQueryString();
    }

    public String getLinksQueryString()
    {
        return getXWikiContext().getLinksQueryString();
    }

    public XWikiValidationStatus getValidationStatus()
    {
        return getXWikiContext().getValidationStatus();
    }

    public List getDisplayedFields()
    {
        return getXWikiContext().getDisplayedFields();
    }
    public Util getUtil()
    {
        return context.getUtil();
    }
}
