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
package org.xwiki.scheduler.test.po;

import org.openqa.selenium.TimeoutException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * A page written to by the script of a scheduler job, used to observe that the job has been executed.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
public class SchedulerJobOutputPage extends ViewPage
{
    /**
     * How long to wait, in seconds, for a job to have written its content. Longer than the default timeout since each
     * reload of this page takes a share of it.
     */
    private static final int TIMEOUT = 30;

    /**
     * @param reference the reference of the page the job writes to
     * @return the page object for that page
     */
    public static SchedulerJobOutputPage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(reference, "view");

        return new SchedulerJobOutputPage();
    }

    /**
     * Wait until the job has written the passed content in this page. A job is executed asynchronously from the action
     * that triggers it, so the content only appears after this page has been loaded, hence the reload. Contrary to
     * {@link ViewPage#waitUntilContent(String)}, reloading is safe here since the content is written by the scheduler
     * and not by an asynchronous process that loading this page would start over.
     * <p>
     * The page must already exist, so that the content of a page that exists is what is being waited for. Locating the
     * content of a page that doesn't exist yet fails with a wait of its own, which would consume the whole timeout.
     *
     * @param expectedContent the content the job is expected to write
     */
    public void waitUntilContentIs(String expectedContent)
    {
        // Using an array to have an effectively final variable.
        String[] lastContent = new String[1];
        try {
            getDriver().waitUntilCondition(driver -> {
                driver.navigate().refresh();
                lastContent[0] = getContent();

                return expectedContent.equals(lastContent[0]);
            }, TIMEOUT);
        } catch (TimeoutException e) {
            throw new TimeoutException(String.format("Got [%s]%nExpected [%s]", lastContent[0], expectedContent), e);
        }
    }
}
