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
 *
 */
package org.xwiki.component.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines a field or method that needs to be injected with a component.
 * A hint can be specified to choose which implementation to use.
 * 
 * @version $Id$
 * @since 1.8.1
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
public @interface Requirement
{
    /**
     * The hint value selecting a specific component implementation to use.
     */
    String value() default "";

    /**
     * The role for the component to inject. Note that the role can usually be guessed from the field
     * type or method parameter type. However, for Lists or Maps we need to pass it since Java doesn't
     * allow getting it using reflection.
     * @deprecated since 2.7RC1 generic types are guessed too
     */
    @Deprecated
    Class< ? > role() default Object.class;

    /**
     * When injecting a Collection of requirements, allows specifying a discrete list of hints to use. If these are
     * not specified, then all implementations for the specified role will be injected.
     */
    String[] hints() default { };
}
