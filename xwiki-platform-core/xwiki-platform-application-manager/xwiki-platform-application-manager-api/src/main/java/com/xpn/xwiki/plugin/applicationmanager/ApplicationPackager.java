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
package com.xpn.xwiki.plugin.applicationmanager;

import java.io.IOException;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.DocumentInfoAPI;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * Provide method to install export applications.
 * 
 * @version $Id$
 * @since 1.9
 */
public class ApplicationPackager
{
    /**
     * The name of the internal packaging plugin.
     */
    private static final String PACKAGEPLUGIN_NAME = "package";

    /**
     * The message tool to use to generate error or comments.
     */
    private XWikiPluginMessageTool messageTool;

    /**
     * Protected API for managing applications.
     */
    private ApplicationManager applicationManager;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @param messageTool the message tool
     */
    public ApplicationPackager(XWikiPluginMessageTool messageTool)
    {
        this.messageTool = messageTool;

        this.applicationManager = new ApplicationManager(this.messageTool);
    }

    /**
     * Get the {@link XWikiPluginMessageTool} to use with ApplicationManager.
     * 
     * @param context the XWiki context.
     * @return a translated strings manager.
     */
    public XWikiPluginMessageTool getMessageTool(XWikiContext context)
    {
        return this.messageTool != null ? this.messageTool : ApplicationManagerMessageTool.getDefault(context);
    }

    /**
     * Export an application into XAR using Packaging plugin.
     * 
     * @param appName the name of the application to export.
     * @param recurse indicate if dependencies applications has to be included in the package.
     * @param withDocHistory indicate if history of documents is exported.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting application descriptor document to export.</li>
     *             <li>or getting application's documents to export.</li>
     *             <li>or when apply export.</li>
     *             </ul>
     * @throws IOException error when apply export.
     */
    public void exportApplicationXAR(String appName, boolean recurse, boolean withDocHistory, XWikiContext context)
        throws XWikiException, IOException
    {
        XWikiApplication app = this.applicationManager.getApplication(appName, context, true);

        PackageAPI export = ((PackageAPI) context.getWiki().getPluginApi(PACKAGEPLUGIN_NAME, context));

        export.setName(app.getAppName() + "-" + app.getAppVersion());

        Set<String> documents = app.getDocumentsNames(recurse, true);
        for (String documentName : documents) {
            export.add(documentName, DocumentInfo.ACTION_OVERWRITE);
        }

        export.setWithVersions(withDocHistory);

        export.export();
    }

    /**
     * Import attached application XAR into current wiki and do all actions needed to installation an application. See
     * {@link #reloadApplication(XWikiApplication, String, XWikiContext)} for more.
     * 
     * @param packageDoc the document where package to import is attached.
     * @param packageName the name of the attached XAR file to import.
     * @param comment a comment used update XWiki.XWikiPreferences.
     * @param context the XWiki context.
     * @throws XWikiException error when :
     *             <ul>
     *             <li>getting attached package file.</li>
     *             <li>or load package in memory.</li>
     *             <li>or installing loaded document in database</li>
     *             <li>or apply application initialization for each application descriptor document.</li>
     *             </ul>
     */
    public void importApplication(XWikiDocument packageDoc, String packageName, String comment, XWikiContext context)
        throws XWikiException
    {
        XWikiAttachment packFile = packageDoc.getAttachment(packageName);

        if (packFile == null) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_PKGDOESNOTEXISTS, packageName));
        }

        // Import
        PackageAPI importer = ((PackageAPI) context.getWiki().getPluginApi(PACKAGEPLUGIN_NAME, context));

        try {
            importer.Import(packFile.getContent(context));
        } catch (IOException e) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_IMPORT, packageName), e);
        }

        if (importer.install() == DocumentInfo.INSTALL_IMPOSSIBLE) {
            throw new ApplicationManagerException(XWikiException.ERROR_XWIKI_UNKNOWN, getMessageTool(context).get(
                ApplicationManagerMessageTool.ERROR_IMORT_INSTALL, packageName));
        }

        // Apply applications installation
        for (DocumentInfoAPI docinfo : importer.getFiles()) {
            XWikiDocument doc = docinfo.getDocInfo().getDoc();

            if (XWikiApplicationClass.getInstance(context).isInstance(doc)) {
                this.applicationManager.reloadApplication(
                    XWikiApplicationClass.getInstance(context).newXObjectDocument(doc, 0, context), comment, context);
            }
        }
    }
}
