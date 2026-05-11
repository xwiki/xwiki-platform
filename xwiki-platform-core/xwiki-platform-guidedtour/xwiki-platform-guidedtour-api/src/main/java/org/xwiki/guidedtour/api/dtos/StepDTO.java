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
package org.xwiki.guidedtour.api.dtos;

import org.xwiki.guidedtour.api.enums.ActionType;
import org.xwiki.guidedtour.api.enums.Placement;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Step DTO used to represent a step with its properties.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepDTO
{
    private String element;

    private int order;

    private String content;

    private Placement placement;

    private boolean backdrop;

    private boolean reflex;

    private String targetPage;

    private ActionType targetAction;

    private String queryParameters;

    /**
     * Default constructor.
     */
    public StepDTO()
    {
    }

    /**
     * Gets the CSS selector of the element to highlight for this step.
     *
     * @return the CSS selector of the element to highlight for this step
     */
    public String getElement()
    {
        return this.element;
    }

    /**
     * Sets the CSS selector of the element to highlight for this step.
     *
     * @param element the CSS selector of the element to highlight for this step
     */
    public void setElement(String element)
    {
        this.element = element;
    }

    /**
     * Gets the order of the step in the task.
     *
     * @return the order of the step in the task
     */
    public int getOrder()
    {
        return this.order;
    }

    /**
     * Sets the order of the step in the task.
     *
     * @param order the order of the step in the task
     */
    public void setOrder(int order)
    {
        this.order = order;
    }

    /**
     * Gets the content to display for this step.
     *
     * @return the content to display for this step
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * Sets the content to display for this step.
     *
     * @param content the content to display for this step
     */
    public void setContent(String content)
    {
        this.content = content;
    }

    /**
     * Gets the placement of the step tooltip.
     *
     * @return the placement of the step tooltip, representing a value from the {@link Placement} enum
     */
    public Placement getPlacement()
    {
        return this.placement;
    }

    /**
     * Sets the placement of the step tooltip.
     *
     * @param placement the placement of the step tooltip, representing a value from the {@link Placement} enum
     */
    public void setPlacement(String placement)
    {
        this.placement = Placement.fromString(placement);
    }

    /**
     * Checks if a backdrop should be shown for this step.
     *
     * @return true if a backdrop should be shown for this step, false otherwise
     */
    public boolean isBackdrop()
    {
        return this.backdrop;
    }

    /**
     * Sets whether a backdrop should be shown for this step.
     *
     * @param backdrop {@code true} to show a backdrop for this step, {@code false} otherwise
     */
    public void setBackdrop(boolean backdrop)
    {
        this.backdrop = backdrop;
    }

    /**
     * Checks if clicking on the highlighted element should be allowed to go to the next step.
     *
     * @return {@code true} if clicking on the highlighted element should be allowed to go to the next step,
     *     {@code false} otherwise
     */
    public boolean isReflex()
    {
        return this.reflex;
    }

    /**
     * Sets whether clicking on the highlighted element should be allowed to go to the next step.
     *
     * @param reflex {@code true} to allow clicking on the highlighted element to go to the next step, {@code false}
     *     otherwise
     */
    public void setReflex(boolean reflex)
    {
        this.reflex = reflex;
    }

    /**
     * Gets the target page on which the step should be displayed.
     *
     * @return the target page on which the step should be displayed
     */
    public String getTargetPage()
    {
        return this.targetPage;
    }

    /**
     * Sets the target page on which the step should be displayed.
     *
     * @param targetPage the target page on which the step should be displayed
     */
    public void setTargetPage(String targetPage)
    {
        this.targetPage = targetPage;
    }

    /**
     * Gets the target action on the target page.
     *
     * @return the target action on the target page, representing a value from the {@link ActionType} enum
     */
    public ActionType getTargetAction()
    {
        return this.targetAction;
    }

    /**
     * Sets the target action on the target page.
     *
     * @param targetAction the target action on the target page, representing a value from the {@link ActionType}
     *     enum
     */
    public void setTargetAction(String targetAction)
    {
        this.targetAction = ActionType.fromString(targetAction);
    }

    /**
     * Gets the query parameters to add to the URL when navigating to the target page.
     *
     * @return the query parameters to add to the URL when navigating to the target page
     */
    public String getQueryParameters()
    {
        return this.queryParameters;
    }

    /**
     * Sets the query parameters to add to the URL when navigating to the target page.
     *
     * @param queryParameters the query parameters to add to the URL when navigating to the target page
     */
    public void setQueryParameters(String queryParameters)
    {
        this.queryParameters = queryParameters;
    }
}
