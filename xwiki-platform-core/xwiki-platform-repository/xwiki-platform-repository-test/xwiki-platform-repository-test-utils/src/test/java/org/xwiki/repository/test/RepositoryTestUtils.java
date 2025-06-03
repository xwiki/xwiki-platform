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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionSupportPlan;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.ExtensionSupporter;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import com.xpn.xwiki.XWiki;

import static org.xwiki.test.ui.TestUtils.RestTestUtils.object;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class RepositoryTestUtils
{
    public final static String PROPERTY_KEY = "repositoryutils";

    private final static String SPACENAME_EXTENSION = "Extension";

    private static final LocalStringEntityReferenceSerializer LOCAL_REFERENCE_SERIALIZER =
        new LocalStringEntityReferenceSerializer(new DefaultSymbolScheme());

    /**
     * @since 7.3M1
     */
    public static org.xwiki.rest.model.jaxb.Object extensionObject(RemoteExtension extension)
    {
        org.xwiki.rest.model.jaxb.Object extensionObject = object(XWikiRepositoryModel.EXTENSION_CLASSNAME);

        extensionObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_EXTENSION_ID, extension.getId().getId()));
        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_TYPE, extension.getType()));
        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_NAME, extension.getName()));
        extensionObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_EXTENSION_SUMMARY, extension.getSummary()));
        if (!extension.getLicenses().isEmpty()) {
            extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME,
                extension.getLicenses().iterator().next().getName()));
        }
        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_FEATURES,
            ExtensionIdConverter.toStringList(extension.getExtensionFeatures())));
        List<String> authors = new ArrayList<String>();
        for (ExtensionAuthor author : extension.getAuthors()) {
            authors.add(author.getName());
        }
        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, authors));
        List<String> supportPlans = new ArrayList<>();
        for (ExtensionSupportPlan supportPlan : extension.getSupportPlans().getSupportPlans()) {
            supportPlans.add(LOCAL_REFERENCE_SERIALIZER.serialize(toSupportPlanReference(supportPlan)));
        }
        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS, supportPlans));
        extensionObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_EXTENSION_WEBSITE, extension.getWebSite()));
        if (extension.getAllowedNamespaces() != null) {
            extensionObject.getProperties()
                .add(property(XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES, extension.getAllowedNamespaces()));
            extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES_EMPTY,
                extension.getAllowedNamespaces() != null && extension.getAllowedNamespaces().isEmpty() ? 1 : 0));
        }
        if (extension.getScm() != null) {
            extensionObject.getProperties()
                .add(property(XWikiRepositoryModel.PROP_EXTENSION_SCMURL, extension.getScm().getUrl()));
        }
        return extensionObject;
    }

    /**
     * @since 7.3M1
     */
    public static org.xwiki.rest.model.jaxb.Object extensionVersionObject(Extension extension)
    {
        org.xwiki.rest.model.jaxb.Object extensionVersionObject = extensionVersionObject(extension.getId().getVersion(),
            null, XWikiRepositoryModel.toStringList(extension.getRepositories()));

        extensionVersionObject.getProperties().add(property(XWikiRepositoryModel.PROP_VERSION_FEATURES,
            ExtensionIdConverter.toStringList(extension.getExtensionFeatures())));

        return extensionVersionObject;
    }

    /**
     * @since 7.3M1
     */
    public static org.xwiki.rest.model.jaxb.Object extensionVersionObject(Object version, Object download,
        Object repositories)
    {
        org.xwiki.rest.model.jaxb.Object versionObject = object(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME);

        if (version != null) {
            versionObject.getProperties().add(property(XWikiRepositoryModel.PROP_VERSION_VERSION, version));
        }
        if (download != null) {
            versionObject.getProperties().add(property(XWikiRepositoryModel.PROP_VERSION_DOWNLOAD, download));
        }
        if (repositories != null) {
            versionObject.getProperties().add(property(XWikiRepositoryModel.PROP_VERSION_REPOSITORIES, repositories));
        }

        return versionObject;
    }

    /**
     * @since 7.3M1
     */
    public static List<org.xwiki.rest.model.jaxb.Object> extensionDependencyObjects(Extension extension)
    {
        List<org.xwiki.rest.model.jaxb.Object> dependencies = new ArrayList<>(extension.getDependencies().size());

        int number = 0;
        for (ExtensionDependency dependency : extension.getDependencies()) {
            org.xwiki.rest.model.jaxb.Object dependencyObject =
                extensionDependencyObject(extension.getId().getVersion(), dependency);
            dependencyObject.setNumber(number++);

            dependencies.add(dependencyObject);
        }

        return dependencies;
    }

    /**
     * @since 7.3M1
     */
    public static org.xwiki.rest.model.jaxb.Object extensionDependencyObject(Object version,
        ExtensionDependency dependency)
    {
        org.xwiki.rest.model.jaxb.Object dependencyObject = object(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME);

        dependencyObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT, dependency.getVersionConstraint()));

        dependencyObject.getProperties().add(property(XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, version));

        dependencyObject.getProperties().add(property(XWikiRepositoryModel.PROP_DEPENDENCY_ID, dependency.getId()));

        dependencyObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_DEPENDENCY_OPTIONAL, dependency.isOptional() ? 1 : 0));

        return dependencyObject;
    }

    public static org.xwiki.rest.model.jaxb.Object extensionSupporterObject(ExtensionSupporter extensionSupporter)
    {
        org.xwiki.rest.model.jaxb.Object extensionObject = object(XWikiRepositoryModel.EXTENSIONSUPPORTER_CLASSNAME);

        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_SUPPORTER_ACTIVE, 1));

        if (extensionSupporter.getURL() != null) {
            extensionObject.getProperties()
                .add(property(XWikiRepositoryModel.PROP_SUPPORTER_URL, extensionSupporter.getURL()));
        }

        return extensionObject;
    }

    public static org.xwiki.rest.model.jaxb.Object extensionSupportPlanObject(ExtensionSupportPlan extensionSupportPlan,
        TestUtils testUtils)
    {
        org.xwiki.rest.model.jaxb.Object extensionObject = object(XWikiRepositoryModel.EXTENSIONSUPPORTPLAN_CLASSNAME);

        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_SUPPORTER_ACTIVE, 1));

        if (extensionSupportPlan.getURL() != null) {
            extensionObject.getProperties()
                .add(property(XWikiRepositoryModel.PROP_SUPPORTPLAN_ACTIVE, extensionSupportPlan.getURL()));
        }

        extensionObject.getProperties()
            .add(property(XWikiRepositoryModel.PROP_SUPPORTPLAN_PAYING, extensionSupportPlan.isPaying()));

        extensionObject.getProperties().add(property(XWikiRepositoryModel.PROP_SUPPORTPLAN_SUPPORTER,
            testUtils.serializeLocalReference(toSupporterReference(extensionSupportPlan.getSupporter()))));

        return extensionObject;
    }

    private static LocalDocumentReference toSupporterReference(ExtensionSupporter supporter)
    {
        String cleanSupporterName = new XWiki().clearName(supporter.getName(), null);
        return new LocalDocumentReference(List.of("Extension", "Support", "Supporter", cleanSupporterName), "WebHome");
    }

    private static LocalDocumentReference toSupportPlanReference(ExtensionSupportPlan supportPlan)
    {
        LocalDocumentReference supporterReference = toSupporterReference(supportPlan.getSupporter());

        String cleanSupportPlanName = new XWiki().clearName(supportPlan.getName(), null);

        return new LocalDocumentReference("WebHome",
            new EntityReference(cleanSupportPlanName, EntityType.SPACE, supporterReference.getParent()));
    }

    private final TestUtils testUtils;

    private RepositoryUtils repositoryUtil;

    private SolrTestUtils solrUtils;

    public RepositoryTestUtils(TestUtils testUtils, RepositoryUtils repositoryUtil, SolrTestUtils solrUtils)
    {
        this.testUtils = testUtils;
        this.repositoryUtil = repositoryUtil != null ? repositoryUtil : new RepositoryUtils();
        this.solrUtils = solrUtils;
    }

    public RepositoryUtils getRepositoryUtil()
    {
        return this.repositoryUtil;
    }

    // Test init

    public void init() throws Exception
    {
        init(new TestEnvironment());
    }

    public void init(TestEnvironment environment) throws Exception
    {
        // Initialize extensions and repositories
        this.repositoryUtil.setup(environment);
    }

    // Test utils

    public TestExtension getTestExtension(ExtensionId id, String type)
    {
        File extensionFile = this.repositoryUtil.getExtensionPackager().getExtensionFile(id);

        return extensionFile != null ? new TestExtension(id, type, extensionFile) : null;
    }

    private LocalDocumentReference getExtensionPageReference(Extension extension)
    {
        String extensionName = extension.getName() != null ? extension.getName() : extension.getId().getId();

        return getExtensionPageReference(extensionName);
    }

    /**
     * @since 7.3M2
     */
    private LocalDocumentReference getExtensionPageReference(String extensionName)
    {
        return new LocalDocumentReference(Arrays.asList(SPACENAME_EXTENSION, extensionName), "WebHome");
    }

    public void deleteExtension(Extension extension) throws Exception
    {
        this.testUtils.rest().delete(getExtensionPageReference(extension));
    }

    /**
     * @since 7.3M2
     */
    public void deleteExtension(String extensionName) throws Exception
    {
        this.testUtils.rest().delete(getExtensionPageReference(extensionName));
    }

    private boolean supporterExist(ExtensionSupporter supporter) throws Exception
    {
        return this.testUtils.rest().exists(toSupporterReference(supporter));
    }

    private boolean supportPlanExist(ExtensionSupportPlan supportPlan) throws Exception
    {
        return this.testUtils.rest().exists(toSupportPlanReference(supportPlan));
    }

    public void addExtension(RemoteExtension extension) throws Exception
    {
        // Delete any pre-existing extension
        deleteExtension(extension);

        // Create Supporters and Support Plans Pages (if needed)
        ExtensionSupportPlans supportPlans = extension.getSupportPlans();
        for (ExtensionSupporter supporter : supportPlans.getSupporters()) {
            if (!supporterExist(supporter)) {
                // Create the Supporter Page
                addExtensionSupporter(supporter);
            }

            // Create Support Plans Pages (if needed)
            for (ExtensionSupportPlan supportPlan : supportPlans.getSupportPlans(supporter)) {
                if (!supportPlanExist(supportPlan)) {
                    // Create the Support Plan Page
                    addExtensionSupportPlan(supportPlan);
                }
            }
        }

        // Create Extension Page
        Page extensionPage = this.testUtils.rest().page(getExtensionPageReference(extension));

        extensionPage.setObjects(new Objects());

        // Add extension object
        extensionPage.getObjects().getObjectSummaries().add(extensionObject(extension));

        // Add the ExtensionVersion object
        extensionPage.getObjects().getObjectSummaries().add(extensionVersionObject(extension));

        // Add the ExtensionDependency objects
        extensionPage.getObjects().getObjectSummaries().addAll(extensionDependencyObjects(extension));

        // Save the extension page
        this.testUtils.rest().save(extensionPage, TestUtils.STATUS_CREATED);

        // Attach the extension file
        attachFile(extension);
    }

    public void addExtensionSupporter(ExtensionSupporter supporter) throws Exception
    {
        LocalDocumentReference supporterReference = toSupporterReference(supporter);

        // Delete any pre-existing supporter
        this.testUtils.rest().delete(supporterReference);

        Page supporterPage = this.testUtils.rest().page(supporterReference);

        supporterPage.setTitle(supporter.getName());

        supporterPage.setObjects(new Objects());
        supporterPage.getObjects().getObjectSummaries().add(extensionSupporterObject(supporter));

        this.testUtils.rest().save(supporterPage, TestUtils.STATUS_CREATED);
    }

    public void addExtensionSupportPlan(ExtensionSupportPlan supportPlan) throws Exception
    {
        LocalDocumentReference supportPlanReference = toSupportPlanReference(supportPlan);

        // Delete any pre-existing supporter plan
        this.testUtils.rest().delete(supportPlanReference);

        Page supportPlanPage = this.testUtils.rest().page(supportPlanReference);

        supportPlanPage.setTitle(supportPlan.getName());

        supportPlanPage.setObjects(new Objects());
        supportPlanPage.getObjects().getObjectSummaries().add(extensionSupportPlanObject(supportPlan, this.testUtils));

        this.testUtils.rest().save(supportPlanPage, TestUtils.STATUS_CREATED);
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

        this.testUtils.addObject(getExtensionPageReference(extension), XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME,
            queryParameters);
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
        this.testUtils.addObject(getExtensionPageReference(extension),
            XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME, XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT,
            dependency.getVersionConstraint(), XWikiRepositoryModel.PROP_DEPENDENCY_ID, dependency.getId(),
            XWikiRepositoryModel.PROP_DEPENDENCY_OPTIONAL, dependency.isOptional() ? 1 : 0,
            XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, version);
    }

    public void attachFile(Extension extension) throws Exception
    {
        InputStream is = extension.getFile().openStream();
        try {
            this.testUtils.attachFile(getExtensionPageReference(extension),
                extension.getId().getId() + "-" + extension.getId().getVersion() + "." + extension.getType(), is, true);
        } finally {
            is.close();
        }
    }

    /**
     * Make sure any change done in extension has been taken into account by Solr.
     */
    public void waitUntilReady() throws Exception
    {
        // Make sure Solr queue is empty
        this.solrUtils.waitEmptyQueue();
    }

    /**
     * @since 9.5RC1
     */
    public ObjectEditPage gotoExtensionObjectsEditPage(String extensionName)
    {
        LocalDocumentReference extensionPageReference = getExtensionPageReference(extensionName);
        testUtils.gotoPage(extensionPageReference, "edit", Collections.singletonMap("editor", "object"));
        return new ObjectEditPage();
    }
}
