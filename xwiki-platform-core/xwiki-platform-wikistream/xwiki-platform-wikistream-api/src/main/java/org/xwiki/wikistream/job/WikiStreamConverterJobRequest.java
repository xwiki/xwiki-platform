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
package org.xwiki.wikistream.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * The request used to configure "wikistream.converter" job.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Unstable
public class WikiStreamConverterJobRequest extends AbstractRequest implements WikiStreamJobRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getInputType()
     */
    private WikiStreamType inputType;

    /**
     * @see #getInputProperties()
     */
    private Map<String, Object> inputProperties;

    /**
     * @see #getOutputType()
     */
    private WikiStreamType outputType;

    /**
     * @see #getOutputProperties()
     */
    private Map<String, Object> outputProperties;

    /**
     * @param inputType the type of the input module
     * @param inputProperties the configuration of the input module
     * @param outputType the type of the output module
     * @param outputProperties the configuration of the output module
     */
    public WikiStreamConverterJobRequest(WikiStreamType inputType, Map<String, Object> inputProperties,
        WikiStreamType outputType, Map<String, Object> outputProperties)
    {
        this.inputType = inputType;
        this.inputProperties = inputProperties;
        this.outputType = outputType;
        this.outputProperties = outputProperties;

        List<String> jobId = new ArrayList<String>();
        jobId.add(JOBID_PREFIX);
        jobId.add("convert");
        jobId.add(inputType.serialize());
        jobId.add(outputType.serialize());
        setId(jobId);
    }

    /**
     * @param request the request to copy
     */
    public WikiStreamConverterJobRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the type of the input module
     */
    public WikiStreamType getInputType()
    {
        return this.inputType;
    }

    /**
     * @return the configuration of the input module
     */
    public Map<String, Object> getInputProperties()
    {
        return this.inputProperties;
    }

    /**
     * @return the type of the output module
     */
    public WikiStreamType getOutputType()
    {
        return this.outputType;
    }

    /**
     * @return the configuration of the output module
     */
    public Map<String, Object> getOutputProperties()
    {
        return this.outputProperties;
    }
}
