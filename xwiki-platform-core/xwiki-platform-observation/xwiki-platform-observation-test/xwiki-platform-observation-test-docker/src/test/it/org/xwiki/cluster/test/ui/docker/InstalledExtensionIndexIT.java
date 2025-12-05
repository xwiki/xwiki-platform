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
package org.xwiki.cluster.test.ui.docker;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.test.junit5.ExtensionTestUtils;
import org.xwiki.job.Request;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.resources.job.JobsResource;
import org.xwiki.test.docker.junit5.Instances;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verify the installed extensions are synchronized between members of the cluster.
 *
 * @version $Id$
 */
@UITest(instances = @Instances(2))
class InstalledExtensionIndexIT
{
    @Test
    void testInstallExtensionOnRoot(TestUtils setup, ComponentManager componentManager,
        ExtensionTestUtils extensionUtils) throws Exception
    {
        // Use superadmin
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        ExtensionId extensionId = new ExtensionId("maven:jar", "1.0");

        /////////////////////////////////////////////
        // Make sure it's not yet installed on node 0 and node 1

        setup.switchExecutor(0);
        assertTrue(!extensionUtils.isInstalled(extensionId, Namespace.ROOT),
            "The extension is already installed on node0");

        setup.switchExecutor(1);
        assertTrue(!extensionUtils.isInstalled(extensionId, Namespace.ROOT),
            "The extension is already installed on node1");

        /////////////////////////////////////////////
        // Install extension on node 0

        setup.switchExecutor(0);

        InstallRequest installRequest = new InstallRequest();
        installRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), null));
        installRequest.setInteractive(false);
        installRequest.addExtension(extensionId);

        executeJob("install", installRequest, setup, componentManager);

        /////////////////////////////////////////////
        // Make sure it has been installed on node 0

        assertTrue(extensionUtils.isInstalled(extensionId, Namespace.ROOT), "The extension was not installed on node0");

        /////////////////////////////////////////////
        // Make sure it has been installed on node 1

        setup.switchExecutor(1);

        long t1 = System.currentTimeMillis();
        while (!extensionUtils.isInstalled(extensionId, Namespace.ROOT)) {
            if (System.currentTimeMillis() - t1 > 10000L) {
                fail("The extension was not installed on node1");
            }
            Thread.sleep(100);
        }

        /////////////////////////////////////////////
        // Uninstall extension from node 1

        setup.switchExecutor(1);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), null));
        uninstallRequest.setInteractive(false);
        uninstallRequest.addExtension(extensionId);

        executeJob("uninstall", uninstallRequest, setup, componentManager);

        /////////////////////////////////////////////
        // Make sure it has been uninstalled from node 1

        assertTrue(!extensionUtils.isInstalled(extensionId, Namespace.ROOT),
            "The extension is still installed on node1");

        /////////////////////////////////////////////
        // Make sure it has been uninstalled from node 0

        setup.switchExecutor(1);

        t1 = System.currentTimeMillis();
        while (extensionUtils.isInstalled(extensionId, Namespace.ROOT)) {
            if (System.currentTimeMillis() - t1 > 10000L) {
                fail("The extension is still installed on node0");
            }
            Thread.sleep(100);
        }
    }

    private void executeJob(String jobType, Request jobRequest, TestUtils setup, ComponentManager componentManager)
        throws Exception
    {
        JobRequest request =
            componentManager.<ModelFactory>getInstance(ModelFactory.class).toRestJobRequest(jobRequest);

        Map<String, Object[]> queryParameters = new HashMap<>();
        queryParameters.put("jobType", new Object[] {jobType});
        queryParameters.put("async", new Object[] {false});

        TestUtils.assertStatusCodes(setup.rest().executePut(JobsResource.class, request, queryParameters), true,
            TestUtils.STATUS_OK);
    }
}
