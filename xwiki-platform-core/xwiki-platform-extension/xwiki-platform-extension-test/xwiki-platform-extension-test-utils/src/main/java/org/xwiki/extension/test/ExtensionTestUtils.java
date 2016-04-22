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
package org.xwiki.extension.test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Utility methods for extension manager functional tests.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionTestUtils
{
    /**
     * The key used to store an instance of this class in the context.
     */
    public final static String PROPERTY_KEY = "extensionUtils";

    /**
     * The reference of the service page.
     */
    private static final LocalDocumentReference SERVICE_REFERENCE = new LocalDocumentReference("ExtensionTest", "Service");

    /**
     * The generic test utility methods.
     */
    private final TestUtils utils;

    /**
     * Creates a new instance.
     * 
     * @param utils the generic test utility methods
     */
    public ExtensionTestUtils(TestUtils utils)
    {
        this.utils = utils;

        // Create the service page if it does not exist
        try (InputStream extensionTestService = this.getClass().getResourceAsStream("/extensionTestService.wiki")) {
            // Make sure to save the service with superadmin
            UsernamePasswordCredentials currentCredentials =
                utils.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

            // Save the service
            utils.rest().savePage(SERVICE_REFERENCE, IOUtils.toString(extensionTestService), "");

            // Restore previous credentials
            utils.setDefaultCredentials(currentCredentials);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup the service", e);
        }
    }

    /**
     * Uninstalls the specified extension removing it also from the local cache.
     * 
     * @param extensionId the id of the extension to uninstall
     */
    public void uninstall(String extensionId)
    {
        uninstall(extensionId, false);
    }

    /**
     * Uninstalls the specified extension, optionally removing it from the local cache.
     * 
     * @param extensionId the id of the extension to uninstall
     * @param keepLocalCache whether to keep the local cached extension or not
     */
    public void uninstall(String extensionId, boolean keepLocalCache)
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("extensionId", extensionId);
        parameters.put("keepLocalCache", String.valueOf(keepLocalCache));
        doAction("uninstall", parameters);
    }

    /**
     * Installs the specified extension.
     * 
     * @param extensionId the id of the extension to install
     */
    public void install(ExtensionId extensionId)
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("extensionId", extensionId.getId());
        parameters.put("extensionVersion", extensionId.getVersion().getValue());
        doAction("install", parameters);
    }

    /**
     * Finishes the current job if there is one and its current state is WAITING.
     */
    public void finishCurrentJob()
    {
        doAction("finish", new HashMap<String, String>());
    }

    private void doAction(String action, Map<String, String> parameters)
    {
        parameters.put("action", action);
        parameters.put("outputSyntax", "plain");
        utils.gotoPage(SERVICE_REFERENCE, "get", parameters);
        utils.getDriver().waitUntilElementHasTextContent(By.tagName("body"), "Done!");
    }
}
