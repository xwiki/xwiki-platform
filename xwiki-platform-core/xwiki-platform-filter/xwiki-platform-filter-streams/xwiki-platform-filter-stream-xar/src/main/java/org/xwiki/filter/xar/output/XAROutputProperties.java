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
package org.xwiki.filter.xar.output;

import org.xwiki.filter.xml.output.XMLOutputProperties;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;

/**
 * XAR output properties.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Unstable
public class XAROutputProperties extends XMLOutputProperties
{
    /**
     * @see #isPreserveVersion()
     */
    private boolean preserveVersion = true;

    /**
     * @see #isForceDocument()
     */
    private boolean forceDocument;

    /**
     * @see #getPackageName()
     */
    private String packageName;

    /**
     * @see #getPackageDescription()
     */
    private String packageDescription;

    /**
     * @see #getPackageLicense()
     */
    private String packageLicense;

    /**
     * @see #getPackageAuthor()
     */
    private String packageAuthor;

    /**
     * @see #getPackageVersion()
     */
    private String packageVersion;

    /**
     * @see #isPackageBackupPack()
     */
    private boolean packageBackupPack;

    /**
     * @see #getPackageExtensionId()
     */
    private String packageExtensionId;

    /**
     * @return Indicate if all revisions related informations should be serialized
     */
    @PropertyName("Preserve revisions informations")
    @PropertyDescription("Indicate if all revisions related informations should be serialized")
    public boolean isPreserveVersion()
    {
        return this.preserveVersion;
    }

    /**
     * @param preserveVersion Indicate if all revisions related informations should be serialized
     */
    public void setPreserveVersion(boolean preserveVersion)
    {
        this.preserveVersion = preserveVersion;
    }

    /**
     * @return true if a unique document should be serialized instead of a XAR package
     */
    @PropertyName("Force document")
    @PropertyDescription("Force serializing a unique document XML instead of a XAR package")
    public boolean isForceDocument()
    {
        return this.forceDocument;
    }

    /**
     * @param forceDocument true if a unique document should be serialized instead of a XAR package
     */
    public void setForceDocument(boolean forceDocument)
    {
        this.forceDocument = forceDocument;
    }

    // package.xml

    /**
     * @return Indicate if all revisions related informations should be serialized
     */
    @PropertyName("Package Name")
    @PropertyDescription("The name to put in package.xml")
    public String getPackageName()
    {
        return this.packageName;
    }

    /**
     * @param packageName Indicate if all revisions related informations should be serialized
     */
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    /**
     * @return The description to put in package.xml
     */
    @PropertyName("Package description")
    @PropertyDescription("The description to put in package.xml")
    public String getPackageDescription()
    {
        return this.packageDescription;
    }

    /**
     * @param packageDescription The description to put in package.xml
     */
    public void setPackageDescription(String packageDescription)
    {
        this.packageDescription = packageDescription;
    }

    /**
     * @return "The licence to put in package.xml
     */
    @PropertyName("Package license")
    @PropertyDescription("The licence to put in package.xml")
    public String getPackageLicense()
    {
        return this.packageLicense;
    }

    /**
     * @param packageLicense "The licence to put in package.xml
     */
    public void setPackageLicense(String packageLicense)
    {
        this.packageLicense = packageLicense;
    }

    /**
     * @return The author to put in package.xml
     */
    @PropertyName("Package author")
    @PropertyDescription("The author to put in package.xml")
    public String getPackageAuthor()
    {
        return this.packageAuthor;
    }

    /**
     * @param packageAuthor The author to put in package.xml
     */
    public void setPackageAuthor(String packageAuthor)
    {
        this.packageAuthor = packageAuthor;
    }

    /**
     * @return The version to put in package.xml
     */
    @PropertyName("Package version")
    @PropertyDescription("The version to put in package.xml")
    public String getPackageVersion()
    {
        return this.packageVersion;
    }

    /**
     * @param packageVersion The version to put in package.xml
     */
    public void setPackageVersion(String packageVersion)
    {
        this.packageVersion = packageVersion;
    }

    /**
     * @return Indicate in package.xml if the XAR is a backup pack
     */
    @PropertyName("Package backuppack")
    @PropertyDescription("Indicate in package.xml if the XAR is a backup pack")
    public boolean isPackageBackupPack()
    {
        return this.packageBackupPack;
    }

    /**
     * @param packageBackupPack Indicate in package.xml if the XAR is a backup pack
     */
    public void setPackageBackupPack(boolean packageBackupPack)
    {
        this.packageBackupPack = packageBackupPack;
    }

    /**
     * @return the id of the extension the XAR contains
     */
    @PropertyName("Extension id")
    @PropertyDescription("The id of the extension the XAR contains")
    public String getPackageExtensionId()
    {
        return this.packageExtensionId;
    }

    /**
     * @param packageExtensionId the id of the extension the XAR contains
     */
    public void setPackageExtensionId(String packageExtensionId)
    {
        this.packageExtensionId = packageExtensionId;
    }
}
