package org.xwiki.contrib.wiki30.internal;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerMessageTool;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServerClass;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

public class LocalWikiManager
{
    protected static final Log LOG = LogFactory.getLog(LocalWikiManager.class);

    /**
     * Get the {@link XWikiPluginMessageTool} to use with WikiManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    private static XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        return WikiManagerMessageTool.getDefault(context);
    }

    /**
     * Encapsulate {@link com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)} adding wiki switch.
     * 
     * @param wikiName the name of the wiki where to get the document.
     * @param fullname the full name of the document to get.
     * @param context the XWiki context.
     * @return the document with full name equals to <code>fullname</code> and wiki <code>wikiName</code>. If it does
     *         not exist return new XWikiDocument.
     * @throws XWikiException error when calling {@link XWiki#getDocument(String, XWikiContext)} .
     * @see com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)
     */
    private static XWikiDocument getDocument(String wikiName, String fullname, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            return context.getWiki().getDocument(fullname, context);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Update or create new wiki description document without saving it.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param context the XWiki context.
     * @return the new wiki descriptor document to save.
     * @throws XWikiException <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             </ul>
     */
    private static XWikiServer getWikiDescriptorToSave(XWikiServer userWikiSuperDoc, boolean failOnExist, XWikiContext context)
        throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        XWikiServerClass wikiClass = XWikiServerClass.getInstance(context);

        XWikiServer wikiSuperDocToSave;

        // If modify existing document
        if (!userWikiSuperDoc.isFromCache()) {
            // Verify if server page already exist
            XWikiDocument docToSave = getDocument(context.getMainXWiki(), userWikiSuperDoc.getFullName(), context);

            if (!docToSave.isNew() && wikiClass.isInstance(docToSave)) {
                // If we are not allowed to continue in case wiki descriptor page already
                // exists.
                if (failOnExist) {
                    throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIALREADYEXISTS, msg.get(
                        WikiManagerMessageTool.ERROR_DESCRIPTORALREADYEXISTS, userWikiSuperDoc.getFullName()));
                } else if (LOG.isWarnEnabled()) {
                    LOG.warn(msg.get(WikiManagerMessageTool.LOG_DESCRIPTORALREADYEXISTS, userWikiSuperDoc.toString()));
                }
            }

            wikiSuperDocToSave = wikiClass.newXObjectDocument(docToSave, 0, context);

            // clear entry in virtual wiki cache
            if (!wikiSuperDocToSave.getServer().equals(userWikiSuperDoc.getServer())) {
                xwiki.getVirtualWikiCache().remove(userWikiSuperDoc.getServer());
            }

            wikiSuperDocToSave.mergeObject(userWikiSuperDoc);
        } else {
            wikiSuperDocToSave = userWikiSuperDoc;
        }

        return wikiSuperDocToSave;
    }

    /**
     * Get the documents for which copied document content will be replace by an #includeInContext(SourceDocument) or
     * #includeTopic(SourceDocument) macro call.
     * 
     * @param wiki the name of the wiki where to find the list of documents.
     * @param context the XWiki context.
     * @return a pair of list of documents names to include and list of documents names to link.
     * @throws XWikiException error when getting Applications descriptors where searched documents are listed.
     */
    private static Collection<String>[] getDocsNames(String wiki, XWikiContext context) throws XWikiException
    {
        Collection<String>[] docsNames = new Collection[2];

        // Get applications manger
        ApplicationManagerPluginApi appmanager =
            (ApplicationManagerPluginApi) context.getWiki().getPluginApi(ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null) {
            return null;
        }

        // //////////////////////////////////
        // Get documents to include or link

        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null) {
                docsNames[0] = rootApp.getDocsNameToInclude(true);
                docsNames[1] = rootApp.getDocsNameToLink(true);
            } else {
                Collection<XWikiApplication> applications = appmanager.getApplicationDocumentList();
                docsNames[0] = XWikiApplication.getDocsNameToInclude(applications);
                docsNames[1] = XWikiApplication.getDocsNameToLink(applications);
            }
        } finally {
            context.setDatabase(database);
        }

        return docsNames;
    }

    /**
     * Create and init new database/schema.
     * 
     * @param targetWiki the name of the new database/schema.
     * @param context the Xwiki context.
     * @param initClasses indicate if standard classes should be initalized
     * @throws WikiManagerException <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     */
    private static void createWikiDatabase(String targetWiki, XWikiContext context, boolean initClasses)
        throws WikiManagerException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Create database/schema
        try {
            xwiki.getStore().createWiki(targetWiki, context);
        } catch (Exception e) {
            LOG.warn(msg.get(WikiManagerMessageTool.LOG_DATABASECREATIONEXCEPTION, targetWiki), e);
        }

        // Init database/schema
        try {
            xwiki.updateDatabase(targetWiki, true, initClasses, context);
        } catch (Exception e) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_UPDATEDATABASE, msg.get(
                WikiManagerMessageTool.ERROR_UPDATEDATABASE, targetWiki), e);
        }

        Utils.getComponent(ObservationManager.class).notify(new WikiCreatedEvent(targetWiki), targetWiki, context);
    }

    /**
     * Copy all documents from <code>sourceWiki</code> wiki to <code>targetWiki</code> wiki.
     * <p>
     * It also take care of ApplicationManager descriptors "documents to include" and "documents to link".
     * </p>
     * 
     * @param sourceWiki the wiki from where to copy documents and get lists of "document to link" and "documents to
     *            copy".
     * @param targetWiki the wiki where to copy documents.
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>copying on of the source wiki to target wiki.</li>
     *             <li>or getting documents to include.</li>
     *             <li>or getting documents to link.</li>
     *             </ul>
     */
    private static void copyWiki(String sourceWiki, String targetWiki, String comment, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        // Copy all the wiki
        xwiki.copyWiki(sourceWiki, targetWiki, null, context);

        String database = context.getDatabase();
        try {
            context.setDatabase(targetWiki);

            Collection<String>[] docsNames = getDocsNames(sourceWiki, context);

            if (docsNames != null) {
                Object[] includeFormatParams = new Object[] {sourceWiki, XObjectDocument.WIKI_SPACE_SEPARATOR, null};

                // Replace documents contents to include
                for (Object item : docsNames[0]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeInContext(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }

                // Replace documents contents to link
                for (Object item : docsNames[1]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeTopic(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }
            }
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Import XAR package into wiki.
     * 
     * @param packageName the name of the attached package file.
     * @param targetWiki the name of the wiki where to install loaded {@link XWikiDocument} from XAR package.
     * @param context the XWiki context.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database
     *             failed.</li>
     *             </ul>
     */
    private static void importPackage(String packageName, String targetWiki, XWikiContext context) throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        // Prepare to import
        XWikiDocument doc = context.getDoc();

        XWikiAttachment packFile = doc.getAttachment(packageName);

        if (packFile == null) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEDOESNOTEXISTS, msg.get(
                WikiManagerMessageTool.ERROR_PACKAGEDOESNOTEXISTS, packageName));
        }

        // Get packager plugin
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi("package", context));

        String database = context.getDatabase();

        try {
            context.setDatabase(targetWiki);

            // Import package
            try {
                importer.Import(packFile.getContent(context));
            } catch (IOException e) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEIMPORT, msg.get(
                    WikiManagerMessageTool.ERROR_PACKAGEIMPORT, packageName), e);
            }

            // Install imported documents
            if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_PACKAGEINSTALL, msg.get(
                    WikiManagerMessageTool.ERROR_PACKAGEINSTALL, packageName));
            }
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Create new wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param templateWikiName the name of the wiki from where to copy document to the new wiki.
     * @param packageName the name of the attached XAR file to import in the new wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user descriptor documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEDOESNOTEXISTS}: attached package does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEIMPORT}: package loading failed.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_PACKAGEINSTALL}: loaded package insertion into database
     *             failed.</li>
     *             </ul>
     */
    public static XWikiServer createNewWiki(XWikiServer userWikiSuperDoc, boolean failOnExist, String templateWikiName,
        String packageName, String comment, XWikiContext context) throws XWikiException
    {
        XWikiPluginMessageTool msg = getMessageTool(context);

        XWiki xwiki = context.getWiki();

        if (!xwiki.isVirtualMode()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_XWIKINOTVIRTUAL,
                msg.get(WikiManagerMessageTool.ERROR_XWIKINOTVIRTUAL));
        }

        String newWikiName = userWikiSuperDoc.getWikiName();

        String database = context.getDatabase();

        try {
            // Return to root database
            context.setDatabase(context.getMainXWiki());

            // Wiki name forbidden
            String wikiForbiddenList = xwiki.Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(newWikiName, wikiForbiddenList, ", ")) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKINAMEFORBIDDEN, msg.get(
                    WikiManagerMessageTool.ERROR_WIKINAMEFORBIDDEN, newWikiName));
            }

            // Update or create wiki descriptor document that will be saved.
            XWikiServer wikiSuperDocToSave = getWikiDescriptorToSave(userWikiSuperDoc, failOnExist, context);

            // Check owner
            if (getDocument(xwiki.getDatabase(), wikiSuperDocToSave.getOwner(), context).isNew()) {
                LOG.warn(msg.get(WikiManagerMessageTool.ERROR_USERDOESNOTEXIST, wikiSuperDocToSave.getOwner()));
                wikiSuperDocToSave.setOwner(XWikiRightService.SUPERADMIN_USER);
            }

            // Create wiki database/schema
            createWikiDatabase(newWikiName, context, templateWikiName == null);

            // Save new wiki descriptor document.
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // This is the only difference wrt to the original WikiManager class. We use the low level API so that all users 
            // are able to create workspaces (wikis)
            // TODO: Restrict this to users in a given group (i.e., people allowed to create workspaces)
            XWikiDocument xdoc = wikiSuperDocToSave.getDocument();
            context.getWiki().saveDocument(xdoc, context);
            
            // Copy template wiki into new wiki
            if (templateWikiName != null) {
                copyWiki(templateWikiName, newWikiName, comment, context);
            }

            // Import XAR package into new wiki
            if (packageName != null) {
                importPackage(packageName, newWikiName, context);
            }

            // Return to root database
            context.setDatabase(context.getMainXWiki());

            return wikiSuperDocToSave;
        } finally {
            context.setDatabase(database);
        }
    }
    
    /**
     * Create a new virtual wiki. The new wiki is a copy of provided existing wiki.
     * 
     * @param userWikiSuperDoc a wiki descriptor document from which the new wiki descriptor document will be created.
     * @param templateWikiName the of the wiki from where to copy document to the new wiki.
     * @param failOnExist if true throw exception when wiki already exist. If false overwrite existing wiki.
     * @param comment the comment to use when saving descriptor document.
     * @param context the XWiki context.
     * @return the new wiki descriptor document.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting unique instance of {@link XWikiServerClass}.</li>
     *             <li>or getting user documents.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_XWIKINOTVIRTUAL}: xwiki is not in virtual mode.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USERDOESNOTEXIST}: provided user does not exists.</li>
     *             <li>{@link WikiManagerException#ERROR_XWIKI_USER_INACTIVE}: provided user is not active.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKINAMEFORBIDDEN}: provided wiki name can't be used to
     *             create new wiki.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_WIKIALREADYEXISTS}: wiki descriptor already exists.</li>
     *             <li>{@link WikiManagerException#ERROR_WM_UPDATEDATABASE}: error occurred when updating database.</li>
     *             </ul>
     * @see #createNewWiki(XWikiServer, boolean, String, XWikiContext)
     * @see #createNewWikiFromPackage(XWikiServer, String, boolean, String, XWikiContext)
     */
    public static XWikiServer createNewWikiFromTemplate(XWikiServer userWikiSuperDoc, String templateWikiName,
        boolean failOnExist, String comment, XWikiContext context) throws XWikiException
    {
        return createNewWiki(userWikiSuperDoc, failOnExist, templateWikiName, null, comment, context);
    }

}
