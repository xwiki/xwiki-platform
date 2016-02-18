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

import com.xpn.xwiki.XWikiException;

/**
 * ReputationException definition.
 * 
 * @version $Id$
 * @since 6.4M3
 */
public class ReputationException extends RatingsException
{
    private static final long serialVersionUID = 1L;

    // Module
    public static final int MODULE_PLUGIN_RATINGS_REPUTATION = 1121;

    // Error
    public static final int ERROR_REPUTATION_NOT_IMPLEMENTED = 1121001;

    /**
     * ReputationException default constructor.
     */
    public ReputationException()
    {
    }

    /**
     * ReputationException constructor.
     * 
     * @param module the module in which the exception occurred
     * @param code the code for the generated exception
     * @param message the message for the generated exception
     */
    public ReputationException(int module, int code, String message)
    {
        super(module, code, message);
    }

    /**
     * ReputationException constructor.
     * 
     * @param module the module in which the exception occurred
     * @param code the code for the generated exception
     * @param message the message for the generated exception
     * @param e the Exception that occurred
     */
    public ReputationException(int module, int code, String message, Exception e)
    {
        super(module, code, message, e);
    }

    /**
     * ReputationException constructor.
     * 
     * @param e the Exception that occurred
     */    
    public ReputationException(XWikiException e)
    {
        super(e.getModule(), e.getCode(), e.getMessage());

        initCause(e.getCause());
        setArgs(e.getArgs());
    }
}
