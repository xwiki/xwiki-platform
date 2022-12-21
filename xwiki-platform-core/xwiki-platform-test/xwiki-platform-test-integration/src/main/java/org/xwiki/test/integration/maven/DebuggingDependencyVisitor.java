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
package org.xwiki.test.integration.maven;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug the Maven dependency resolution nodes by printing them.
 *
 * @version $Id$
 * @since 10.9
 */
public class DebuggingDependencyVisitor implements DependencyVisitor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggingDependencyVisitor.class);

    private static final int SPACES_INCREMENT = 4;

    private int level;

    @Override
    public boolean visitEnter(DependencyNode dependencyNode)
    {
        printInfoLog(dependencyNode);
        this.level += SPACES_INCREMENT;
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode dependencyNode)
    {
        printInfoLog(dependencyNode);
        this.level -= SPACES_INCREMENT;
        return true;
    }

    private void printInfoLog(DependencyNode dependencyNode)
    {
        LOGGER.info(StringUtils.repeat(" ", this.level) + "{}", dependencyNode.getArtifact());
    }
}
