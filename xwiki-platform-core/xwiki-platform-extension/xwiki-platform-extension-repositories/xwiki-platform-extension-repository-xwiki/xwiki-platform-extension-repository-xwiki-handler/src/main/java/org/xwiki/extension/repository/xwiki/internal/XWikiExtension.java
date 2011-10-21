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
import java.io.StringReader;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;

/**
 * XWiki Repository implementation of {@link Extension}.
 * 
 * @version $Id$
 */
public class XWikiExtension extends AbstractExtension
{
    public XWikiExtension(XWikiExtensionRepository repository, ExtensionVersion extension,
        ExtensionLicenseManager licenseManager)
    {
        super(repository, new ExtensionId(extension.getId(), extension.getVersion()), extension.getType());

        setName(extension.getName());
        setSummary(extension.getSummary());
        setDescription(extension.getDescription());
        setWebsite(extension.getWebsite());

        setAuthors(extension.getAuthors());
        setFeatures(extension.getFeatures());
        
        // License

        License license = extension.getLicense();
        if (license != null && license.getName() != null) {
            ExtensionLicense extensionLicense = licenseManager.getLicense(license.getName());
            if (extensionLicense != null) {
                addLicense(extensionLicense);
            } else {
                List<String> content = null;
                if (license.getContent() != null) {
                    try {
                        content = IOUtils.readLines(new StringReader(license.getContent()));
                    } catch (IOException e) {
                        // That should never happen
                    }
                }

                addLicense(new ExtensionLicense(license.getName(), content));
            }
        }

        // Dependencies

        for (ExtensionDependency dependency : extension.getDependencies()) {
            addDependency(new DefaultExtensionDependency(dependency.getId(), dependency.getVersion()));
        }
    }

    @Override
    public XWikiExtensionRepository getRepository()
    {
        return (XWikiExtensionRepository) super.getRepository();
    }

    @Override
    public void download(File file) throws ExtensionException
    {
        XWikiExtensionRepository repository = getRepository();

        try {
            InputStream stream =
                repository.getRESTResourceAsStream(repository.getExtensionFileUriBuider(), getId().getId(), getId()
                    .getVersion());

            FileUtils.copyInputStreamToFile(stream, file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to download extension [" + this + "]");
        }
    }
}
