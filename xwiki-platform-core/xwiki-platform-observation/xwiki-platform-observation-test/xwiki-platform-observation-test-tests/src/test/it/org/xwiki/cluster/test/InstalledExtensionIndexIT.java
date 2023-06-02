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
package org.xwiki.cluster.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.cache.infinispan.internal.InfinispanCacheFactory;
import org.xwiki.cache.internal.DefaultCacheManager;
import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.cluster.test.framework.AbstractClusterHttpIT;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.internal.ContextComponentManager;
import org.xwiki.component.internal.DocumentComponentManager;
import org.xwiki.component.internal.SpaceComponentManager;
import org.xwiki.component.internal.UserComponentManager;
import org.xwiki.component.internal.WikiComponentManager;
import org.xwiki.component.internal.embed.EmbeddableComponentManagerFactory;
import org.xwiki.component.internal.multi.DefaultComponentManagerManager;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.configuration.internal.DefaultConfigurationSourceProvider;
import org.xwiki.configuration.internal.XWikiPropertiesConfigurationSource;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.job.Request;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rest.internal.JAXBConverter;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.resources.job.JobsResource;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.user.internal.ConfiguredStringUserReferenceSerializer;
import org.xwiki.user.internal.DefaultUserConfiguration;
import org.xwiki.user.internal.document.DocumentStringUserReferenceSerializer;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptorManager;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;
import org.xwiki.xstream.internal.SafeXStream;
import org.xwiki.xstream.internal.XStreamUtils;

import com.xpn.xwiki.doc.DefaultDocumentAccessBridge;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verify the installed extensions are synchronized between members of the cluster.
 *
 * @version $Id$
 */
public class InstalledExtensionIndexIT extends AbstractClusterHttpIT
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDeclaration> componentDeclarations = new ArrayList<>();
        componentDeclarations.add(new ComponentDeclaration(ModelFactory.class.getName()));

        // All deps required by ModelFactory
        componentDeclarations.add(new ComponentDeclaration(DefaultWikiDescriptorManager.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(WikiDescriptorCache.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultCacheManager.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultCacheManagerConfiguration.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultConfigurationSourceProvider.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(InfinispanCacheFactory.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(ConfiguredStringUserReferenceSerializer.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DocumentStringUserReferenceSerializer.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultUserConfiguration.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(ContextComponentManager.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(UserComponentManager.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultDocumentAccessBridge.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(CurrentMixedStringDocumentReferenceResolver.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(CurrentMixedEntityReferenceProvider.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(CompactWikiStringEntityReferenceSerializer.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(CurrentEntityReferenceProvider.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(DocumentComponentManager.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(SpaceComponentManager.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(WikiComponentManager.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(DefaultExecution.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(DefaultComponentManagerManager.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(EmbeddableComponentManagerFactory.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(XWikiPropertiesConfigurationSource.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(StandardEnvironment.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(DefaultConverterManager.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(EnumConverter.class.getName()));
        componentDeclarations.add(
            new ComponentDeclaration(ConvertUtilsConverter.class.getName()));

        componentDeclarations.add(new ComponentDeclaration(JAXBConverter.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(SafeXStream.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(XStreamUtils.class.getName()));
        loader.initialize(AbstractTest.componentManager, AbstractTest.class.getClassLoader(), componentDeclarations);

        // Make sure extension utils is initialized and set.
        if (AbstractClusterHttpIT.getExtensionTestUtils() == null) {
            AllIT.initExtensionTestUtils(AbstractTest.context);
        }
    }

    @Test
    public void testInstallExtensionOnRoot() throws Exception
    {
        // Use superadmin
        AbstractTest.getUtil().setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        ExtensionId extensionId = new ExtensionId("maven:jar", "1.0");

        /////////////////////////////////////////////
        // Make sure it's not yet installed on node 0 and node 1

        AbstractTest.getUtil().switchExecutor(0);
        assertTrue("The extension is already installed on node0",
            !AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT));

        AbstractTest.getUtil().switchExecutor(1);
        assertTrue("The extension is already installed on node1",
            !AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT));

        /////////////////////////////////////////////
        // Install extension on node 0

        AbstractTest.getUtil().switchExecutor(0);

        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), null));
        installRequest.setInteractive(false);
        installRequest.addExtension(extensionId);

        executeJob("install", installRequest);

        /////////////////////////////////////////////
        // Make sure it has been installed on node 0

        assertTrue("The extension was not installed on node0",
            AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT));

        /////////////////////////////////////////////
        // Make sure it has been installed on node 1

        AbstractTest.getUtil().switchExecutor(1);

        long t1 = System.currentTimeMillis();
        while (!AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT)) {
            if (System.currentTimeMillis() - t1 > 10000L) {
                fail("The extension was not installed on node1");
            }
            Thread.sleep(100);
        }

        /////////////////////////////////////////////
        // Uninstall extension from node 1

        AbstractTest.getUtil().switchExecutor(1);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), null));
        uninstallRequest.setInteractive(false);
        uninstallRequest.addExtension(extensionId);

        executeJob("uninstall", uninstallRequest);

        /////////////////////////////////////////////
        // Make sure it has been uninstalled from node 1

        assertTrue("The extension is still installed on node1",
            !AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT));

        /////////////////////////////////////////////
        // Make sure it has been uninstalled from node 0

        AbstractTest.getUtil().switchExecutor(1);

        t1 = System.currentTimeMillis();
        while (AbstractClusterHttpIT.getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT)) {
            if (System.currentTimeMillis() - t1 > 10000L) {
                fail("The extension is still installed on node0");
            }
            Thread.sleep(100);
        }
    }

    private void executeJob(String jobType, Request jobRequest) throws Exception
    {
        JobRequest request =
            AbstractTest.componentManager.<ModelFactory>getInstance(ModelFactory.class).toRestJobRequest(jobRequest);

        Map<String, Object[]> queryParameters = new HashMap<>();
        queryParameters.put("jobType", new Object[]{ jobType });
        queryParameters.put("async", new Object[]{ false });

        TestUtils.assertStatusCodes(
            AbstractTest.getUtil().rest().executePut(JobsResource.class, request, queryParameters), true,
            TestUtils.STATUS_OK);
    }
}
