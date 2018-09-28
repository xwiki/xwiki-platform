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
package org.xwiki.sheet.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.context.Execution;

/**
 * Takes the sheet parameter from the request and puts it on the execution context to be used by the sheet manager.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("sheet")
@Singleton
public class SheetRequestInitializer implements RequestInitializer
{
    /**
     * The name of the request parameter specifying the sheet to be applied to the requested document. This is also the
     * name of the execution context property specifying the same thing.
     */
    public static final String SHEET_PROPERTY_NAME = "sheet";

    /**
     * Execution context handler.
     */
    @Inject
    private Execution execution;

    @Override
    public void initialize(Request request) throws RequestInitializerException
    {
        String sheet = (String) request.getProperty(SHEET_PROPERTY_NAME);
        // Note: Empty string value makes sense because it means the sheet is the document itself (the sheet reference
        // is resolved relative to the current document and thus the empty string reference refers to the current
        // document). In other words, the empty string value can be used to display the document without a sheet.
        if (sheet != null) {
            execution.getContext().setProperty(SHEET_PROPERTY_NAME, sheet);
        }
    }
}
