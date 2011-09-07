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
package org.xwiki.extension.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;

/**
 * Default implementation of {@link ExtensionLicenseManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultExtensionLicenseManager implements ExtensionLicenseManager, Initializable
{
    @Inject
    private Logger logger;

    /**
     * The known licenses.
     */
    private Map<String, ExtensionLicense> licenses = new ConcurrentHashMap<String, ExtensionLicense>();

    @Override
    public void initialize() throws InitializationException
    {
        Reflections reflections =
            new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner())
                .setUrls(ClasspathHelper.forPackage(""))
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix("extension.licenses"))));

        for (String licenseFile : reflections.getResources(Pattern.compile(".*\\.txt"))) {
            URL licenseUrl = getClass().getClassLoader().getResource(licenseFile);

            try {
                // Get name
                String path = licenseUrl.toURI().getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                name = name.substring(0, name.length() - ".txt".length());

                // Get content
                InputStream is = licenseUrl.openStream();
                try {
                    List<String> content = IOUtils.readLines(is);

                    this.licenses.put(name, new ExtensionLicense(name, content));
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                this.logger.error("Failed to load license file at [" + licenseUrl + "]", e);
            }
        }
    }

    @Override
    public List<ExtensionLicense> getLicenses()
    {
        return new ArrayList<ExtensionLicense>(this.licenses.values());
    }

    @Override
    public ExtensionLicense getLicense(String name)
    {
        return this.licenses.get(name);
    }

    @Override
    public void addLicense(ExtensionLicense license)
    {
        this.licenses.put(license.getName(), license);
    }
}
