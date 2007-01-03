/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author jeremi
 * @author sdumitriu
 */


package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PackageAPI   extends Api {
    Package plugin;

    public PackageAPI(Package plugin, XWikiContext context) throws PackageException {
        super(context);
        if (!hasAdminRights())
            throw new PackageException(XWikiException.ERROR_XWIKI_ACCESS_DENIED, "Admin right is needed to use this plugin");
        setPlugin(plugin);
    }

    private void setPlugin(Package plugin) {
        this.plugin = plugin;
    }

    public String getName() {
        return plugin.getName();
    }

    public void setName(String name) {
        plugin.setName(name);
    }

    public Package getPackage()
    {
        if (hasProgrammingRights())
            return plugin;
        return null;
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public void setDescription(String description) {
        plugin.setDescription(description);
    }

    public String getVersion() {
        return plugin.getVersion();
    }

    public void setVersion(String version) {
        plugin.setVersion(version);
    }

    public String getLicence() {
        return plugin.getLicence();
    }

    public void setLicence(String licence) {
        plugin.setLicence(licence);
    }

    public String getAuthorName() {
        return plugin.getAuthorName();
    }

    public void setAuthorName(String authorName) {
        plugin.setAuthorName(authorName);
    }

    public boolean isBackupPack() {
        return plugin.isBackupPack();
    }

    public void setBackupPack(boolean backupPack) {
        plugin.setBackupPack(backupPack);
    }

    public boolean isWithVersions() {
         return plugin.isWithVersions();
     }

    public void setWithVersions(boolean withVersions) {
         plugin.setWithVersions(withVersions);
     }

    public void addDocumentFilter(Object filter) throws PackageException {
        plugin.addDocumentFilter(filter);
    }

    public List getFiles() {
        List files =  plugin.getFiles();
        ArrayList APIfiles = new ArrayList(files.size());

        for (int i = 0; i < files.size(); i++)
            APIfiles.add(new DocumentInfoAPI((DocumentInfo) files.get(i), context));
        return APIfiles;
    }


    public boolean add(String docFullName, int DefaultAction) throws XWikiException {
        return plugin.add(docFullName, DefaultAction, context);
    }

    public boolean add(String docFullName) throws XWikiException {
        return plugin.add(docFullName, context);
    }

    public void setDocumentAction(String docFullName, int action)
    {
        Iterator it = plugin.getFiles().iterator();
        while (it.hasNext())
        {
            DocumentInfo docInfos = (DocumentInfo)it.next();
            if (docInfos.getFullName().compareTo(docFullName) == 0)
                docInfos.setAction(action);
        }
    }

    public void setDocumentAction(String docFullName, String language, int action)
    {
        Iterator it = plugin.getFiles().iterator();
        while (it.hasNext())
        {
            DocumentInfo docInfos = (DocumentInfo)it.next();
            if ((docInfos.getFullName().compareTo(docFullName) == 0)&&(language.equals(docInfos.getLanguage())))
                docInfos.setAction(action);
        }
    }


    public String export() throws IOException, XWikiException {
        context.getResponse().setContentType("application/zip");
        context.getResponse().addHeader("Content-disposition", "attachment; filename=" + XWiki.getURLEncoded(plugin.getName()) + ".xar");
        context.setFinished(true);
        return  plugin.export(context.getResponse().getOutputStream(), context);
    }

    public String Import(byte file[]) throws IOException, XWikiException {
        return plugin.Import(file, context);
    }

    public int testInstall()
    {
        return plugin.testInstall(false, context);
    }

    public int testInstall(boolean isAdmin)
    {
        return plugin.testInstall(isAdmin, context);
    }

    public void backupWiki() throws XWikiException, IOException {
        plugin.addAllWikiDocuments(context);
        this.export();
    }

    public String toXml()
    {
        return plugin.toXml(context);
    }

    public int install() throws XWikiException {
        return plugin.install(context);
    }

    public List getErrors() {
        return plugin.getErrors(context);
    }

    public List getSkipped() {
        return plugin.getSkipped(context);
    }

    public List getInstalled() {
        return plugin.getInstalled(context);
    }

    public int getStatus() {
        return plugin.getStatus(context);
    }
}
