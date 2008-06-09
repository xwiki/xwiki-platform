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
package org.xwiki.rendering.macro;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.DOM;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class TocMacro extends AbstractMacro
{
    private boolean isNumbered;
    
    public List<Block> execute(Map<String, String> parameters, String content, final DOM dom)
    {
        // Example:
        // 1 Section1
        // 1 Section2
        // 1.1 Section3
        // 1 Section4
        // 1.1.1 Section5
        
        // Generates:
        // ListBlock
        //  |_ ListItemBlock (TextBlock: Section1)
        //  |_ ListItemBlock (TextBlock: Section2)
        //    |_ ListBlock
        //      |_ ListItemBlock (TextBlock: Section3)
        //  |_ ListItemBlock (TextBlock: Section4)
        //    |_ ListBlock
        //      |_ ListBlock
        //        |_ ListItemBlock (TextBlock: Section5)
        
        // Look for all Section blocks
/*        
        BulletedListBlock listBlock = null;
        int depth = 0;
        for (Iterator it = dom.getBlocksByType(SectionBlock.class).iterator(); it.hasNext();) {
            SectionBlock sectionBlock = (SectionBlock) it.next();
            TextBlock textBlock = new TextBlock(sectionBlock.getTitle());
            ListItemBlock itemBlock = new ListItemBlock(textBlock);
            // Is the current list block at the correct depth?
            int levelDiff = sectionBlock.getLevel().getAsInt() - depth;
            if (levelDiff == 0) {
                // Simply add the list item to the list
                listBlock.addChildBlock(itemBlock);
            } else if (levelDiff > 0) {
                BulletedListBlock diffList = new BulletedListBlock(itemBlock);
                BulletedListBLock newList = diffList;
                for (int i = 0; i < levelDiff - 1; i++) {
                    diffList = new BulletedListBlock(diffList);
                }
                if (listBlock == null) {
                    listBlock = diffList;
                } else {
                    listBlock.addChildBlock(diffList);
                }
                depth = sectionBlock.getLevel().getAsInt();
            }
            
            if ((listBlock == null) || 
            
        }
        
        l = new List()
        diffLevel = sectionLevel - l.depth;
        List listToAttach = findList(diffLevel);
        listToAttach.add(l);
        */
        
        
        return null;
    }

}
