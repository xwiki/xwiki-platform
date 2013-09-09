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
package org.xwiki.wikistream.xar.internal.output;

import org.xwiki.wikistream.xar.internal.XARModel;
import org.xwiki.wikistream.xml.internal.output.XMLOuputProperties;

public class XAROutputProperties extends XMLOuputProperties
{
    private String name;

    private String description;

    private String license;

    private String author;

    private String version;

    private boolean backupPack;

    private boolean preserveVersion;

    private int defaultAction = XARModel.ACTION_OVERWRITE;

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getLicense()
    {
        return this.license;
    }

    public void setLicense(String license)
    {
        this.license = license;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public boolean isBackupPack()
    {
        return this.backupPack;
    }

    public void setBackupPack(boolean backupPack)
    {
        this.backupPack = backupPack;
    }

    public boolean isPreserveVersion()
    {
        return this.preserveVersion;
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.preserveVersion = preserveVersion;
    }

    public int getDefaultAction()
    {
        return this.defaultAction;
    }

    public void setDefaultAction(int defaultAction)
    {
        this.defaultAction = defaultAction;
    }
}
