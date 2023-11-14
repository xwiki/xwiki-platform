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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.concurrent.AbstractContextStore;

/**
 * Saves and restores the sheet specified on the execution context.
 * 
 * @version $Id$
 * @since 14.10.18
 * @since 15.5.3
 * @since 15.9RC1
 */
@Component
@Named("sheet")
@Singleton
public class SheetContextStore extends AbstractContextStore
{
    @Inject
    private Execution execution;

    /**
     * Default constructor.
     */
    public SheetContextStore()
    {
        super(SheetRequestInitializer.SHEET_PROPERTY_NAME);
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Collection<String> entries)
    {
        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            String sheet = (String) context.getProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME);
            if (sheet != null) {
                save(contextStore, SheetRequestInitializer.SHEET_PROPERTY_NAME, sheet, entries);
            }
        }
    }

    @Override
    public void restore(Map<String, Serializable> contextStore)
    {
        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            String sheet = (String) contextStore.get(SheetRequestInitializer.SHEET_PROPERTY_NAME);
            if (sheet != null) {
                context.setProperty(SheetRequestInitializer.SHEET_PROPERTY_NAME, sheet);
            }
        }
    }
}
