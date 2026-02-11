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
package org.xwiki.rendering.transformation;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Executes rendering transformations on an XDOM.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Role
@Unstable
public interface TransformationExecutor
{
    /**
     * Sets the execution id.
     * 
     * @param id the execution id
     * @return this instance, for chaining
     * @see TransformationContext#getId()
     */
    TransformationExecutor withId(String id);

    /**
     * Sets the XDOM to transform.
     * 
     * @param xdom the XDOM to transform
     * @return this instance, for chaining
     * @see TransformationContext#getXDOM()
     */
    TransformationExecutor withXDOM(XDOM xdom);

    /**
     * Sets the syntax of the XDOM to transform.
     * 
     * @param syntax the syntax of the XDOM to transform
     * @return this instance, for chaining
     * @see TransformationContext#getSyntax()
     */
    TransformationExecutor withSyntax(Syntax syntax);

    /**
     * Sets the target syntax.
     * 
     * @param targetSyntax the target syntax
     * @return this instance, for chaining
     * @see TransformationContext#getTargetSyntax()
     */
    TransformationExecutor withTargetSyntax(Syntax targetSyntax);

    /**
     * Sets whether the transformations should be executed in restricted mode.
     * 
     * @param restricted whether to execute transformations in restricted mode
     * @return this instance, for chaining
     * @see TransformationContext#isRestricted()
     */
    TransformationExecutor withRestricted(boolean restricted);

    /**
     * Sets the list of transformations to execute.
     * 
     * @param transformationNames the list of transformations to execute
     * @return this instance, for chaining
     * @see TransformationContext#getTransformationNames()
     */
    TransformationExecutor withTransformations(List<String> transformationNames);

    /**
     * Sets the reference of the document whose content is being transformed. Some transformations require specific
     * access rights, which are evaluated for the current user against this document.
     * 
     * @param contentDocumentReference the content document reference
     * @return this instance, for chaining
     */
    TransformationExecutor withContentDocument(DocumentReference contentDocumentReference);

    /**
     * Executes the transformations.
     * 
     * @throws TransformationException if any error occurs during the execution of the transformations
     */
    void execute() throws TransformationException;
}
