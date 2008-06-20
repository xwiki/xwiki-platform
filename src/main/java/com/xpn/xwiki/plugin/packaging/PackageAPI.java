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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.util.Util;

public class PackageAPI extends Api
{
    Package plugin;

    public PackageAPI(Package plugin, XWikiContext context) throws PackageException
    {
        super(context);

        if (!hasAdminRights()) {
            throw new PackageException(XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Admin right is needed to use this plugin");
        }

        setPlugin(plugin);
    }

    private void setPlugin(Package plugin)
    {
        this.plugin = plugin;
    }

    public String getName()
    {
        return this.plugin.getName();
    }

    public void setName(String name)
    {
        this.plugin.setName(name);
    }

    public Package getPackage()
    {
        if (hasProgrammingRights()) {
            return this.plugin;
        }
        return null;
    }

    public String getDescription()
    {
        return this.plugin.getDescription();
    }

    public void setDescription(String description)
    {
        this.plugin.setDescription(description);
    }

    public String getVersion()
    {
        return this.plugin.getVersion();
    }

    public void setVersion(String version)
    {
        this.plugin.setVersion(version);
    }

    public String getLicence()
    {
        return this.plugin.getLicence();
    }

    public void setLicence(String licence)
    {
        this.plugin.setLicence(licence);
    }

    public String getAuthorName()
    {
        return this.plugin.getAuthorName();
    }

    public void setAuthorName(String authorName)
    {
        this.plugin.setAuthorName(authorName);
    }

    public boolean isBackupPack()
    {
        return this.plugin.isBackupPack();
    }

    public void setBackupPack(boolean backupPack)
    {
        this.plugin.setBackupPack(backupPack);
    }

    public boolean isVersionPreserved()
    {
        return this.plugin.isVersionPreserved();
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.plugin.setPreserveVersion(preserveVersion);
    }

    public boolean isWithVersions()
    {
        return this.plugin.isWithVersions();
    }

    public void setWithVersions(boolean withVersions)
    {
        this.plugin.setWithVersions(withVersions);
    }

    public void addDocumentFilter(Object filter) throws PackageException
    {
        this.plugin.addDocumentFilter(filter);
    }

    public List<DocumentInfoAPI> getFiles()
    {
        List<DocumentInfo> files = this.plugin.getFiles();
        ArrayList<DocumentInfoAPI> apiFiles = new ArrayList<DocumentInfoAPI>(files.size());

        for (DocumentInfo docInfo : files) {
            apiFiles.add(new DocumentInfoAPI(docInfo, getXWikiContext()));
        }

        return apiFiles;
    }

    public boolean add(String docFullName, int DefaultAction) throws XWikiException
    {
        return this.plugin.add(docFullName, DefaultAction, getXWikiContext());
    }

    public boolean add(String docFullName) throws XWikiException
    {
        return this.plugin.add(docFullName, getXWikiContext());
    }

    public void setDocumentAction(String docFullName, int action)
    {
        for (DocumentInfo docInfo : this.plugin.getFiles()) {
            if (docInfo.getFullName().compareTo(docFullName) == 0) {
                docInfo.setAction(action);
            }
        }
    }

    public void setDocumentAction(String docFullName, String language, int action)
    {
        for (DocumentInfo docInfo : this.plugin.getFiles()) {
            if ((docInfo.getFullName().compareTo(docFullName) == 0) && (language.equals(docInfo.getLanguage()))) {
                docInfo.setAction(action);
            }
        }
    }

    public String export() throws IOException, XWikiException
    {
        getXWikiContext().getResponse().setContentType("application/zip");
        getXWikiContext().getResponse().addHeader("Content-disposition",
            "attachment; filename=" + Util.encodeURI(plugin.getName(), context) + ".xar");
        getXWikiContext().setFinished(true);

        return this.plugin.export(getXWikiContext().getResponse().getOutputStream(), getXWikiContext());
    }

    public String Import(byte file[]) throws IOException, XWikiException
    {
        return this.plugin.Import(file, getXWikiContext());
    }

    public int testInstall()
    {
        return this.plugin.testInstall(false, getXWikiContext());
    }

    public int testInstall(boolean isAdmin)
    {
        return this.plugin.testInstall(isAdmin, getXWikiContext());
    }

    public void backupWiki() throws XWikiException, IOException
    {
        this.plugin.addAllWikiDocuments(getXWikiContext());
        this.export();
    }

    public String toXml()
    {
        return this.plugin.toXml(getXWikiContext());
    }

    public int install() throws XWikiException
    {
        return this.plugin.install(getXWikiContext());
    }

    public List<String> getErrors()
    {
        return this.plugin.getErrors(getXWikiContext());
    }

    public List<String> getSkipped()
    {
        return this.plugin.getSkipped(getXWikiContext());
    }

    public List<String> getInstalled()
    {
        return this.plugin.getInstalled(getXWikiContext());
    }

    public int getStatus()
    {
        return this.plugin.getStatus(getXWikiContext());
    }
}
