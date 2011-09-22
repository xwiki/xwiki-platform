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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /**
     * The prefix used to mark license name alias.
     */
    private static final String ALIAS_PREFIX = ".alias=";

    /**
     * The package where license files are located.
     */
    private static final String LICENSE_PACKAGE = "extension.licenses";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The known licenses.
     */
    private Map<String, ExtensionLicense> licenses = new ConcurrentHashMap<String, ExtensionLicense>();

    @Override
    public void initialize() throws InitializationException
    {
        Set<URL> licenseURLs = ClasspathHelper.forPackage(LICENSE_PACKAGE);

        // FIXME: remove that as soon as possible
        licenseURLs = filterURLs(licenseURLs);

        Reflections reflections =
            new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner()).setUrls(licenseURLs)
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(LICENSE_PACKAGE))));

        for (String licenseFile : reflections.getResources(Pattern.compile(".*\\.txt"))) {
            URL licenseUrl = getClass().getClassLoader().getResource(licenseFile);

            try {
                // Get name
                String path = decode(licenseUrl.getPath());
                String name = path.substring(path.lastIndexOf('/') + 1);
                name = name.substring(0, name.length() - ".txt".length());

                // Get content
                InputStream is = licenseUrl.openStream();
                try {
                    List<String> content = IOUtils.readLines(is);

                    List<String> aliases = new ArrayList<String>();
                    aliases.add(name);

                    for (String line : content) {
                        if (!line.startsWith(ALIAS_PREFIX)) {
                            break;
                        }

                        aliases.add(line.substring(ALIAS_PREFIX.length()));
                    }

                    content = content.subList(aliases.size() - 1, content.size());

                    for (String alias : aliases) {
                        this.licenses.put(alias.toLowerCase(), new ExtensionLicense(name, content));
                    }
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                this.logger.error("Failed to load license file at [" + licenseUrl + "]", e);
            }
        }
    }

    /**
     * Very nasty hack which unescape invalid characters from the {@link URL} path because
     * {@link Reflections#Reflections(org.reflections.Configuration)} does not do it...
     * 
     * @param urls base URLs to modify
     * @return modified base URLs
     */
    // TODO: remove when http://code.google.com/p/reflections/issues/detail?id=87 is fixed
    private Set<URL> filterURLs(Set<URL> urls)
    {
        Set<URL> results = new HashSet<URL>(urls.size());
        for (URL url : urls) {
            try {
                results.add(new URL(url.getProtocol(), url.getHost(), url.getPort(), decode(url.getFile())));
            } catch (Exception e) {
                this.logger.error("Failed to convert url [" + url + "]", e);

                results.add(url);
            }
        }

        return results;
    }

    /**
     * Decode URL path.
     * 
     * @param path the URL path
     * @return the decoded path
     * @throws UnsupportedEncodingException error when unescaping provided path
     */
    private String decode(String path) throws UnsupportedEncodingException
    {
        return URLDecoder.decode(path, "UTF-8");
    }

    @Override
    public List<ExtensionLicense> getLicenses()
    {
        return new ArrayList<ExtensionLicense>(this.licenses.values());
    }

    @Override
    public ExtensionLicense getLicense(String name)
    {
        return this.licenses.get(name.toLowerCase());
    }

    @Override
    public void addLicense(ExtensionLicense license)
    {
        this.licenses.put(license.getName().toLowerCase(), license);
    }
}
