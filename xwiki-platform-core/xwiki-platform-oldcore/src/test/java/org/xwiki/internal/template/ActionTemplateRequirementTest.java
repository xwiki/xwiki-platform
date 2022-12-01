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

import org.junit.jupiter.api.Test;
import org.xwiki.template.TemplateRequirementException;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate {@link ActionTemplateRequirement}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class ActionTemplateRequirementTest
{
    @InjectMockComponents
    private ActionTemplateRequirement requirement;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void checkRequirementWithEmptyRequirement() throws TemplateRequirementException
    {
        this.requirement.checkRequirement("action", "", null);

        this.oldcore.getXWikiContext().setAction("action");

        assertThrows(TemplateRequirementException.class, () -> this.requirement.checkRequirement("action", "", null));
    }

    @Test
    void checkRequirementWithSimpleRequirement() throws TemplateRequirementException
    {
        assertThrows(TemplateRequirementException.class,
            () -> this.requirement.checkRequirement("action", "action", null));

        this.oldcore.getXWikiContext().setAction("action");

        this.requirement.checkRequirement("action", "action", null);

        this.oldcore.getXWikiContext().setAction("action2");

        assertThrows(TemplateRequirementException.class,
            () -> this.requirement.checkRequirement("action", "action", null));
    }

    @Test
    void checkRequirementWithRegexRequirement() throws TemplateRequirementException
    {
        this.oldcore.getXWikiContext().setAction(".*");

        this.requirement.checkRequirement("action", "", null);
        this.requirement.checkRequirement("action", "action", null);
    }
}
