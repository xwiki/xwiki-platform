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
package com.xpn.xwiki.internal.template;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent.UniqueContext;

/**
 * Manipulate template related data stored in the context.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component(roles = TemplateContext.class)
@Singleton
public class TemplateContext
{
    static final String TEMPLATES = "template.templates";

    @Inject
    private Execution execution;

    /**
     * @param template the template to search for
     * @return true if the passed template is supposed to be unique and already been executed
     */
    public boolean isExecuted(Template template)
    {
        UniqueContext unique;
        try {
            unique = template.getContent().getUnique();
        } catch (Exception e) {
            // Does not make much sense at this level
            return false;
        }

        ExecutionContext executionContext = this.execution.getContext();

        if (executionContext != null) {
            Set<String> templates = (Set<String>) executionContext.getProperty(TEMPLATES);

            if (templates != null && unique == UniqueContext.REQUEST) {
                return !templates.add(template.getId());
            }
        }

        return false;
    }
}
