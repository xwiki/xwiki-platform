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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Method;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Execute a {@link XDOM} tree and returns a result matching the return value of a given {@link Method}.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface WikiComponentMethodExecutor
{
    /**
     * The key under which inputs are put in the method context.
     */
    String INPUT_KEY = "input";

    /**
     * The key under which the output is stored in the method context.
     */
    String OUTPUT_KEY = "output";

    /**
     * Execute a {@link XDOM} and returns a value retrieved from the XDOM, retrieved from the
     * {@link org.xwiki.component.wiki.internal.WikiMethodOutputHandler} set in the method context under the OUTPUT_KEY
     * key, or from the rendered content itself. In this last case the rendered content is transformed in order to match
     * the return type of the passed Method.
     *
     * @param method The method to match the return value
     * @param args The arguments passed to the method
     * @param componentDocumentReference The reference to the component Document
     * @param xdom The XDOM mimicking the method
     * @param syntax The syntax of the XDOM
     * @param methodContext A map of key/value pairs to put in the context before executing the XDOM
     * @return A value matching the return type of the passed method
     */
    Object execute(Method method, Object[] args, DocumentReference componentDocumentReference,  XDOM xdom,
        Syntax syntax, Map<String, Object> methodContext);
}
