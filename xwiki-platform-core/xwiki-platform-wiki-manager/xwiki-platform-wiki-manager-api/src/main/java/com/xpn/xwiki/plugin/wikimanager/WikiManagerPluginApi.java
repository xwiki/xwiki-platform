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
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.globalsearch.GlobalSearchPluginApi;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API for managing wikis (create wiki, delete wiki, create wiki from template, etc).
 * 
 * @version $Id$
 * @see com.xpn.xwiki.plugin.wikimanager.WikiManagerPlugin
 */
public class WikiManagerPluginApi extends PluginApi<WikiManagerPlugin>
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * Logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(WikiManagerPluginApi.class);

    /**
     * The default WikiManager managed exception.
     */
    private XWikiExceptionApi defaultException;

    /**
     * API tool to be able to make and merge multi wikis search queries.
     */
    private GlobalSearchPluginApi searchApi;

    /**
     * Protected API for managing wikis (create wiki, delete wiki, create wiki from template, etc).
     */
    private WikiManager wikiManager;

    /**
     * The plugin internationalization service.
     */
    private XWikiPluginMessageTool messageTool;

    /**
     * Create an instance of the Wiki Manager plugin user api.
     * 
     * @param plugin the entry point of the Wiki Manager plugin.
     * @param context the XWiki context.
     */
    public WikiManagerPluginApi(WikiManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        this.defaultException = new XWikiExceptionApi(WikiManagerException.getDefaultException(), this.context);

        // Message Tool
        Locale locale = (Locale) context.get("locale");
        this.messageTool = new WikiManagerMessageTool(locale, plugin, context);
        context.put(WikiManagerMessageTool.MESSAGETOOL_CONTEXT_KEY, this.messageTool);

        this.searchApi = plugin.getGlobalSearchApiPlugin(context);
        this.wikiManager = new WikiManager(this.messageTool);
    }

    /**
     * @return the default plugin api exception.
     */
    public XWikiExceptionApi getDefaultException()
    {
        return this.defaultException;
    }

    /**
     * @return the plugin internationalization service.
     */
    public XWikiMessageTool getMessageTool()
    {
        return this.messageTool;
    }

    /**
     * @return the GlobalSearch plugin api.
     */
    public GlobalSearchPluginApi getSearchApi()
    {
        return this.searchApi;
    }

    /**
     * Log error and store details in the context.
     * 
     * @param errorMessage error message.
     * @param e the catched exception.
     */
    private void error(String errorMessage, XWikiException e)
    {
        LOGGER.debug(errorMessage, e);

        this.context.put(CONTEXT_LASTERRORCODE, Integer.valueOf(e.getCode()));
        this.context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, this.context));
    }

    /**
     * @return true if the it's possible to create a wiki in this context
     */
    public boolean canCreateWiki()
    {
        return this.wikiManager.canCreateWiki(this.context);
    }

    /**
     * @return true if the it's possible to edit a wiki descriptor in this context
     */
    public boolean canEditWiki()
    {
        return this.wikiManager.canDeleteWiki(this.context);
    }

    /**
     * @return true if the it's possible to delete a wiki in this context
     */
    public boolean canDeleteWiki()
    {
        return this.wikiManager.canEditWiki(this.context);
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Wikis management

    /**
     * Create a new wiki from template.
     * 
     * @param wikiName the name of the new wiki.
     * @param templateWiki the name of the wiki from where to copy document to the new wiki.
     * @param pkgName the name of the attached XAR file to import in the new wiki.
     * @param wikiXObjectDocument a wiki descriptor document from which the new wiki descriptor document will be
     *            created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *         <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to create
     *         new wiki.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database failed.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int createNewWiki(String wikiName, String templateWiki, String pkgName, XWikiServer wikiXObjectDocument,
        boolean failOnExist) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canCreateWiki()) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, this.messageTool.get(
                    WikiManagerMessageTool.ERROR_RIGHTTOCREATEWIKI, wikiName));
            }

            if (wikiName == null || wikiName.trim().equals("")) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKINAMEFORBIDDEN, messageTool.get(
                    WikiManagerMessageTool.ERROR_WIKINAMEFORBIDDEN, wikiName));
            }

            wikiXObjectDocument.setWikiName(wikiName);

            String realTemplateWikiName =
                templateWiki == null || templateWiki.trim().length() == 0 ? null : templateWiki;

            String realPkgName = pkgName == null || pkgName.trim().length() == 0 ? null : pkgName;

            String comment = WikiManagerMessageTool.COMMENT_CREATEEMPTYWIKI;

            this.wikiManager.createNewWiki(wikiXObjectDocument, failOnExist, realTemplateWikiName, realPkgName,
                comment, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKICREATION, wikiXObjectDocument.toString()), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Delete wiki descriptor document and wiki's database.
     * 
     * @param wikiName the name of the wiki to delete.
     * @param deleteDatabase if true wiki's database is also removed.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         <li>{@link XWikiException#ERROR_XWIKI_ACCESS_DENIED}: you don't have right to delete wiki.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     * @since 1.1
     */
    public int deleteWiki(String wikiName, boolean deleteDatabase) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canDeleteWiki()) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, this.messageTool.get(
                    WikiManagerMessageTool.ERROR_RIGHTTODELETEWIKI, wikiName));
            }

            this.wikiManager.deleteWiki(wikiName, deleteDatabase, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIDELETION, wikiName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Delete wiki descriptor document and wiki's database.
     * 
     * @param wikiName the name of the wiki to delete.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         <li>{@link XWikiException#ERROR_XWIKI_ACCESS_DENIED}: you don't have right to delete wiki.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int deleteWiki(String wikiName) throws XWikiException
    {
        return deleteWiki(wikiName, true);
    }

    /**
     * Delete wiki descriptor document from database.
     * 
     * @param wikiName the name of the wiki to delete.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         <li>{@link XWikiException#ERROR_XWIKI_ACCESS_DENIED}: you don't have right to delete wiki.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     * @deprecated Use {@link #deleteWikiAlias(String, int)} since 1.1.
     */
    @Deprecated
    public int deleteWiki(String wikiName, int objectId) throws XWikiException
    {
        return deleteWikiAlias(wikiName, objectId);
    }

    /**
     * Delete wiki descriptor alias document from database.
     * 
     * @param wikiName the name of the wiki to delete.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         <li>{@link XWikiException#ERROR_XWIKI_ACCESS_DENIED}: you don't have right to delete wiki.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     * @since 1.1
     */
    public int deleteWikiAlias(String wikiName, int objectId) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            if (!canEditWiki()) {
                throw new WikiManagerException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, this.messageTool.get(
                    WikiManagerMessageTool.ERROR_RIGHTTODELETEWIKI, wikiName));
            }

            this.wikiManager.deleteWikiAlias(wikiName, objectId, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIDELETION, wikiName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Get {@link Wiki} with provided name.
     * 
     * @param wikiName the name of the wiki.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when getting document from wiki name.
     */
    public Wiki getWikiFromName(String wikiName) throws XWikiException
    {
        Wiki doc = null;

        try {
            doc = this.wikiManager.getWikiFromName(wikiName, context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIGET, wikiName), e);
        }

        return doc;
    }

    /**
     * @return the list of all {@link Wiki}.
     * @throws XWikiException error when getting wiki documents descriptors.
     */
    public List<Wiki> getAllWikis() throws XWikiException
    {
        List<Wiki> wikiList = Collections.emptyList();

        try {
            wikiList = this.wikiManager.getAllWikis(context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIGETALL), e);
        }

        return wikiList;
    }

    /**
     * Get {@link Wiki} described by document with provided full name.
     * 
     * @param documentFullName the full name of the wiki document descriptor.
     * @return the {@link Wiki} object.
     * @throws XWikiException error when getting document.
     */
    public Wiki getWikiFromDocumentName(String documentFullName) throws XWikiException
    {
        Wiki doc = null;

        try {
            doc = this.wikiManager.getWikiFromDocumentName(documentFullName, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIGET, documentFullName), e);
        }

        return doc;
    }

    /**
     * Get wiki descriptor document corresponding to provided wiki name.
     * 
     * @param wikiName the name of the wiki.
     * @return null if there is an error and add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public XWikiServer getWikiDocument(String wikiName) throws XWikiException
    {
        return getWikiDocument(wikiName, 0);
    }

    /**
     * Get wiki descriptor document corresponding to provided wiki name.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @return null if there is an error and add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public XWikiServer getWikiDocument(String wikiName, int objectId) throws XWikiException
    {
        XWikiServer doc = null;

        try {
            doc = this.wikiManager.getWikiAlias(wikiName, objectId, true, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIALIASGET, wikiName), e);
        }

        return doc;
    }

    /**
     * Get the list of all wiki descriptor documents.
     * 
     * @return the list {@link XWikiServer}.
     * @throws XWikiException critical error in xwiki engine.
     */
    public List<XWikiServer> getWikiDocumentList() throws XWikiException
    {
        List<XWikiServer> listDocument = Collections.emptyList();

        try {
            listDocument = this.wikiManager.getWikiAliasList(this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKIALIASGETALL), e);
        }

        return listDocument;
    }

    /**
     * Create an empty not saved {@link XWikiServer}.
     * 
     * @return an empty not saved {@link XWikiServer}.
     * @throws XWikiException critical error in xwiki engine.
     */
    public XWikiServer createWikiDocument() throws XWikiException
    {
        return XWikiServerClass.getInstance(this.context).newXObjectDocument(this.context);
    }

    /**
     * Check if a Server of the given name exists in the master Wiki by checking if the "XWiki.XWikiServer{serverName}"
     * document is new.
     * 
     * @param wikiName the name of the server to be checked
     * @return true if server exists, false otherwise
     */
    public boolean isWikiExist(String wikiName)
    {
        return isWikiExist(wikiName, 0);
    }

    /**
     * Check if a Server of the given name exists in the master Wiki by checking if the "XWiki.XWikiServer{serverName}"
     * document is new.
     * 
     * @param wikiName the name of the server to be checked
     * @param objectId the id of the XWiki object included in the document to manage.
     * @return true if server exists, false otherwise
     */
    public boolean isWikiExist(String wikiName, int objectId)
    {
        return this.wikiManager.isWikiAliasExist(wikiName, objectId, this.context);
    }

    /**
     * Indicate if the provided wiki name could be used to create a new wiki.
     * 
     * @param wikiName the name of the wiki.
     * @return true if the name is already used, false otherwise.
     * @throws XWikiException error when trying to find an existing database/schema by name.
     */
    public boolean isWikiNameAvailable(String wikiName) throws XWikiException
    {
        return this.context.getWiki().getStore().isWikiNameAvailable(wikiName, this.context);
    }

    /**
     * Change the {@link XWikiServerClass} "visibility" field of a wiki descriptor document.
     * 
     * @param wikiName the name of the wiki descriptor.
     * @param visibility the new value of "visibility" field. Can be "public", "private" or "template".
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int setWikiVisibility(String wikiName, String visibility) throws XWikiException
    {
        return setWikiVisibility(wikiName, 0, visibility);
    }

    /**
     * Change the {@link XWikiServerClass} "visibility" field of a wiki descriptor document.
     * 
     * @param wikiName the name of the wiki descriptor.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param visibility the new value of "visibility" field. Can be "public", "private" or "template".
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int setWikiVisibility(String wikiName, int objectId, String visibility) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            XWikiServer wikiAlias = this.wikiManager.getWikiAlias(wikiName, objectId, true, this.context);
            wikiAlias.setVisibility(visibility);
            wikiAlias.save();
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKISETVISIBILITY, wikiName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Template management

    /**
     * Change the {@link XWikiServerClass} "visibility" field of a wiki descriptor document.
     * 
     * @param wikiName the name of the wiki descriptor.
     * @param isWikiTemplate true if it's a wiki template, false otherwise.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int setIsWikiTemplate(String wikiName, boolean isWikiTemplate) throws XWikiException
    {
        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        try {
            Wiki wiki = this.wikiManager.getWikiFromName(wikiName, this.context);
            XWikiServer wikiAlias = wiki.getFirstWikiAlias();
            wikiAlias.setIsWikiTemplate(isWikiTemplate);
            wikiAlias.save();
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKISETVISIBILITY, wikiName), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Create a new xiki with {@link XWikiServerClass} "visibility" field set to "template".
     * 
     * @param templateName the name of the new wiki template to create.
     * @param templateDescription the description of the new wiki template to create.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @return If there is error, it add error code in context {@link #CONTEXT_LASTERRORCODE} field and exception in
     *         context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *         <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to create
     *         new wiki.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database failed.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public int createWikiTemplate(String templateName, String templateDescription, String packageName)
        throws XWikiException
    {
        if (!hasAdminRights()) {
            return XWikiException.ERROR_XWIKI_ACCESS_DENIED;
        }

        int returncode = XWikiExceptionApi.ERROR_NOERROR;

        XWikiServer wikiXObjectDocument = XWikiServerClass.getInstance(this.context).newXObjectDocument(this.context);
        wikiXObjectDocument.setWikiName(templateName);
        wikiXObjectDocument.setDescription(templateDescription);

        wikiXObjectDocument.setServer(templateName + ".template.local");

        wikiXObjectDocument.setState(XWikiServerClass.FIELDL_STATE_ACTIVE);
        wikiXObjectDocument.setOwner(this.context.getUser());

        try {
            String[] params = new String[] {templateName, packageName};
            String message = this.messageTool.get(WikiManagerMessageTool.COMMENT_CREATEWIKITEMPLATE, params);
            this.wikiManager.createWikiTemplate(wikiXObjectDocument, packageName, message, this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKICREATION, wikiXObjectDocument.toString()), e);

            returncode = e.getCode();
        }

        return returncode;
    }

    /**
     * Get wiki descriptor document corresponding to provided wiki name with {@link XWikiServerClass} "visibility" field
     * set to "template".
     * 
     * @param wikiName the name of the wiki template.
     * @return null if there is an error and add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public XWikiServer getWikiTemplateDocument(String wikiName) throws XWikiException
    {
        return getWikiTemplateDocument(wikiName, 0);
    }

    /**
     * Get wiki descriptor document corresponding to provided wiki name with {@link XWikiServerClass} "visibility" field
     * set to "template".
     * 
     * @param wikiName the name of the wiki template.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @return null if there is an error and add error code in context {@link #CONTEXT_LASTERRORCODE} field and
     *         exception in context's {@link #CONTEXT_LASTEXCEPTION} field.
     *         <p>
     *         Error codes can be :
     *         <ul>
     *         <li>{@link XWikiExceptionApi#ERROR_NOERROR}: methods succeed.</li>
     *         <li>{@link WikiManagerException#ERROR_WM_WIKIDOESNOTEXISTS}: wiki to delete does not exists.</li>
     *         </ul>
     * @throws XWikiException critical error in xwiki engine.
     */
    public XWikiServer getWikiTemplateDocument(String wikiName, int objectId) throws XWikiException
    {
        XWikiServer doc = null;

        try {
            doc = this.wikiManager.getWikiTemplateAlias(wikiName, objectId, this.context, true);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKITEMPLATEGET, wikiName), e);
        }

        return doc;
    }

    /**
     * @return all the template wiki. Wiki with "visibility" field equals to "template".
     * @throws XWikiException critical error in xwiki engine.
     */
    public List<XWikiServer> getWikiTemplateList() throws XWikiException
    {
        List<XWikiServer> listDocument = Collections.emptyList();

        try {
            listDocument = this.wikiManager.getWikiTemplateAliasList(this.context);
        } catch (WikiManagerException e) {
            error(this.messageTool.get(WikiManagerMessageTool.LOG_WIKITEMPLATEGETALL), e);
        }

        return listDocument;
    }
}
