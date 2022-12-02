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
package org.xwiki.template;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Component in charge of validating the a requirement for a template.
 * <p>
 * This requirement is generally expressed using the template content properties syntax as
 * {@code ##!require.<hint>=<value>}. For example if the following template will trigger a call to
 * {@link TemplateRequirement#checkRequirement(String, String, Template)} with the requirement key "my.requirement" and
 * value "expected value":
 * 
 * <pre>
 * ##!require.my.requirement=expected value
 * </pre>
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 */
@Role
@Unstable
public interface TemplateRequirement
{
    /**
     * @param requirementKey the requirement key to check
     * @param value the requirement value to check
     * @param template the template on which the requirement applies
     * @throws TemplateRequirementException when the requirement is not met or cannot be evaluated
     */
    void checkRequirement(String requirementKey, String value, Template template) throws TemplateRequirementException;
}
