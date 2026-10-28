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
package org.xwiki.vfs;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Stub of the {@code tree} macro, exposing the parameters it receives as attributes of the generated group, so that
 * page tests can verify how other macros call it.
 *
 * @version $Id$
 */
@Component
@Named("tree")
@Singleton
public class TestTreeMacro extends AbstractMacro<TestTreeMacro.Parameters>
{
    /**
     * The parameters of the stubbed tree macro.
     *
     * @version $Id$
     */
    public static class Parameters
    {
        private String root;

        private String reference;

        private boolean links;

        /**
         * @return the root of the tree
         */
        public String getRoot()
        {
            return this.root;
        }

        /**
         * @param root the root of the tree
         */
        public void setRoot(String root)
        {
            this.root = root;
        }

        /**
         * @return the reference of the tree data source
         */
        public String getReference()
        {
            return this.reference;
        }

        /**
         * @param reference the reference of the tree data source
         */
        public void setReference(String reference)
        {
            this.reference = reference;
        }

        /**
         * @return whether the tree nodes are links
         */
        public boolean isLinks()
        {
            return this.links;
        }

        /**
         * @param links whether the tree nodes are links
         */
        public void setLinks(boolean links)
        {
            this.links = links;
        }
    }

    /**
     * Default constructor.
     */
    public TestTreeMacro()
    {
        super("Tree", "Tree stub", Parameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(Parameters parameters, String content, MacroTransformationContext context)
    {
        return List.of(new GroupBlock(Map.of(
            "class", "tree-stub",
            "data-root", parameters.getRoot(),
            "data-reference", parameters.getReference(),
            "data-links", String.valueOf(parameters.isLinks()))));
    }
}
