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
package com.xpn.xwiki.internal.velocity;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiException;
import org.xwiki.component.annotation.Role;

/**
 * Evaluate content with Velocity.
 *
 * @version $Id$
 * @since 9.11RC1
 */
@Role
public interface VelocityEvaluator
{
    /**
     * Evaluate a template with Velocity.
     *
     * @param content the Velocity content to evaluate
     * @param namespace the namespace under which to evaluate it (used for isolation)
     * @param vcontext the Velocity context to use when evaluating. If {@code null}, then a new context will be created,
     *            initialized and used.
     * @return the evaluated content
     * @throws XWikiException if an error occurs
     */
    String evaluateVelocity(String content, String namespace, VelocityContext vcontext) throws XWikiException;
}
