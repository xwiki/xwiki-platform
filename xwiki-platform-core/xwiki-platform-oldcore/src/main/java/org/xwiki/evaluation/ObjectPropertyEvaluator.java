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

import com.xpn.xwiki.objects.BaseObject;

/**
 * Evaluates the properties of an object and returns a Map that stores the evaluation results, in which keys are the
 * field names of the properties, and values their evaluated content.
 * Instances of this interface should be used as helpers by implementations of {@link ObjectEvaluator}.
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.2
 */
@Role
public interface ObjectPropertyEvaluator
{
    /**
     * Evaluates the properties of an object.
     *
     * @param object the object to evaluate
     * @param properties the names of the properties to evaluate
     * @return a Map storing the evaluated properties
     * @throws ObjectEvaluatorException if the evaluation fails
     */
    Map<String, String> evaluateProperties(BaseObject object, String... properties) throws ObjectEvaluatorException;
}
