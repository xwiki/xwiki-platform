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
    Browser browser() default Browser.SYSTEM;

    /**
     * @return the database to use, see {@link Database}
     * @since 10.9
     */
    Database database() default Database.SYSTEM;

    /**
     * @return the Servlet Engine to use, see {@link ServletEngine}
     * @since 10.9
     */
    ServletEngine servletEngine() default ServletEngine.SYSTEM;

    /**
     * @return true if the test should output verbose console logs or not
     * @since 10.10RC1
     */
    boolean verbose() default false;

    /**
     * @return true if the database data should be mapped to a local directory on the host computer so that it can
     *         be saved and reused for another run
     * @since 10.10RC1
     */
    boolean saveDatabaseData() default false;

    /**
     * @return true if the Maven resolving is done in offline mode (i.e. you need to have the required artifacts in
     *         your local repository). False by default to avoid developer problems but should be set to true in the
     *         CI to improve performance of functional tests
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
}
