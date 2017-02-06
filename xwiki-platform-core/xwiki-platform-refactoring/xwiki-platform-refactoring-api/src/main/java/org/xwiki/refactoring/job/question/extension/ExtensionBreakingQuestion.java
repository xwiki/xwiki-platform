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
package org.xwiki.refactoring.job.question.extension;

import java.util.List;
import java.util.Map;

import org.xwiki.extension.ExtensionId;
import org.xwiki.refactoring.job.question.AbstractEntityQuestion;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 */
@Unstable
public class ExtensionBreakingQuestion extends AbstractEntityQuestion
{
    private Map<ExtensionId, ExtensionSelection> extensions;

    public ExtensionBreakingQuestion(Map<ExtensionId, ExtensionSelection> extensions,
            List<EntitySelection> entities)
    {
        super(entities);
        this.extensions = extensions;
    }

    public Map<ExtensionId, ExtensionSelection> getExtensions()
    {
        return extensions;
    }

    /**
     * @return pages that do not belong to any extension
     */
    public List<EntitySelection> getFreePages()
    {
        return getEntities();
    }

    public void selectAll(boolean selectAll)
    {
        for (ExtensionSelection extension : extensions.values()) {
            extension.selectAllPages(selectAll);
        }
    }
}
