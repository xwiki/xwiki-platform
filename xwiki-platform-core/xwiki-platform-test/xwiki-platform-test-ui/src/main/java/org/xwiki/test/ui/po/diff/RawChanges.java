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
package org.xwiki.test.ui.po.diff;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the raw changes display (in a tab in the compare documents view or in the conflict view).
 *
 * @version $Id$
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 */
public class RawChanges extends BaseElement
{
    private final WebElement container;

    @FindBy(className = "diff-summary")
    private WebElement diffSummary;

    /**
     * @param container the container element
     */
    public RawChanges(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return {@code true} if the "No changes" message is displayed and there are no diffs displayed, {@code false}
     *         otherwise
     */
    public boolean hasNoChanges()
    {
        return !getDriver().findElementsWithoutWaiting(this.container,
            By.xpath("//div[@class = 'box infomessage' and . = 'No changes']")).isEmpty()
            && getChangedEntities().isEmpty();
    }

    /**
     * @return the summary for the displayed changes
     */
    public DocumentDiffSummary getDiffSummary()
    {
        return new DocumentDiffSummary(this.diffSummary);
    }

    /**
     * @return the names (labels) for the entities that have been modified (have modified properties)
     */
    public List<String> getChangedEntities()
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(By.xpath("//dl[@class = 'diff-group']/dt"));
        List<String> labels = new ArrayList<>();
        for (WebElement element : elements) {
            labels.add(element.getText().trim());
        }
        return labels;
    }

    /**
     * @param label the entity label
     * @return the changes for the specified entity
     */
    public EntityDiff getEntityDiff(String label)
    {
        return new EntityDiff(this.container.findElement(By
            .xpath("//dd[parent::dl[@class = 'diff-group'] and preceding-sibling::dt[normalize-space(.) = '" + label
                + "']]")));
    }
}
