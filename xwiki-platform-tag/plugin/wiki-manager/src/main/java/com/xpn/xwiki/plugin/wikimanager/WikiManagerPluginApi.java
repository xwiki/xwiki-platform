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

package com.xpn.xwiki.plugin.wikimanager;

import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * API for managing multiwikis
 * 
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.wikimanager.WikiManagerPlugin
 */
public class WikiManagerPluginApi extends PluginApi
{
    protected static final Log LOG = LogFactory.getLog(WikiManagerPluginApi.class);

    private XWikiExceptionApi defaultException;

    public WikiManagerPluginApi(WikiManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        this.defaultException =
            new XWikiExceptionApi(WikiManagerException.getDefaultException(), this.context);
    }

    public XWikiExceptionApi getDefaultException()
    {
        return this.defaultException;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Wikis management

    public int createNewWiki(String wikiname, String templateWiki, XWikiServer wikiSuperDocument,
        boolean failOnExist) throws XWikiException
    {
        return createNewWiki(wikiname, templateWiki, null, wikiSuperDocument, failOnExist);
    }

    public int createNewWiki(String wikiname, String templateWiki, String pkg,
        XWikiServer wikiSuperDocument, boolean failOnExist) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        wikiSuperDocument.setWikiName(wikiname);

        // Some initializations dues to Velocity
        if ("".equals(templateWiki))
            templateWiki = null;

        try {
            if (templateWiki != null)
                WikiManager.getInstance().createNewWikiFromTemplate(wikiSuperDocument, templateWiki, failOnExist, this.context);
            else if (pkg != null)
                WikiManager.getInstance().createNewWikiFromPackage(wikiSuperDocument, pkg, failOnExist, this.context);
            else
                WikiManager.getInstance().createNewWiki(wikiSuperDocument, failOnExist, this.context);
        } catch (WikiManagerException e) {
            LOG.error("Try to create wiki \"" + wikiSuperDocument + "\"", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));

            returncode = e.getCode();
        }

        return returncode;
    }

    public int deleteWiki(String wikiName) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            WikiManager.getInstance().deleteWiki(wikiName, this.context);
        } catch (WikiManagerException e) {
            LOG.error("Try to delete wiki \"" + wikiName + "\"", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));

            returncode = e.getCode();
        }

        return returncode;
    }

    public XWikiServer getWikiDocument(String wikiName) throws XWikiException
    {
        XWikiServer doc = null;

        try {
            doc =
                WikiManager.getInstance().getWiki(wikiName, this.context, true);
        } catch (WikiManagerException e) {
            LOG.error("Try to get wiki \"" + wikiName + "\" document", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));
        }

        return doc;
    }

    /**
     * Get the list of wiki associated to a given username
     * 
     * @param username the name of the user that own the wikis to be retrieved
     * @return the list of wikis owned by the user
     * @throws XWikiException
     */
    public List getWikiDocumentList(String username) throws XWikiException
    {
        List listDocument = new ArrayList();

        try {
            listDocument = WikiManager.getInstance().getWikiList(/*username, */this.context);
        } catch (WikiManagerException e) {
            if (username != null)
                LOG.error("Try to get wikis documents for user \"" + username + "\"", e);
            else
                LOG.error("Try to get all wikis documents", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));
        }

        return listDocument;
    }

    /**
     * Get all wikis
     * 
     * @return a list of all the wikis
     * @throws XWikiException
     */
    public List getWikiDocumentList() throws XWikiException
    {
        return getWikiDocumentList(null);
    }

    /**
     * Create empty wiki document
     * 
     * @return Document Empty wiki document
     * @throws XWikiException
     */
    public XWikiServer createWikiDocument() throws XWikiException
    {
        return (XWikiServer)XWikiServerClass.getInstance(context).newSuperDocument(context);
    }

    /**
     * Check if a Server of the given name exists in the master Wiki by checking if the
     * "XWiki.XWikiServer{serverName}" document is new
     * 
     * @param wikiName the name of the server to be checked
     * @return true if server exists, false otherwise
     */
    public boolean isWikiExist(String wikiName)
    {
        return WikiManager.getInstance().isWikiExist(wikiName, this.context);
    }

    public int setWikiVisibility(String wikiName, String visibility) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            XWikiServer wikiDoc = WikiManager.getInstance().getWiki(wikiName, this.context, true);
            wikiDoc.setVisibility(visibility);
            wikiDoc.save();
        } catch (WikiManagerException e) {
            LOG.error("Try to set wiki visibility \"" + wikiName + "\"", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));

            returncode = e.getCode();
        }

        return returncode;
    }

    public String getWikiName(XWikiServer document) throws XWikiException
    {
        String wikiName = null;

        try {
            wikiName =
                XWikiServerClass.getInstance(this.context).getItemDefaultName(
                    document.getFullName(), this.context);
        } catch (WikiManagerException e) {
            LOG.error("Try to get wiki name \"" + document.getFullName() + "\"", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));
        }

        return wikiName;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Template management

    public int createWikiTemplate(String templateName, String templateDescription,
        String packageName) throws XWikiException
    {
        if (!hasAdminRights())
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        XWikiServer wikiSuperDocument = (XWikiServer)XWikiServerClass.getInstance(context).newSuperDocument(context);
        wikiSuperDocument.setWikiName(templateName);
        wikiSuperDocument.setDescription(templateDescription);

        wikiSuperDocument.setServer(templateName + ".template.local");

        wikiSuperDocument.setState(XWikiServerClass.FIELDL_state_active);
        wikiSuperDocument.setOwner(this.context.getUser());

        try {
            WikiManager.getInstance().createWikiTemplate(wikiSuperDocument, packageName,
                this.context);
        } catch (WikiManagerException e) {
            LOG.error("Try to create wiki template \"" + wikiSuperDocument + "\"", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));

            returncode = e.getCode();
        }

        return returncode;
    }

    public XWikiServer getWikiTemplateDocument(String wikiName) throws XWikiException
    {
        XWikiServer doc = null;

        try {
            doc =
                WikiManager.getInstance().getWikiTemplate(wikiName, this.context, true);
        } catch (WikiManagerException e) {
            LOG.error("Try to get wiki \"" + wikiName + "\" document", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));
        }

        return doc;
    }

    public List getWikiTemplateList() throws XWikiException
    {
        List listDocument = new ArrayList();

        try {
            List listXWikiDocument = WikiManager.getInstance().getWikiTemplateList(this.context);
            
            for (Iterator it = listXWikiDocument.iterator(); it.hasNext();) {
                XWikiDocument doc = (XWikiDocument) it.next();
                listDocument.add(doc.newDocument(this.context));
            }
        } catch (WikiManagerException e) {
            LOG.error("Try to get wikis templates", e);

            this.context.put("lasterrorcode", new Integer(e.getCode()));
            this.context.put("lastexception", new XWikiExceptionApi(e, this.context));
        }

        return listDocument;
    }
}
