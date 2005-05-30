package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

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
 * Date: May 9, 2005
 * Time: 4:40:57 PM
 */
public class ExportPluginApi  extends Api {


    private ExportPlugin    plugin;
    private Package         pack;


    public ExportPluginApi(ExportPlugin plugin, XWikiContext context) {
        super(context);
        setPlugin(plugin);
        pack = new Package();
    }

    public ExportPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(ExportPlugin plugin) {
        this.plugin = plugin;
    }

    public String getName()
    {
        return (pack.getName());
    }

    public void setName(String name)
    {
        pack.setName(name);
    }

    public void setSpace(String SpaceName)
    {
        pack.setSpaceName(SpaceName);
    }

    public boolean add(String DocFullName) throws XWikiException {
        return pack.add(DocFullName, super.context);
    }

    public boolean add(String DocFullName, String defaultAction) throws XWikiException {
        if (defaultAction.compareTo("skip") == 0)
            return pack.add(DocFullName, DocumentInfo.ACTION_SKIP, super.context);
        else if (defaultAction.compareTo("overwrite") == 0)
            return pack.add(DocFullName, DocumentInfo.ACTION_OVERWRITE, super.context);
        else if (defaultAction.compareTo("merge") == 0)
            return pack.add(DocFullName, DocumentInfo.ACTION_MERGE, super.context);
        else
            return pack.add(DocFullName, DocumentInfo.ACTION_NOT_DEFINED, super.context);
    }

    public void export() throws IOException, XWikiException {
        pack.Export(super.context);
    }

    public void setInfos(String name, String description, String licence, String authorName)
    {
        pack.setName(name);
        pack.setDescription(description);
        pack.setLicence(licence);
        pack.setAuthorName(authorName);
    }

    public void setVersion(String major, String minor, String revision)
    {
        pack.setVersion(major + "." + minor + "." + revision);
    }

    public void backupWiki() throws XWikiException {
        pack.backupWiki(super.context);
    }

}
