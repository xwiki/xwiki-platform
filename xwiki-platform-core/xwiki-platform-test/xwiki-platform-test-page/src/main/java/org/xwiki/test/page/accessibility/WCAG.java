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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.xwiki.test.page.PageTest;

/**
 * Enables the automatic WCAG validation of the pages rendered during a page test.
 * <p>
 * When a test class (or one of its methods) carries this annotation, every page rendered through
 * {@link PageTest#renderPage} or {@link PageTest#renderHTMLPage} during the test is collected, and validated at
 * the end of the test with the same engine as {@link PageTest#assertWCAG}: an {@link AssertionError} listing all the violations of all
 * the rendered pages is thrown when any of them is violated.
 * <p>
 * Note that the validation only runs when the axe-core bridge of the page test framework is available; see
 * {@link WCAGCustomizer} for the rules taken into account and the way to configure them.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface WCAG
{
    /**
     * @return {@code false} to disable the validation, {@code true} otherwise (default). On a method, takes
     *     precedence over the value set on the enclosing class.
     */
    boolean enabled() default true;
}
