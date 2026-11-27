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
package org.xwiki.store.hibernate.internal.patcher;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated type is a mixin whose methods should be merged into a target class or interface by
 * {@link HibernatePatcher}.
 * <p>
 * There are two kinds of mixins:
 * <ul>
 * <li>an interface extending exactly one interface (the target) and without {@link #target()}: all its methods are
 * copied into the target and all the references to the mixin are replaced by references to the target</li>
 * <li>a class indicating its {@link #target()} (which can be a final or sealed class): each of its static methods
 * taking the target as first parameter becomes an instance method of the target without that first parameter (the
 * bytecode is the same since the first parameter and {@code this} are both stored in the first local variable)</li>
 * </ul>
 *
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Mixin
{
    /**
     * @return the generic signatures (as found in a class file, for example {@code Lorg/hibernate/Query<TR;>;}) of the
     *         interfaces to add to the target super interfaces
     */
    String[] interfaces() default {};

    /**
     * @return the target of a static mixin
     */
    Class<?> target() default void.class;
}
