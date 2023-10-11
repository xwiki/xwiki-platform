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
package org.xwiki.rendering.macro.velocity.filter;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Role;
import org.xwiki.velocity.VelocityTemplate;

/**
 * Component called before and after velocity execution in velocity macro to modify the content to execute or parse. It
 * can be used for example to reformat the velocity content to take care of indentation etc.
 * 
 * @version $Id$
 */
@Role
public interface VelocityMacroFilter
{
    /**
     * Called just before the velocity script is executed.
     * 
     * @param content the velocity script
     * @param velocityContext the velocity context. this method can inject here bindings usable in the script like
     *            <code>$somebinding</code>.
     * @return the modified content
     */
    String before(String content, VelocityContext velocityContext);

    /**
     * @return true of the filter support preparation
     * @since 15.9RC1
     */
    default boolean isPreparationSupported()
    {
        return false;
    }

    /**
     * Called just before the velocity script is executed in the case of a prepared macro.
     * 
     * @param content the velocity script
     * @param velocityContext the velocity context. this method can inject here bindings usable in the script like
     *            <code>$somebinding</code>.
     * @since 15.9RC1
     */
    default void before(VelocityTemplate content, VelocityContext velocityContext)
    {

    }

    /**
     * @param content the velocity script
     * @return the modified content
     * @since 15.9RC1
     */
    default String prepare(String content)
    {
        return content;
    }

    /**
     * Called just before the result of the velocity script execution is parsed.
     * 
     * @param content the result of the velocity script execution
     * @param velocityContext the velocity context. This method usually clean the bindings injected in the velocity
     *            context by {@link #before(String, VelocityContext)}
     * @return the modified content
     */
    String after(String content, VelocityContext velocityContext);
}
