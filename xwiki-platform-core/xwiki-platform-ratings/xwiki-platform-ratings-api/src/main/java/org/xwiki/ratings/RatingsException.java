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

import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * RatingsException definition.
 * 
 * @version $Id$
 * @since 6.4M3
 */
public class RatingsException extends XWikiException
{
    private static final long serialVersionUID = 1L;

    // Module
    public static final int MODULE_PLUGIN_RATINGS = 1120;
    
    // Error list
    public static final int ERROR_RATINGS_CREATECONTAINER_NULLSPACE = 1120001;

    public static final int ERROR_RATINGS_ADDRATING_NULLCONTAINER = 1120002;

    public static final int ERROR_RATINGS_ADDRATING_NULLCONTENT = 1120003;

    public static final int ERROR_RATINGS_INVALID_RATING_ID = 1120004;

    public static final int ERROR_RATINGS_SAVERATING_NULLDOCUMENT = 1120005;

    /**
     * RatingsException default constructor
     */
    public RatingsException()
    {
    }

    /**
     * RatingsException constructor.
     * 
     * @param module the module in which the exception occurred
     * @param code the code for the generated exception
     * @param message the message for the generated exception
     */
    public RatingsException(int module, int code, String message)
    {
        super(module, code, message);
    }

    /**
     * RatingsException constructor.
     * 
     * @param module the module in which the exception occurred
     * @param code the code for the generated exception
     * @param message the message for the generated exception
     * @param e the XWikiException that occurred
     */
    public RatingsException(int module, int code, String message, Exception e)
    {
        super(module, code, message, e);
    }

    /**
     * RatingsException constructor.
     * 
     * @param e the XWikiException that occurred
     */
    public RatingsException(XWikiException e)
    {
        super(e.getModule(), e.getCode(), e.getMessage());

        initCause(e.getCause());
        setArgs(e.getArgs());
    }

    /**
     * Constructor taking a message and a parent exception.
     *
     * @param message the message of the exception.
     * @param throwable the parent cause.
     * @since 12.6
     */
    @Unstable
    public RatingsException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
