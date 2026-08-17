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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Controls which host/port is set as the default one in the wiki descriptor, and which is thus used to generate
 * external URLs from background threads, that is when no request is available to get the host/port from.
 * <p>
 * Contrary to {@link UITest#wikiDescriptorTarget()}, this annotation is resolved for each test and can thus be put on a
 * test method, on a test class or on an enclosing test class: the closest declaration wins (the test method first, then
 * the test class and its enclosing classes). This matters because a single XWiki instance is shared by all the test
 * classes executed by a top level test (for example {@code AllIT}), and thus {@link UITest#wikiDescriptorTarget()} can
 * only define a single value for all of them.
 * <p>
 * Use this annotation for a test that requires a specific target, and {@link UITest#wikiDescriptorTarget()} (or the
 * {@code xwiki.test.ui.wikiDescriptorTarget} system property, for example to switch it on the command line for a whole
 * test module) to define the target used by all the tests that don't have this annotation.
 *
 * @version $Id$
 * @since 16.10.19
 * @since 17.10.11
 * @since 18.4.4
 * @since 18.7.0RC1
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, ANNOTATION_TYPE})
public @interface UseWikiDescriptorTarget
{
    /**
     * @return the host/port to set as the default one in the wiki descriptor
     */
    WikiDescriptorTarget value() default WikiDescriptorTarget.HTTP_CLIENT;
}
