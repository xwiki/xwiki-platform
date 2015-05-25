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
package org.xwiki.extension.script;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.PredicateUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.job.AbstractExtensionRequest;
import org.xwiki.extension.job.history.ExtensionJobHistory;
import org.xwiki.extension.job.history.ExtensionJobHistoryRecord;
import org.xwiki.extension.job.history.ExtensionJobHistorySerializer;
import org.xwiki.extension.job.history.ReplayRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Various script APIs related to extension job history.
 * 
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + ExtensionHistoryScriptService.ID)
@Singleton
@Unstable
public class ExtensionHistoryScriptService extends AbstractExtensionScriptService
{
    /**
     * Utility class to build filters for the extension job history.
     */
    public class ExtensionHistoryFilter
    {
        /**
         * The list of constraints applied by this filter.
         */
        private final List<Predicate<ExtensionJobHistoryRecord>> constraints = new ArrayList<>();

        /**
         * Filters the history records with the specified job type.
         * 
         * @param jobTypes the type of jobs that should be included in the result
         * @return this
         */
        public ExtensionHistoryFilter ofType(final List<String> jobTypes)
        {
            this.constraints.add(new Predicate<ExtensionJobHistoryRecord>()
            {
                @Override
                public boolean evaluate(ExtensionJobHistoryRecord record)
                {
                    return jobTypes.contains(record.getJobType());
                }
            });
            return this;
        }

        /**
         * Filters the jobs that have been executed on the current wiki or on the entire farm.
         * 
         * @return this
         */
        public ExtensionHistoryFilter fromThisWiki()
        {
            final String currentWikiNamespace = "wiki:" + xcontextProvider.get().getWikiId();
            this.constraints.add(new Predicate<ExtensionJobHistoryRecord>()
            {
                @Override
                public boolean evaluate(ExtensionJobHistoryRecord record)
                {
                    return !record.getRequest().hasNamespaces()
                        || record.getRequest().getNamespaces().contains(currentWikiNamespace);
                }
            });
            return this;
        }

        /**
         * Lists the history records that match this filter and that are older than the specified offset record.
         * 
         * @param offsetRecordId specifies the offset record (where to start from); pass {@code null} to start from the
         *            most recent record in the history
         * @param limit the maximum number of records to return from the specified offset
         * @return a list of history records that match this filter and are older than the specified offset record
         */
        public List<ExtensionJobHistoryRecord> list(String offsetRecordId, int limit)
        {
            return history.getRecords(PredicateUtils.allPredicate(this.constraints), offsetRecordId, limit);
        }
    }

    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "history";

    @Inject
    private ExtensionJobHistory history;

    @Inject
    private ExtensionJobHistorySerializer serializer;

    /**
     * @return a new history filter
     */
    public ExtensionHistoryFilter getRecords()
    {
        return new ExtensionHistoryFilter();
    }

    /**
     * Serializes a history record.
     * 
     * @param record the history record to serialize
     * @return the string serialization of the given history record
     */
    public String serialize(ExtensionJobHistoryRecord record)
    {
        setError(null);

        try {
            return this.serializer.serialize(record);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Deserializes a list of history records.
     * 
     * @param serializedHistoryRecords the serialized list of history records
     * @return the list of history records
     */
    public List<ExtensionJobHistoryRecord> deserialize(String serializedHistoryRecords)
    {
        setError(null);

        try {
            return this.serializer.deserialize(serializedHistoryRecords);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Reads a list of history records from a given input stream (e.g. the attachment content input stream).
     * 
     * @param inputStream an input stream that provides a list of serialized history records
     * @return the list of history records
     */
    public List<ExtensionJobHistoryRecord> read(InputStream inputStream)
    {
        setError(null);

        try {
            return this.serializer.read(new InputStreamReader(inputStream));
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Replay the given list of extension history records.
     * 
     * @param records the history records to replay
     * @return the {@link Job} object that can be used to monitor the progress of the replay process, or {@code null} in
     *         case of failure
     */
    public Job replay(List<ExtensionJobHistoryRecord> records)
    {
        setError(null);

        ReplayRequest request = createReplayRequest(records);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Only the users that have PR can preserve the rights-related properties of the original extension request.
            setRightsProperties(request);
        }

        try {
            return this.jobExecutor.execute(InstallJob.JOBTYPE, request);
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    private ReplayRequest createReplayRequest(List<ExtensionJobHistoryRecord> records)
    {
        ReplayRequest request = new ReplayRequest();
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        request.setId(Arrays.asList(ExtensionManagerScriptService.EXTENSION_JOBID_PREFIX, ID, suffix));
        // There may be questions for which there isn't a specified answer.
        request.setInteractive(true);
        request.setRecords(records);
        return request;
    }

    private void setRightsProperties(ReplayRequest request)
    {
        for (ExtensionJobHistoryRecord record : request.getRecords()) {
            setRightsProperties((AbstractExtensionRequest) record.getRequest());
        }
    }
}
