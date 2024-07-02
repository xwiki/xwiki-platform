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
package org.xwiki.test.docker.junit5;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.test.docker.internal.junit5.MultiUserTestUtilsParameterResolver;
import org.xwiki.test.docker.internal.junit5.TestLocalReferenceParameterResolver;
import org.xwiki.test.docker.internal.junit5.TestReferenceParameterResolver;
import org.xwiki.test.docker.internal.junit5.XWikiDockerExtension;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.junit5.ValidateConsoleExtension;

/**
 * Marks a test as being a functional UI Test.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ExtendWith(ValidateConsoleExtension.class)
@ExtendWith(XWikiDockerExtension.class)
@ExtendWith(TestReferenceParameterResolver.class)
@ExtendWith(TestLocalReferenceParameterResolver.class)
@ExtendWith(MultiUserTestUtilsParameterResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public @interface UITest
{
    /**
     * @return the browser to use, see {@link Browser}
     */
    Browser browser() default Browser.FIREFOX;

    /**
     * @return the database to use, see {@link Database}
     * @since 10.9
     */
    Database database() default Database.HSQLDB_EMBEDDED;

    /**
     * @return the Servlet Engine to use, see {@link ServletEngine}
     * @since 10.9
     */
    ServletEngine servletEngine() default ServletEngine.JETTY_STANDALONE;

    /**
     * @return true if the test should output verbose console logs or not
     * @since 10.10RC1
     */
    boolean verbose() default false;

    /**
     * @return true if the test should output debug console logs or not
     * @since 10.11RC1
     */
    boolean debug() default false;

    /**
     * @return true if the Maven resolving is done in offline mode (i.e. you need to have the required artifacts in your
     * local repository). False by default to avoid developer problems but should be set to true in the CI to improve
     * performance of functional tests
     * @since 10.10RC1
     */
    boolean offline() default false;

    /**
     * @return the docker image tag to use (if not specified, uses the "latest" tag)
     * @since 10.10RC1
     */
    String servletEngineTag() default "";

    /**
     * @return the docker image tag to use (if not specified, uses the default from TestContainers)
     * @since 10.10RC1
     */
    String databaseTag() default "";

    /**
     * @return the version of the JDBC driver to use for the selected database (if not specified, uses a default version
     *         depending on the database)
     * @since 10.10RC1
     */
    String jdbcDriverVersion() default "";

    /**
     * @return true if VNC container is started and recording is done and saved on test exit
     * @since 10.10RC1
     */
    boolean vnc() default true;

    /**
     * @return true if WCAG tests should be executed, false otherwise
     */
    boolean wcag() default false;

    /**
     * @return {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     * @since 16.1.0
     */
    boolean wcagStopOnError() default true;

    /**
     * @return the list of configuration properties to use when generating the XWiki configuration files such as
     *         {@code xwiki.properties} with Velocity (check {@code xwiki.properties.vm} to find the list of supported
     *         properties)
     * @since 10.10RC1
     */
    String[] properties() default {};

    /**
     * @return the properties to overwrite in the generated extensions descriptors
     * @since 11.6RC1
     */
    ExtensionOverride[] extensionOverrides() default {};

    /**
     * @return the optional list of JARs (specified as artifact coordinates {@code groupId:artifactId}) that should be
     *         added to {@code WEB-INF/lib}. For example this is useful for artifacts containing Hibernate mapping files
     *         since those cannot currently be loaded as extensions.
     * @since 10.11RC1
     */
    String[] extraJARs() default {};

    /**
     * @return true if the specified extra JAR versions must be resolved (when they're not specified), by checking the
     *     transitive list of dependencies in the current POM. Note that there are 2 limitations: resolving takes time
     *     and SNAPSHOT versions will be resolved to the latest published SNAPSHOT. This is why it's off by default.
     *     When false the current POM version will be used for the missing extra JAR versions. The main use case for
     *     using true is in contrib extensions where the current POM version usually don't match extra JARs versions and
     *     you may not want to hardcode the version and thus you can let the docker test framework find it for you.
     * @since 12.5RC1
     */
    boolean resolveExtraJARs() default false;

    /**
     * @return the list of ports that should be SSH-forwarded when connecting from a Docker container to the
     *         host (i.e. when using the {@code host.testcontainers.internal} host name). This is in addition to port
     *         {@code 8080} which is always added. For example if you need XWiki to send a mail to a SMTP server
     *         running on port 3025 on the host, you should add port 3025 to the list.
     * @since 10.11RC1
     */
    int[] sshPorts() default {};

    /**
     * @return the list of Maven profiles to activate when resolving dependencies for the current POM.
     * @since 10.11RC1
     */
    String[] profiles() default {};

    /**
     * Note: The version of LibreOffice is read from the current {@code pom.xml}.
     *
     * @return true if LibreOffice must be installed in the Servlet Container.
     * @since 10.11RC1
     * @see org.xwiki.test.docker.internal.junit5.servletengine.ServletContainerExecutor
     */
    boolean office() default false;

    /**
     * @return the list of Servlet Engines on which this test must not be executed. If the Servlet Engine is selected
     *         then the test will be skipped
     * @since 10.11RC1
     */
    ServletEngine[] forbiddenEngines() default {};

    /**
     * @return the list of database docker commands to use and that will override default commands (example of command
     *         {@code character-set-server=utf8mb4}
     * @since 11.2RC1
     */
    String[] databaseCommands() default {};

    /**
     * @return true if the database data should be mapped to a local directory on the host computer so that it can be
     *         saved and reused for another run
     * @since 10.10RC1
     */
    boolean saveDatabaseData() default false;

    /**
     * @return true if the XWiki permanent directory should be preserved after the test is finished and the XWiki
     *         container stopped (doesn't make sense for Servlet containers running outside of Docker). Can be useful
     *         for debugging purposes
     * @since 14.5
     */
    boolean savePermanentDirectoryData() default false;

    /**
     * @return the list of network aliases to use for the servlet engine Docker container; this is useful when you need
     *         to access the same XWiki instance using different domains, e.g. because you need to login with different
     *         XWiki users in the same browser instance (but different tabs).
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    String[] servletEngineNetworkAliases() default {};
}
