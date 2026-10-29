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
package org.xwiki.test.page.accessibility;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The locations the axe-core Node.js bridge needs to run: a Node.js executable, and the directory holding the
 * {@code package.json} and {@code node_modules} with the {@code axe-core} and {@code jsdom} dependencies. Both
 * are normally installed by the frontend-maven-plugin of {@code xwiki-platform-test-page}.
 * <p>
 * They are located, in this order:
 * <ul>
 *   <li>the {@code xwiki.testPage.wcag.nodeBinary} and {@code xwiki.testPage.wcag.npmRoot} system properties,
 *       pointing respectively to the Node.js executable and to the directory holding the
 *       {@code package.json}/{@code node_modules} of the framework</li>
 *   <li>the module being built, when the tests of {@code xwiki-platform-test-page} itself are running
 *       ({@code target/node} and {@code src/main/node})</li>
 * </ul>
 *
 * @param node the Node.js executable
 * @param npmRoot the directory holding the {@code package.json} and {@code node_modules} of the framework
 * @version $Id$
 * @since 18.10.0RC1
 */
record AxeBridgeEnvironment(Path node, Path npmRoot)
{
    private static final String NODE_BINARY_PROPERTY = "xwiki.testPage.wcag.nodeBinary";

    private static final String NPM_ROOT_PROPERTY = "xwiki.testPage.wcag.npmRoot";

    private static final String NODE_NAME = "node";

    private static final String NODE_MODULES_NAME = "node_modules";

    /**
     * Resolves the environment to spawn the axe-core Node.js bridge with.
     *
     * @return the resolved environment
     * @throws IOException with a human readable message explaining what is missing
     */
    static AxeBridgeEnvironment resolve() throws IOException
    {
        Path moduleClasses = findModuleClasses();

        Path npmRoot = resolveNpmRoot(moduleClasses);
        requireDirectory(npmRoot.resolve(NODE_MODULES_NAME).resolve("axe-core"),
            String.format("the axe-core npm dependency is not installed in [%s]", npmRoot));
        requireDirectory(npmRoot.resolve(NODE_MODULES_NAME).resolve("jsdom"),
            String.format("the jsdom npm dependency is not installed in [%s]", npmRoot));

        return new AxeBridgeEnvironment(resolveNode(moduleClasses), npmRoot);
    }

    /**
     * @return the directory holding the classes of the module when it is being built ({@code <basedir>/target/
     *     classes}), or {@code null} when the class is loaded from a jar of the Maven repository instead
     */
    private static Path findModuleClasses()
    {
        try {
            Path classes = Path.of(AxeBridgeEnvironment.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI());
            return Files.isDirectory(classes) && classes.getParent() != null ? classes : null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    private static Path resolveNpmRoot(Path moduleClasses) throws IOException
    {
        String property = System.getProperty(NPM_ROOT_PROPERTY);
        if (property != null) {
            return Path.of(property);
        }
        if (moduleClasses != null) {
            return moduleClasses.getParent().getParent().resolve("src").resolve("main").resolve(NODE_NAME);
        }
        throw new IOException(String.format("No location known for the npm dependencies of the WCAG validation: "
            + "the [%s] system property must point to the directory holding the package.json and node_modules "
            + "of xwiki-platform-test-page", NPM_ROOT_PROPERTY));
    }

    private static Path resolveNode(Path moduleClasses) throws IOException
    {
        String property = System.getProperty(NODE_BINARY_PROPERTY);
        if (property != null) {
            Path node = Path.of(property);
            if (!Files.isExecutable(node)) {
                throw new IOException(String.format("The Node.js executable [%s] does not exist or is not "
                    + "executable", node));
            }
            return node;
        }
        if (moduleClasses != null) {
            // The frontend-maven-plugin installs the Node.js executable of the module in <basedir>/target/node.
            Path node = moduleClasses.getParent().resolve(NODE_NAME).resolve(NODE_NAME);
            if (Files.isExecutable(node)) {
                return node;
            }
        }
        // Last resort: a Node.js executable from the PATH, validated when the process is spawned.
        return Path.of(NODE_NAME);
    }

    private static void requireDirectory(Path directory, String message) throws IOException
    {
        if (!Files.isDirectory(directory)) {
            throw new IOException(message);
        }
    }
}
