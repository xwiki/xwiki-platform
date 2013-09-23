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
package org.xwiki.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.linker.IFrameLinker;

/**
 * Ensures that the {@code *.cache.html} files are loaded in standards mode by adding the document type declaration.
 * <p>
 * NOTE: This linker was added manly to overcome http://code.google.com/p/google-web-toolkit/issues/detail?id=4567 .
 * 
 * @version $Id$
 */
@LinkerOrder(LinkerOrder.Order.PRIMARY)
public class StandardsModeIFrameLinker extends IFrameLinker
{
    /**
     * The strict HTML 4.01 document type declaration.
     */
    private static final String DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";

    @Override
    protected String getModulePrefix(TreeLogger logger, LinkerContext context, String strongName)
        throws UnableToCompleteException
    {
        return DOCTYPE + super.getModulePrefix(logger, context, strongName);
    }

    @Override
    protected String getModulePrefix(TreeLogger logger, LinkerContext context, String strongName, int numFragments)
        throws UnableToCompleteException
    {
        return DOCTYPE + super.getModulePrefix(logger, context, strongName, numFragments);
    }
}
