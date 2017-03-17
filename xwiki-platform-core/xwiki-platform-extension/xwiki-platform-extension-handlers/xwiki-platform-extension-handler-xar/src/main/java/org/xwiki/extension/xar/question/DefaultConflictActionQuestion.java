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
package org.xwiki.extension.xar.question;

import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.question.ConflictQuestion.ConflictType;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;

/**
 * @version $Id$
 * @since 9.2RC1
 */
public class DefaultConflictActionQuestion
{
    private final PackageConfiguration configuration;

    /**
     * @param configuration the configuration
     */
    public DefaultConflictActionQuestion(PackageConfiguration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @param type the type of conflict
     * @return the default action for the passed type of conflict
     */
    public GlobalAction getConflictAction(ConflictType type)
    {
        return this.configuration.getConflictAction(type);
    }

    /**
     * @param type the type of conflict
     * @param action the default action for the passed type of conflict
     */
    public void setConflictAction(ConflictType type, GlobalAction action)
    {
        this.configuration.setConflictAction(type, action, action);
    }
}
