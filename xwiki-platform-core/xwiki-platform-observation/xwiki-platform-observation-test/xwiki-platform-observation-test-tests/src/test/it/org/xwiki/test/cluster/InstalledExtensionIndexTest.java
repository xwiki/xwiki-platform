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
package org.xwiki.test.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.rest.internal.JAXBConverter;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.resources.job.JobsResource;
import org.xwiki.test.cluster.framework.AbstractClusterHttpTest;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verify the installed extensions are synchronized between members of the cluster.
 * 
 * @version $Id$
 */
public class InstalledExtensionIndexTest extends AbstractClusterHttpTest
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDeclaration> componentDeclarations = new ArrayList<>();
        componentDeclarations.add(new ComponentDeclaration(ModelFactory.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(JAXBConverter.class.getName()));
        loader.initialize(AbstractTest.componentManager, AbstractTest.class.getClassLoader(), componentDeclarations);

        // Make sure extension utils is initialized and set.
        if (getExtensionTestUtils() == null) {
            AllTests.initExtensionTestUtils(context);
        }
    }

    @Test
    public void testInstallExtensionOnRoot() throws Exception
    {
        // Use superadmin
        getUtil().setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        ExtensionId extensionId = new ExtensionId("maven:jar", "1.0");

        /////////////////////////////////////////////
        // Install extension on node 0

        getUtil().switchExecutor(0);

        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, extensionId.getId(), null));
        installRequest.setInteractive(false);
        installRequest.addExtension(extensionId);
        installRequest.setRootModificationsAllowed(true);

        JobRequest request = AbstractTest.componentManager.<ModelFactory>getInstance(ModelFactory.class)
            .toRestJobRequest(installRequest);

        Map<String, Object[]> queryParameters = new HashMap<>();
        queryParameters.put("jobType", new Object[] { "install" });
        queryParameters.put("async", new Object[] { false });

        TestUtils.assertStatusCodes(getUtil().rest().executePut(JobsResource.class, request, queryParameters), true,
            TestUtils.STATUS_OK);

        /////////////////////////////////////////////
        // Make sure it has been installed on node 0

        assertTrue("The extension was not installed on node0",
            getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT));

        /////////////////////////////////////////////
        // Make sure it has been installed on node 1

        getUtil().switchExecutor(1);

        long t1 = System.currentTimeMillis();
        long t2;
        while (!getExtensionTestUtils().isInstalled(extensionId, Namespace.ROOT)) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                fail("The extension was not installed on node1");
            }
            Thread.sleep(100);
        }
    }
}
