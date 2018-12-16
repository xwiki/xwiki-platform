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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a test as being a functional UI Test.
 *
 * @version $Id$
 * @since 10.6RC1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ExtendWith(XWikiDockerExtension.class)
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
     * @return true if the database data should be mapped to a local directory on the host computer so that it can be
     * saved and reused for another run
     * @since 10.10RC1
     */
    boolean saveDatabaseData() default false;

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
     * depending on the database)
     * @since 10.10RC1
     */
    String jdbcDriverVersion() default "";

    /**
     * @return true if VNC container is started and recording is done and saved on test exit
     * @since 10.10RC1
     */
    boolean vnc() default true;

    /**
     * @return the list of configuration properties to use when generating the XWiki configuration files such as as
     * {@code xwiki.properties} (check {@code xwiki.properties.vm} to find the list of supported properties)
     * @since 10.10RC1
     */
    String[] properties() default {};

    /**
     * @return the optional list of JARs (specified as artifact coordinates {@code groupId:artifactId}) that should be
     *         added to {@code WEB-INF/lib}. For example this is useful for artifacts containing Hibernate mapping files
     *         since those cannot currently be loaded as extensions.
     * @since 10.11RC1
     */
    String[] extraJARs() default {};

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
     * @return true if an office docker container must be provided to run the tests.
     * @since 10.11RC1
     */
    boolean office() default false;

    /**
     * @return the list of Servlet Engines on which this test must not be executed. If the Servlet Engine is selected
     *         then the test will be skipped
     * @since 10.11RC1
     */
    ServletEngine[] forbiddenEngines() default {};
}
