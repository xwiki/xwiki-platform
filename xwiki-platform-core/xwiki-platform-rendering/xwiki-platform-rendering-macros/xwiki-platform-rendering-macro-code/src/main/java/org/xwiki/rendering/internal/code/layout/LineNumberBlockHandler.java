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
package org.xwiki.rendering.internal.code.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.code.CodeMacroLayout;

/**
 * A {@link CodeLayoutHandler} that displays line numbers next to the rendered code. It does so by writing the line
 * numbers in a {@link GroupBlock} and wrapping the code into its own <code>GroupBlock</code>.
 * 
 * @version $Id$
 * @since 11.5RC1
 */
@Component
@Singleton
@Named(CodeMacroLayout.Constants.LINE_NUM_HINT)
public class LineNumberBlockHandler implements CodeLayoutHandler
{
    private Pattern linePattern = Pattern.compile("\r?\n");

    @Override
    public List<Block> layout(List<Block> blocks, String originalContent)
    {
        Matcher matcher = linePattern.matcher(originalContent);
        int lineCount = 1;
        while (matcher.find()) {
            lineCount++;
        }

        List<Block> lineBlocks = new ArrayList<Block>(lineCount * 2);
        for (int i = 0; i < lineCount; i++) {
            lineBlocks.add(new WordBlock(Integer.toString(i + 1)));
            lineBlocks.add(new NewLineBlock());
        }
        GroupBlock lineNumbers = new GroupBlock(lineBlocks);
        setClassParameter(lineNumbers, "linenos");

        GroupBlock codeBlocks = new GroupBlock(blocks);
        GroupBlock wrapper = new GroupBlock(Arrays.asList(lineNumbers, codeBlocks));
        setClassParameter(wrapper, "linenoswrapper");

        return Arrays.<Block>asList(wrapper);
    }
    
    private void setClassParameter(Block block, String value) {
        block.setParameter("class", value);
    }
}
