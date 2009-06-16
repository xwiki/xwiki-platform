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
 *
 */
package org.xwiki.component.logging;

import org.xwiki.component.phase.LogEnabled;

/**
 * Convenience class so that components can simply extend this class and not have to reimplement the
 * {@link Logger} interface.
 *  
 * @version $Id$
 */
public class AbstractLogEnabled implements LogEnabled
{
    private static final Logger VOID_LOGGER = new VoidLogger();
    
    private Logger logger;

    public void enableLogging(Logger logger)
    {
        this.logger = logger;
    }

    protected Logger getLogger()
    {
        Logger logger = VOID_LOGGER;
        if (this.logger != null) {
            logger = this.logger;
        }
        return logger;
    }
}
