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
package com.xpn.xwiki.wysiwyg.client.sync;

import java.util.ArrayList;
import java.util.List;

public class SyncStatus
{
    protected int currentVersionNumber = 0;

    protected List<String> versions = new ArrayList<String>();

    protected String pageName;

    private String lastXWikiVersion;

    private String lastXWikiContent;

    public SyncStatus(String pageName, String version, String content)
    {
        this.pageName = pageName;
        versions.add("");
        addVersion(content);
        this.lastXWikiVersion = version;
        this.lastXWikiContent = content;
    }

    public List<String> getVersions()
    {
        return versions;
    }

    public int getCurrentVersionNumber()
    {
        return currentVersionNumber;
    }

    public String getVersion(int version)
    {
        return (String) versions.get(version);
    }

    public void addVersion(String newContent)
    {
        versions.add(newContent);
        currentVersionNumber++;
    }

    public String getCurrentVersion()
    {
        return getVersion(getCurrentVersionNumber());
    }

    public String getLastXWikiVersion() {
        return lastXWikiVersion;
    }

    public String getLastXWikiContent() {
        return lastXWikiContent;
    }
    
    public void setLastXWikiContent(String version, String content) {
        this.lastXWikiVersion = version;
        this.lastXWikiContent = content;
    }

}
