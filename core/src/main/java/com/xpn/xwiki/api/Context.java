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
 * @author ludovic
 */
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
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
        return getContext().getRequest();
    }

    /**
     * @return an object which contains the response object
     */
    public XWikiResponse getResponse()
    {
        return getContext().getResponse();
    }

    public int getMode()
    {
        return getContext().getMode();
    }

    /**
     * @return the current database name
     */
    public String getDatabase()
    {
        return getContext().getDatabase();
    }

    /**
     * @return the original database
     */
    public String getOriginalDatabase()
    {
        return getContext().getOriginalDatabase();
    }

    /**
     * set the database if you have the programming right
     *
     * @param database the data name
     */
    public void setDatabase(String database)
    {
        if (hasProgrammingRights()) {
            getContext().setDatabase(database);
        }
    }

    /**
     * @return the url Factory
     */
    public XWikiURLFactory getURLFactory()
    {
        return getContext().getURLFactory();
    }

    /**
     * @return true if the server is in virtual mode (ie host more than one wiki)
     */
    public boolean isVirtual()
    {
        return getContext().isVirtual();
    }

    /**
     * @return the requested action
     */
    public String getAction()
    {
        return getContext().getAction();
    }

    /**
     * @return the language of the current user
     */
    public String getLanguage()
    {
        return getContext().getLanguage();
    }

    /**
     * @return the interface language preference of the current user
     */
    public String getInterfaceLanguage()
    {
        return getContext().getInterfaceLanguage();
    }

    /**
     * @return the XWiki object if you have the programming right
     */
    public com.xpn.xwiki.XWiki getXWiki()
    {
        if (hasProgrammingRights()) {
            return getContext().getWiki();
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
            return getContext().getDoc();
        } else {
            return null;
        }
    }

    /**
     * @return the current user which made the request
     */
    public String getUser()
    {
        return getContext().getUser();
    }

    /**
     * @return the local username of the current user which made the request
     */
    public String getLocalUser()
    {
        return getContext().getLocalUser();
    }

    /**
     * set the document if you have the programming right
     */
    public void setDoc(XWikiDocument doc)
    {
        if (hasProgrammingRights()) {
            getContext().setDoc(doc);
        }
    }

    /**
     * @return the unwrapped version of the context if you have the programming right
     */
    public XWikiContext getContext()
    {
        if (hasProgrammingRights()) {
            return getContext();
        } else {
            return null;
        }
    }

    protected XWikiContext getProtectedContext()
    {
        return getContext();
    }

    public java.lang.Object get(String key)
    {
        if (hasProgrammingRights()) {
            return getContext().get(key);
        } else {
            return null;
        }
    }

    public java.lang.Object getEditorWysiwyg()
    {
        return getContext().getEditorWysiwyg();
    }

    public void put(String key, java.lang.Object value)
    {
        if (hasProgrammingRights()) {
            getContext().put(key, value);
        }
    }

    public void setFinished(boolean finished)
    {
        getContext().setFinished(finished);
    }

    /**
     * @return the cache duration
     */
    public int getCacheDuration()
    {
        return getContext().getCacheDuration();
    }

    /**
     * @param duration in second
     */
    public void setCacheDuration(int duration)
    {
        getContext().setCacheDuration(duration);
    }

    public void setLinksAction(String action)
    {
        getContext().setLinksAction(action);
    }

    public void unsetLinksAction()
    {
        getContext().unsetLinksAction();
    }

    public String getLinksAction()
    {
        return getContext().getLinksAction();
    }

    public void setLinksQueryString(String value)
    {
        getContext().setLinksQueryString(value);
    }

    public void unsetLinksQueryString()
    {
        getContext().unsetLinksQueryString();
    }

    public String getLinksQueryString()
    {
        return getContext().getLinksQueryString();
    }

    public XWikiValidationStatus getValidationStatus()
    {
        return getContext().getValidationStatus();
    }

    public List getDisplayedFields()
    {
        return getContext().getDisplayedFields();
    }
}
