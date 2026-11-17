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
package org.xwiki.uiextension.internal.parameters;

import org.xwiki.velocity.XWikiVelocityException;

/**
 * The value of a single UI extension parameter, prepared once so that it can be evaluated cheaply on each request.
 * <p>
 * A value without any Velocity is returned as is, a value whose only Velocity is a call that can be executed directly
 * in Java skips the Velocity engine, and only the other values are evaluated with Velocity.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public sealed interface UIXParameterValue permits StaticParameterValue, ShortcutParameterValue, VelocityParameterValue
{
    /**
     * @return the value as written in the UI extension object, which is also the value used when the author of the UI
     *     extension isn't allowed to execute scripts or when the evaluation fails
     */
    String raw();

    /**
     * @return {@code true} if the value contains no script, in which case the evaluation always returns {@link #raw()}
     */
    default boolean isStatic()
    {
        return false;
    }

    /**
     * @return {@code true} if {@link #evaluate(VelocityParameterSession)} needs a Velocity session, which also means
     *     that the evaluation needs to be executed with the rights of the author of the UI extension
     */
    default boolean requiresVelocity()
    {
        return false;
    }

    /**
     * Evaluates the value. The caller is responsible for checking that the author of the UI extension has script
     * right.
     *
     * @param session the Velocity session to use, only required (and then not {@code null}) when
     *     {@link #requiresVelocity()} returns {@code true}
     * @return the evaluated value
     * @throws XWikiVelocityException when the Velocity evaluation fails
     */
    String evaluate(VelocityParameterSession session) throws XWikiVelocityException;
}
