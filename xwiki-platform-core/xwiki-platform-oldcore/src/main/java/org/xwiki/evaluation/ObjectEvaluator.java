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
package org.xwiki.evaluation;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.evaluation.internal.DefaultObjectEvaluator;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Evaluates the properties of an object and returns a Map that stores the evaluation results, in which keys are the
 * field names of the properties, and values their evaluated content.
 * Implement an instance with a hint corresponding to the class name of the object you want to evaluate and use
 * {@link DefaultObjectEvaluator} that will proxy the calls to the right implementation.
 * This ensures that the properties being evaluated are only the ones referenced explicitly by the provided
 * implementation, in order to avoid accidental evaluation of properties that should only be used in specific contexts.
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.2
 */
@Role
public interface ObjectEvaluator
{
    /**
     * Evaluates the properties of an object.
     *
     * @param object the object to evaluate
     * @return a Map storing the evaluated properties
     * @throws ObjectEvaluatorException if the evaluation fails
     */
    Map<String, String> evaluate(BaseObject object) throws ObjectEvaluatorException;
}
