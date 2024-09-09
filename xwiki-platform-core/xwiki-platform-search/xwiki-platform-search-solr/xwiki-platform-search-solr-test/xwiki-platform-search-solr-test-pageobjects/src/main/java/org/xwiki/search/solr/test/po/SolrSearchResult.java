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
package org.xwiki.search.solr.test.po;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * A search result in the Solr search page.
 *
 * @version $Id$
 */
public class SolrSearchResult extends BaseElement
{
    private final WebElement container;

    /**
     * Constructor.
     *
     * @param container the container of the search result
     */
    public SolrSearchResult(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the title of the search result
     */
    public String getTitle()
    {
        return this.container.findElement(By.cssSelector(".search-result-title a")).getText();
    }

    /**
     * Get the highlights that are displayed. Highlights are automatically expanded to make them all visible.
     *
     * @return a map from the name of the field to the snippets that are displayed for the field
     */
    public Map<String, List<String>> getHighlights()
    {
        // If there is a link to show all highlights, click it so they are visible.
        getDriver().findElementsWithoutWaiting(this.container, By.cssSelector("a.search-result-highlightAll"))
            .forEach(WebElement::click);
        WebElement highlightContainer = this.container.findElement(By.className("search-result-highlights"));
        List<WebElement> highlightElements =
            getDriver().findElementsWithoutWaiting(highlightContainer, By.xpath("./*"));
        Map<String, List<String>> highlights = new HashMap<>();
        String highlightKey = null;
        for (WebElement highlightElement : highlightElements) {
            if ("dt".equals(highlightElement.getTagName())) {
                highlightKey = highlightElement.getText();
            } else if (highlightKey != null && "dd".equals(highlightElement.getTagName())) {
                highlights.put(highlightKey,
                    getDriver().findElementsWithoutWaiting(highlightElement, By.tagName("blockquote"))
                        .stream()
                        .map(WebElement::getText)
                        .collect(Collectors.toList())
                );
            }
        }

        return highlights;
    }
}
