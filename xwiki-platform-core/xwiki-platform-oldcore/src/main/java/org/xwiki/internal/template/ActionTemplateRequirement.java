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
package org.xwiki.internal.template;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateRequirement;
import org.xwiki.template.TemplateRequirementException;
import org.xwiki.template.TemplateRequirementsException;

import com.xpn.xwiki.XWikiContext;

/**
 * Validate action related template requirement.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 */
@Component
@Singleton
@Named("action")
public class ActionTemplateRequirement implements TemplateRequirement
{
    @Inject
    @Named("readonly")
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void checkRequirement(String requirement, String value, Template template)
        throws TemplateRequirementException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String action = null;
        if (xcontext != null) {
            action = xcontext.getAction();
        }

        if (action == null) {
            action = "";
        }

        if (!value.matches(action)) {
            throw new TemplateRequirementException(TemplateRequirementsException.TRANSLATION_KEY_PREFIX + "action",
                "Action [{}] does not match action requirement [{}].", action, value);
        }
    }
}
