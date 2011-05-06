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
package org.xwiki.extension.repository.xwiki.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.io.FileUtils;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "Extension")
@XmlRootElement(name = "extension")
public class XWikiExtension extends AbstractExtension
{
    public XWikiExtension()
    {

    }

    public XWikiExtension(ExtensionId id, String type)
    {
        super(null, id, type);
    }

    public XWikiExtension(XWikiExtensionRepository repository, ExtensionId id, String type)
    {
        super(repository, id, type);
    }

    @Override
    public XWikiExtensionRepository getRepository()
    {
        return (XWikiExtensionRepository) super.getRepository();
    }

    public void download(File file) throws ExtensionException
    {
        XWikiExtensionRepository repository = getRepository();

        try {
            InputStream stream =
                repository.getRESTResourceAsStream(repository.getExtensionFileUriBuider(), getExtensionId(),
                    getExtensionVersion());

            FileUtils.copyInputStreamToFile(stream, file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to download extension [" + this + "]");
        }
    }

    // Properties

    @XmlElement(name = "id", required = true)
    public String getExtensionId()
    {
        return getId().getId();
    }

    public void setExtensionId(String id)
    {
        super.setId(new ExtensionId(id, getId().getVersion()));
    }

    @XmlElement(name = "version", required = true)
    public String getExtensionVersion()
    {
        return getId().getVersion();
    }

    public void setExtensionVersion(String version)
    {
        super.setId(new ExtensionId(getId().getId(), version));
    }

    @XmlElement(required = true)
    public String getType()
    {
        return super.getType();
    }

    @XmlElement(required = false)
    public String getDescription()
    {
        return super.getDescription();
    }

    @XmlElement(required = false)
    public String getAuthor()
    {
        return super.getAuthor();
    }

    @XmlElement(required = false)
    public String getWebSite()
    {
        return super.getWebSite();
    }

    @XmlElement(required = false)
    public String getName()
    {
        return super.getName();
    }

    @XmlElement(required = false)
    public List<XWikiExtensionDependency> getDependencies()
    {
        return (List<XWikiExtensionDependency>) getDependencies();
    }
}
