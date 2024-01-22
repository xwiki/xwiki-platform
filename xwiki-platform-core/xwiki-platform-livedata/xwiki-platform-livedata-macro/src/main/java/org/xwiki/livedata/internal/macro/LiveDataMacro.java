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
package org.xwiki.livedata.internal.macro;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.macro.LiveDataMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Display dynamic lists of data.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("liveData")
@Singleton
public class LiveDataMacro extends AbstractMacro<LiveDataMacroParameters>
{
    @Inject
    private LiveDataRenderer liveDataRenderer;

    /**
     * Default constructor.
     */
    public LiveDataMacro()
    {
        super("Live Data", "Display dynamic lists of data.",
            new DefaultContentDescriptor("Advanced Live Data configuration (JSON)", false),
            LiveDataMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(LiveDataMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        try {
            boolean restricted = context.getTransformationContext().isRestricted();
            return List.of(this.liveDataRenderer.execute(parameters, content, restricted));
        } catch (LiveDataException e) {
            throw new MacroExecutionException("Failed to render the content of the LiveData macro.", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}
