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
package org.xwiki.refactoring.job.question;

import java.util.List;

import org.xwiki.stability.Unstable;

/**
 * Question that a refactoring job could ask with a list of entities the user can select or un-select for the
 * refactoring.
 *
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class AbstractEntityQuestion
{
    private List<EntitySelection> entities;

    private boolean selectAll = false;

    public AbstractEntityQuestion(List<EntitySelection> entities)
    {
        this.entities = entities;
    }

    public List<EntitySelection> getEntities()
    {
        return entities;
    }

    public boolean isSelectAll()
    {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll)
    {
        this.selectAll = selectAll;
    }
}
