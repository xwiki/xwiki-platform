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

import org.apache.commons.io.FileUtils;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;

public class XWikiExtension extends AbstractExtension
{
    public XWikiExtension(XWikiExtensionRepository repository, Extension extension)
    {
        super(repository, new ExtensionId(extension.getId(), extension.getVersion()), extension.getType());

        setName(extension.getName());
        setDescription(extension.getDescription());
        setAuthors(extension.getAuthors());
        setWebsite(extension.getWebsite());

        for (ExtensionDependency dependency : extension.getDependencies()) {
            addDependency(new XWikiExtensionDependency(dependency));
        }
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
                repository.getRESTResourceAsStream(repository.getExtensionFileUriBuider(), getId().getId(), getId()
                    .getVersion());

            FileUtils.copyInputStreamToFile(stream, file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to download extension [" + this + "]");
        }
    }
}
