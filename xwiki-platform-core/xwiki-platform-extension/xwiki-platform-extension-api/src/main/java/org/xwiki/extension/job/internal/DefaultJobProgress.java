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
package org.xwiki.extension.job.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.xwiki.extension.job.event.status.JobProgress;
import org.xwiki.extension.job.event.status.PopLevelProgressEvent;
import org.xwiki.extension.job.event.status.PushLevelProgressEvent;
import org.xwiki.extension.job.event.status.StepProgressEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 */
public class DefaultJobProgress implements EventListener, JobProgress
{
    /**
     * Listened events.
     */
    private static final List<Event> EVENTS = Arrays.asList(new PushLevelProgressEvent(), new PopLevelProgressEvent(),
        new StepProgressEvent());

    /**
     * The unique name of the current job progress.
     */
    private String name;

    /**
     * The progress stack.
     */
    private Stack<Level> progress = new Stack<Level>();

    /**
     * A step.
     * 
     * @version $Id$
     */
    static class Level
    {
        /**
         * Global progress between 0 and 1.
         */
        public double globalOffset;

        /**
         * Current level progress between 0 and 1.
         */
        public double levelOffset;

        /**
         * Size of the step between 0 and 1.
         */
        public double globalStepSize;

        /**
         * Size of the step between 0 and 1.
         */
        public double localStepSize;

        /**
         * The current step.
         */
        public int currentStep;

        /**
         * The number of steps.
         */
        public int steps;

        /**
         * @param steps number of steps
         * @param offset the current offset
         * @param parentSize the size of the parent step
         */
        public Level(int steps, double offset, double parentSize)
        {
            this.steps = steps;

            this.globalOffset = offset;
            this.globalStepSize = parentSize / steps;

            this.levelOffset = 0.0D;
            this.localStepSize = 1.0D / steps;
        }
    }

    /**
     * @param id the unique id of the parent job status
     */
    public DefaultJobProgress(String id)
    {
        this.name = getClass().getName() + '_' + id;
    }

    // EventListener

    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        if (event instanceof PushLevelProgressEvent) {
            PushLevelProgressEvent progressEvent = (PushLevelProgressEvent) event;
            if (this.progress.isEmpty()) {
                this.progress.push(new Level(progressEvent.getSteps(), getOffset(), 1.0D));
            } else {
                this.progress
                    .push(new Level(progressEvent.getSteps(), getOffset(), this.progress.peek().globalStepSize));
            }
        } else if (event instanceof PopLevelProgressEvent) {
            this.progress.pop();
            nextStep();
        } else if (event instanceof StepProgressEvent) {
            nextStep();
        }
    }

    /**
     * Move progress to next step.
     */
    private void nextStep()
    {
        if (!this.progress.isEmpty()) {
            Level step = this.progress.peek();

            ++step.currentStep;
            step.globalOffset += step.globalStepSize;
            step.levelOffset += step.localStepSize;
        }
    }

    // JobProgress

    @Override
    public double getOffset()
    {
        return this.progress.isEmpty() ? 0.0D : this.progress.peek().globalOffset;
    }

    @Override
    public double getCurrentLevelOffset()
    {
        return this.progress.isEmpty() ? 0.0D : this.progress.peek().levelOffset;
    }
}
