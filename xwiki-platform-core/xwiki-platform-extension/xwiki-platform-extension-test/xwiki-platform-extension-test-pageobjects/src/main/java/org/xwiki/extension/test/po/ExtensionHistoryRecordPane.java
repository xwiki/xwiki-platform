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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * A record of the Extension History (an install, uninstall or repair job).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ExtensionHistoryRecordPane extends BaseElement
{
    private static final String RECORD_CLASS = "extension-history-record";

    private final WebElement container;

    /**
     * Creates a new instance.
     *
     * @param container the list item that displays the record
     */
    public ExtensionHistoryRecordPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the type of the job that was recorded (e.g. {@code install}, {@code uninstall}, {@code repairxar})
     */
    public String getJobType()
    {
        return StringUtils.normalizeSpace(StringUtils.remove(this.container.getAttribute("class"), RECORD_CLASS));
    }

    /**
     * @return the names of the extensions targeted by the recorded job; an extension that can't be resolved (e.g. an
     *     uninstalled one) is displayed with its id, without link
     */
    public List<String> getExtensionNames()
    {
        // The extensions are listed as "name version, name version" (the version is missing when the job request
        // doesn't specify it) in the span that follows the record icon.
        WebElement extensions =
            getDriver().findElementWithoutWaiting(this.container, By.xpath("./div[1]/span[not(@class)]"));
        List<String> versions = getExtensionVersions();
        List<String> names = new ArrayList<>();
        for (String extension : extensions.getText().split(", ")) {
            String name = extension.trim();
            for (String version : versions) {
                name = StringUtils.removeEnd(name, ' ' + version);
            }
            names.add(name);
        }
        return names;
    }

    /**
     * @return the versions of the extensions targeted by the recorded job, when the job request specifies them
     */
    public List<String> getExtensionVersions()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.className("extension-history-record-version"))
            .stream().map(WebElement::getText).filter(StringUtils::isNotEmpty).toList();
    }

    /**
     * @return the message telling who executed the job and when (e.g. "Installed by superadmin on 2026/10/03 10:00")
     */
    public String getUserAndDate()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className("extension-history-record-user"))
            .getText();
    }
}
