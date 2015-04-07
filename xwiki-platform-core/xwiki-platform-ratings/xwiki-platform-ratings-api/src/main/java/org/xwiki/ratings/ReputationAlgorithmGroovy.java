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
package org.xwiki.ratings;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Algorithm to calculate a user's reputation which is loaded from a page containing a groovy script.
 * 
 * @version $Id$
 * @since 6.4M3
 */
@Unstable
public interface ReputationAlgorithmGroovy extends ReputationAlgorithm
{
    /**
     * Sets current ratings manager.
     * 
     * @param rManager the current RatingsManager
     */
    void setRatingsManager(RatingsManager rManager);

    /**
     * Sets the current execution context.
     * 
     * @param execution the current execution manager
     */
    void setExecution(Execution execution);

    /**
     * Sets the current ComponentManager.
     * 
     * @param componentManager the current ComponentManager
     */
    void setComponentManager(ComponentManager componentManager);

    /**
     * Sets the current XWiki Context.
     * 
     * @param context the current XWikiContext
     */
    void setXWikiContext(XWikiContext context);

}
