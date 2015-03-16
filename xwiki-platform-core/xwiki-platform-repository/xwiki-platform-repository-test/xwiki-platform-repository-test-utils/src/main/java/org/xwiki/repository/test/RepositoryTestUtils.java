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
package org.xwiki.repository.test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.test.ui.TestUtils;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class RepositoryTestUtils
{
    public final static String PROPERTY_KEY = "repositoryutils";

    private final static String SPACENAME_EXTENSION = "Extension";

    private final TestUtils testUtils;

    private RepositoryUtils repositoryUtil;

    public RepositoryTestUtils(TestUtils testUtils)
    {
        this(testUtils, null);
    }

    public RepositoryTestUtils(TestUtils testUtils, RepositoryUtils repositoryUtil)
    {
        this.testUtils = testUtils;
        this.repositoryUtil = repositoryUtil != null ? repositoryUtil : new RepositoryUtils();
    }

    public RepositoryUtils getRepositoryUtil()
    {
        return this.repositoryUtil;
    }

    // Test init

    public void init() throws Exception
    {
        // Initialize extensions and repositories
        this.repositoryUtil.setup();
    }

    // Test utils

    public TestExtension getTestExtension(ExtensionId id, String type)
    {
        File extensionFile = this.repositoryUtil.getExtensionPackager().getExtensionFile(id);

        return extensionFile != null ? new TestExtension(id, type, extensionFile) : null;
    }

    private String getPageName(Extension extension)
    {
        return extension.getName() != null ? extension.getName() : extension.getId().getId();
    }

    public void deleteExtension(Extension extension)
    {
        this.testUtils.deletePage("Extension", getPageName(extension));
    }

    public void addExtension(Extension extension) throws Exception
    {
        addExtension(extension, null);
    }

    public void addExtension(Extension extension, UsernamePasswordCredentials credentials) throws Exception
    {
        this.testUtils.deletePage("Extension", getPageName(extension));

        // Add the Extension object
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_ID, extension.getId().getId());
        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_TYPE, extension.getType());

        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_NAME, extension.getName());
        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_SUMMARY, extension.getSummary());
        if (!extension.getLicenses().isEmpty()) {
            queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, extension.getLicenses().iterator()
                .next().getName());
        }
        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_FEATURES, extension.getFeatures());
        List<String> authors = new ArrayList<String>();
        for (ExtensionAuthor author : extension.getAuthors()) {
            authors.add(author.getName());
        }
        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, authors);
        queryParameters.put(XWikiRepositoryModel.PROP_EXTENSION_WEBSITE, extension.getWebSite());
        if (extension.getScm() != null) {
            queryParameters.put("source", extension.getScm().getUrl());
        }

        this.testUtils.addObject(SPACENAME_EXTENSION, getPageName(extension), XWikiRepositoryModel.EXTENSION_CLASSNAME,
            queryParameters);

        // Add the ExtensionVersion object
        addVersionObject(extension);

        // Add the ExtensionDependency objects
        addDependencies(extension);

        // Attach the file
        attachFile(extension, credentials);
    }

    public void addVersionObject(Extension extension)
    {
        addVersionObject(extension, extension.getId().getVersion(), null);
    }

    public void addVersionObject(Extension extension, Object version, Object download)
    {
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        if (version != null) {
            queryParameters.put(XWikiRepositoryModel.PROP_VERSION_VERSION, version);
        }
        if (download != null) {
            queryParameters.put(XWikiRepositoryModel.PROP_VERSION_DOWNLOAD, download);
        }

        this.testUtils.addObject(SPACENAME_EXTENSION, getPageName(extension),
            XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME, queryParameters);
    }

    public void addDependencies(Extension extension)
    {
        addDependencies(extension, extension.getId().getVersion());
    }

    public void addDependencies(Extension extension, Object version)
    {
        for (ExtensionDependency dependency : extension.getDependencies()) {
            addDependency(extension, version, dependency);
        }
    }

    public void addDependency(Extension extension, ExtensionDependency dependency)
    {
        addDependency(extension, extension.getId().getVersion(), dependency);
    }

    public void addDependency(Extension extension, Object version, ExtensionDependency dependency)
    {
        this.testUtils.addObject(SPACENAME_EXTENSION, getPageName(extension),
            XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME, XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT,
            dependency.getVersionConstraint(), XWikiRepositoryModel.PROP_DEPENDENCY_ID, dependency.getId(),
            XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, version);
    }

    public void attachFile(Extension extension) throws Exception
    {
        attachFile(extension, null);
    }

    public void attachFile(Extension extension, UsernamePasswordCredentials credentials) throws Exception
    {
        InputStream is = extension.getFile().openStream();
        try {
            this.testUtils.attachFile(SPACENAME_EXTENSION, getPageName(extension), extension.getId().getId() + "-"
                + extension.getId().getVersion() + "." + extension.getType(), is, true, credentials);
        } finally {
            is.close();
        }
    }
}
