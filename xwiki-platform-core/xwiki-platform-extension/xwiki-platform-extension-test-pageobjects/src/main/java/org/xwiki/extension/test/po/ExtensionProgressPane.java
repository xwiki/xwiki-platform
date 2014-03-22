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
package org.xwiki.extension.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The section that displays the extension progress log.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionProgressPane extends BaseElement
{
    /**
     * The section container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the section container
     */
    public ExtensionProgressPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the list of extension that will be affected by the current job
     */
    public List<DependencyPane> getJobPlan()
    {
        return new ExtensionDependenciesPane(container).getDependenciesAfter("The following ");
    }

    /**
     * @return the lines of job log
     */
    public List<LogItemPane> getJobLog()
    {
        List<LogItemPane> log = new ArrayList<LogItemPane>();
        for (WebElement element : getUtil().findElementsWithoutWaiting(getDriver(), container,
            By.className("job-log-item"))) {
            log.add(new LogItemPane(element));
        }
        return log;
    }

    /**
     * @return the pane that displays the merge conflict resolution UI
     */
    public MergeConflictPane getMergeConflict()
    {
        if (getUtil().findElementsWithoutWaiting(getDriver(), container, By.name("versionToKeep")).size() > 0) {
            return new MergeConflictPane();
        }
        return null;
    }
}
