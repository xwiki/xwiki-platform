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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.ExtensionId;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;

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
    public static final String PROPERTY_KEY = "extensionUtils";

    /**
     * The reference of the service page.
     */
    private static final LocalDocumentReference SERVICE_REFERENCE =
        new LocalDocumentReference("ExtensionTest", "Service");

    /**
     * The generic test utility methods.
     */
    private final TestUtils utils;

    private final Map<Integer, Boolean> initialized = new ConcurrentHashMap<>();

    private XWikiCredentials adminCredentials = TestUtils.SUPER_ADMIN_CREDENTIALS;

    /**
     * Creates a new instance.
     * 
     * @param utils the generic test utility methods
     */
    public ExtensionTestUtils(TestUtils utils)
    {
        this.utils = utils;
    }

    /**
     * Creates a new instance.
     * 
     * @param utils the generic test utility methods
     * @param adminCredentials the admin credentials to use
     */
    public ExtensionTestUtils(TestUtils utils, XWikiCredentials adminCredentials)
    {
        this.utils = utils;
        this.adminCredentials = adminCredentials;
    }

    private void checkinit() throws Exception
    {
        int port = this.utils.getCurrentExecutor().getPort();

        if (!this.initialized.containsKey(port)) {
            // Create the service page if it does not exist
            try (InputStream extensionTestService = this.getClass().getResourceAsStream("/extensionTestService.wiki")) {
                // Make sure to save the service with superadmin
                XWikiCredentials currentCredentials =
                    this.utils.setDefaultCredentials(this.adminCredentials);

                // Save the service
                this.utils.rest().savePage(SERVICE_REFERENCE,
                    IOUtils.toString(extensionTestService, StandardCharsets.UTF_8), "");

                // Restore previous credentials
                this.utils.setDefaultCredentials(currentCredentials);
            }

            this.initialized.put(port, true);
        }
    }

    /**
     * Uninstalls the specified extension removing it also from the local cache.
     * 
     * @param extensionId the id of the extension to uninstall
     */
    public void uninstall(String extensionId) throws Exception
    {
        uninstall(extensionId, false);
    }

    /**
     * Uninstalls the specified extension, optionally removing it from the local cache.
     * 
     * @param extensionId the id of the extension to uninstall
     * @param keepLocalCache whether to keep the local cached extension or not
     */
    public void uninstall(String extensionId, boolean keepLocalCache) throws Exception
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("extensionId", extensionId);
        parameters.put("keepLocalCache", String.valueOf(keepLocalCache));

        doAction("uninstall", parameters);
    }

    /**
     * Installs the specified extension.
     * 
     * @param extensionId the id of the extension to install
     */
    public void install(ExtensionId extensionId) throws Exception
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("extensionId", extensionId.getId());
        parameters.put("extensionVersion", extensionId.getVersion().getValue());

        doAction("install", parameters);
    }

    /**
     * Finishes the current job if there is one and its current state is WAITING.
     */
    public void finishCurrentJob() throws Exception
    {
        doAction("finish", new HashMap<String, String>());
    }

    public boolean isInstalled(ExtensionId extensionId) throws Exception
    {
        return isInstalled(extensionId, null);
    }

    public boolean isInstalled(ExtensionId extensionId, Namespace namespace) throws Exception
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("extensionId", extensionId.getId());
        if (extensionId.getVersion() != null) {
            parameters.put("extensionVersion", extensionId.getVersion().getValue());
        }
        if (namespace != null) {
            String namespaceString = namespace.serialize();
            parameters.put("extensionNamespace", namespaceString != null ? namespaceString : "");
        }

        return execute("is_installed", parameters).equals("true");
    }

    private void doAction(String action, Map<String, String> parameters) throws Exception
    {
        assertEquals("Done!", execute(action, parameters));
    }

    private String execute(String action, Map<String, String> parameters) throws Exception
    {
        checkinit();

        parameters.put("action", action);
        parameters.put("outputSyntax", "plain");

        return this.utils.executeAndGetBodyAsString(SERVICE_REFERENCE, parameters);
    }
}
