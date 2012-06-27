package org.xwiki.repository.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * @version $Id$
 * @since 3.3M2
 */
public class ExtensionsLiveTableElement extends LiveTableElement
{
    public ExtensionsLiveTableElement()
    {
        super("extensions");
    }

    public void filterName(String pattern)
    {
        filterColumn("xwiki-livetable-extensions-filter-1", pattern);
    }

    public ExtensionPage clickExtensionName(String name)
    {
        getUtil().findElementWithoutWaiting(getDriver(), By.linkText(name)).click();

        return new ExtensionPage();
    }
}
