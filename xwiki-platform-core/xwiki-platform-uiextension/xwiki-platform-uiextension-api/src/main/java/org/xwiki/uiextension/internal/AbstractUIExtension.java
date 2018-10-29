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
package org.xwiki.uiextension.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.uiextension.UIExtension;

/**
 * Base class to automate things common to most implementations of {@link UIExtension}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractUIExtension implements UIExtension
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUIExtension.class);

    protected final ComponentManager componentManager;

    protected final JobProgressManager progress;

    protected final ErrorBlockGenerator errorBlockGenerator;

    /**
     * Default constructor.
     *
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     */
    public AbstractUIExtension(ComponentManager componentManager) throws ComponentLookupException
    {
        this.componentManager = componentManager;
        this.progress = componentManager.getInstance(JobProgressManager.class);
        this.errorBlockGenerator = componentManager.getInstance(ErrorBlockGenerator.class);
    }

    @Override
    public Block execute()
    {
        this.progress.startStep(this, "panel.progress.execute", "Execute UIX with id [{}]", getId());

        Block result;
        try {
            result = executeInternal();
        } catch (Exception e) {
            result = new CompositeBlock(this.errorBlockGenerator
                .generateErrorBlocks(String.format("Failed to execute UIX with id [%s]", getId()), e, false));
        } finally {
            this.progress.endStep(this);
        }

        return result;
    }

    /**
     * @return the {@link Block} that must be rendered when this extension is displayed
     */
    protected abstract Block executeInternal() throws Exception;
}
