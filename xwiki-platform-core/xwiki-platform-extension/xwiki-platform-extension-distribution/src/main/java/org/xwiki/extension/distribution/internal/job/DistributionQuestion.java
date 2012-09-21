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
package org.xwiki.extension.distribution.internal.job;

/**
 * @version $Id$
 * @since 4.2M3
 */
public class DistributionQuestion
{
    public enum Action
    {
        /**
         * Validate current step and go to next one.
         */
        COMPLETE_STEP,

        /**
         * Skip the current step.
         */
        SKIP_STEP,

        /**
         * Cancel the current step until next distribution modification.
         */
        CANCEL_STEP,

        /**
         * Skip all the remaining steps.
         */
        SKIP,

        /**
         * Cancel all the remaining steps until next distribution modification.
         */
        CANCEL
    }

    private String stepId;

    private Action action;

    private boolean save = true;

    public DistributionQuestion(String stepId)
    {
        this.stepId = stepId;
    }

    public String getStepId()
    {
        return this.stepId;
    }

    public Action getAction()
    {
        return this.action;
    }

    public void setUpdateState(Action action)
    {
        this.action = action;
    }

    public boolean isSave()
    {
        return this.save;
    }

    public void setSave(boolean save)
    {
        this.save = save;
    }
}
