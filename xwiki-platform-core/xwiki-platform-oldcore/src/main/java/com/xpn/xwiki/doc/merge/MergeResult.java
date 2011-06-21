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
package com.xpn.xwiki.doc.merge;

import java.util.ArrayList;
import java.util.List;

/**
 * Report of what happen during merge.
 * 
 * @version $Id$
 * @since 3.2M1
 */
public class MergeResult
{
    /**
     * @see #isModified()
     */
    private boolean modified;

    /**
     * @see #getErrors()
     */
    private List<Exception> errors = new ArrayList<Exception>();

    /**
     * @see #getWarnings()
     */
    private List<Exception> warnings = new ArrayList<Exception>();

    /**
     * @param modified indicate that something has been modified during the merge
     */
    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    /**
     * @return true if something has been modified during the merge
     */
    public boolean isModified()
    {
        return this.modified;
    }

    /**
     * Error raised during the merge.
     * <p>
     * Generally collision for which we don't know what do to at all.
     * 
     * @return the merge errors
     */
    public List<Exception> getErrors()
    {
        return this.errors;
    }

    /**
     * Warning raised during the merge.
     * <p>
     * The difference with error is that in that case a decision which should be good (or at least safe enough) for most
     * of the case has been made.
     * 
     * @return the merge warning
     */
    public List<Exception> getWarnings()
    {
        return this.warnings;
    }

    /**
     * Add error.
     * 
     * @param e the error
     */
    public void error(Exception e)
    {
        getErrors().add(e);
    }

    /**
     * Add warning.
     * 
     * @param e the warning
     */
    public void warn(Exception e)
    {
        getWarnings().add(e);
    }
}
