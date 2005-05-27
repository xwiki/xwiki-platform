package com.xpn.xwiki.plugin.packaging;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.io.IOException;
import java.util.List;

/**
 * ===================================================================
 * <p/>
 * Copyright (c) 2005 Jérémi Joslin, All rights reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 * <p/>
 * User: jeremi
 * Date: May 16, 2005
 * Time: 5:48:29 PM
 */
public class ImportPluginApi   extends Api {


    private ImportPlugin    plugin;
    private Package         pack;


    public ImportPluginApi(ImportPlugin plugin, XWikiContext context) {
        super(context);
        setPlugin(plugin);
        pack = new Package();
    }

    public ImportPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(ImportPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSpace(String SpaceName)
    {
        pack.setSpaceName(SpaceName);
    }

    public List getDocuments()
    {
        return pack.getFiles();
    }

    public int testInstall()
    {
        return pack.TestInstall(super.context);
    }

    public void importPackage(byte[] PackageFile) throws IOException, XWikiException {
        pack.Import(PackageFile, super.context);
    }

    public int install()
    {
        return pack.install(super.context);
    }

    public String getTestInstallToString(int status)
    {
        return DocumentInfo.installStatusToString(status);
    }

    public String getActionToString(int status)
    {
        return DocumentInfo.actionToString(status);
    }

    public String getName()
    {
        return (pack.getName());
    }

    public String getAuthor()
    {
        return (pack.getAuthorName());
    }

    public String getVersion()
    {
        return (pack.getVersion());
    }

    public String getDescription()
    {
        return (pack.getDescription());
    }

    public String getLicence()
    {
        return (pack.getLicence());
    }

    public void setDocumentAction(String docName, String action) throws XWikiException {
        if (action == null || action.length() == 0 || docName == null || docName.length() == 0)
            return;
        int iaction = DocumentInfo.actionToInt(action);

        pack.updateDoc(docName, iaction, context);
    }
}
