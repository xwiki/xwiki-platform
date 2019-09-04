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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a conflict displayed in a diff view.
 *
 * @version $Id$
 * @since 11.8RC1
 */
public class Conflict extends BaseElement
{
    private static final String DECISION_ID = "conflict_decision_value_%s_%s";
    private static final String DECISION_SELECT_ID = "conflict_decision_select_%s";
    private static final String DECISION_CONTAINER_ID = "conflict_decision_container_%s";
    private static final String DECISION_CLASSNAME = "diff-decision";

    private WebElement container;

    /**
     * The available decisions to solve the conflict.
     */
    public enum DecisionType {
        /**
         * Previous value.
         */
        PREVIOUS,

        /**
         * Current value.
         */
        CURRENT,

        /**
         * Next value.
         */
        NEXT,

        /**
         * Custom value.
         */
        CUSTOM
    }

    private String conflictReference;

    /**
     * Default constructor. A conflict is defined by its reference.
     *
     * @param conflictReference the reference of the conflict.
     */
    public Conflict(String conflictReference)
    {
        this.conflictReference = conflictReference;
        this.container = getDriver().findElement(By.id(String.format(DECISION_CONTAINER_ID, conflictReference)));
    }

    /**
     * @return the current decision value, i.e. what will be submitted to fix the conflict.
     */
    public String getDecisionChange()
    {
        List<WebElement> decisions = this.container.findElements(By.className(DECISION_CLASSNAME));
        for (WebElement decision : decisions) {
            if (decision.isDisplayed()) {
                return decision.getText();
            }
        }
        throw new NoSuchElementException("Cannot find any visible decision.");
    }

    /**
     * @return {@code true} if the current decision value is empty, i.e. it announces that the inserted value will be
     * removed.
     */
    public boolean isDecisionChangeEmpty()
    {
        List<WebElement> decisions = this.container.findElements(By.className(DECISION_CLASSNAME));
        for (WebElement decision : decisions) {
            if (decision.isDisplayed()) {
                return decision.getAttribute("class").contains("empty-decision-value");
            }
        }
        throw new NoSuchElementException("Cannot find any visible decision.");
    }

    /**
     * Change the decision with the given type.
     * @param type the decision to be taken.
     */
    public void setDecision(DecisionType type)
    {
        WebElement select = getDriver().findElement(By.id(String.format(DECISION_SELECT_ID, conflictReference)));
        new Select(select).selectByValue(type.name().toLowerCase());
    }

    /**
     * @return the current decision type.
     */
    public DecisionType getCurrentDecision()
    {
        List<WebElement> decisions = this.container.findElements(By.className(DECISION_CLASSNAME));
        for (WebElement decision : decisions) {
            if (decision.isDisplayed()) {
                String id = decision.getAttribute("id");
                return DecisionType.valueOf(id.split("_")[3].toUpperCase());
            }
        }
        throw new NoSuchElementException("Cannot find any visible decision.");
    }

    private WebElement getDecisionElement(DecisionType type)
    {
        return getDriver().findElement(By.id(String.format(DECISION_ID, type.name().toLowerCase(), conflictReference)));
    }

    /**
     * Take a custom decision with a default value.
     * @param customValue the value for the custom decision.
     */
    public void setCustomValue(String customValue)
    {
        setDecision(DecisionType.CUSTOM);
        WebElement customInput = getDecisionElement(DecisionType.CUSTOM);
        customInput.sendKeys(customValue);
    }
}
