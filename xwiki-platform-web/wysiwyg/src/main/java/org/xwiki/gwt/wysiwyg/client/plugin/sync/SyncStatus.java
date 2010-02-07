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
package org.xwiki.gwt.wysiwyg.client.plugin.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * The synchronization status.
 * 
 * @version $Id$
 */
public class SyncStatus
{
    /**
     * The current version number.
     */
    protected int currentVersionNumber;

    /**
     * The list of versions.
     */
    protected List<String> versions = new ArrayList<String>();

    /**
     * The name of the edited page.
     */
    protected String pageName;

    /**
     * The current version identifier of the edited page.
     */
    private String lastXWikiVersion;

    /**
     * The current content of the edited page.
     */
    private String lastXWikiContent;

    /**
     * Creates a new synchronization status.
     * 
     * @param pageName the name of the edited page
     * @param version the version of the edited page
     * @param content the content of the edited page
     */
    public SyncStatus(String pageName, String version, String content)
    {
        this.pageName = pageName;
        versions.add("");
        addVersion(content);
        this.lastXWikiVersion = version;
        this.lastXWikiContent = content;
    }

    /**
     * @return {@link #versions}
     */
    public List<String> getVersions()
    {
        return versions;
    }

    /**
     * @return {@link #currentVersionNumber}
     */
    public int getCurrentVersionNumber()
    {
        return currentVersionNumber;
    }

    /**
     * @param version version index
     * @return the content in the specified version
     */
    public String getVersion(int version)
    {
        return (String) versions.get(version);
    }

    /**
     * Adds a new version.
     * 
     * @param newContent the content for the new version
     */
    public void addVersion(String newContent)
    {
        versions.add(newContent);
        currentVersionNumber++;
    }

    /**
     * @return the content for the current version
     */
    public String getCurrentVersion()
    {
        return getVersion(getCurrentVersionNumber());
    }

    /**
     * @return {@link #lastXWikiVersion}
     */
    public String getLastXWikiVersion()
    {
        return lastXWikiVersion;
    }

    /**
     * @return {@link #lastXWikiContent}
     */
    public String getLastXWikiContent()
    {
        return lastXWikiContent;
    }

    /**
     * Updates the state of the edited page.
     * 
     * @param version the current version of the edited page
     * @param content the current content of the edited page
     */
    public void setLastXWikiContent(String version, String content)
    {
        this.lastXWikiVersion = version;
        this.lastXWikiContent = content;
    }
}
