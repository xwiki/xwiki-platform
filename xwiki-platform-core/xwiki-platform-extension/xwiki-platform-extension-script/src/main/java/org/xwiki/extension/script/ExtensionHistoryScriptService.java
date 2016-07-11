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
import java.util.Collection;
import java.util.Collections;
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
import org.xwiki.extension.job.history.ReplayJobStatus;
import org.xwiki.extension.job.history.ReplayRequest;
import org.xwiki.extension.job.history.internal.ReplayJob;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.WikiReference;
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
            final String currentWikiNamespace = WIKI_NAMESPACE_PREFIX + xcontextProvider.get().getWikiId();
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

        ReplayRequest request = createReplayRequest(createReplayPlan(records, true, null));

        try {
            return this.jobExecutor.execute(ReplayJob.JOB_TYPE, request);
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Prepares a list of history records for replay.
     * 
     * @param records the history records to prepare for replay
     * @param preserveUsers {@code true} if the given history records should be replayed using their original users,
     *            {@code false} if the current user should be used instead
     * @param namespaces the namespaces where to replay the given history records; pass {@code null} or an empty
     *            collection if you want to preserve the original namespaces
     * @return the modified history records, prepared to be replayed
     */
    public List<ExtensionJobHistoryRecord> createReplayPlan(List<ExtensionJobHistoryRecord> records,
        boolean preserveUsers, Collection<String> namespaces)
    {
        String currentWiki = this.xcontextProvider.get().getWikiId();
        if (!this.authorization.hasAccess(Right.ADMIN, new WikiReference(currentWiki))) {
            return Collections.emptyList();
        } else if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Replay on the current wiki using the current user.
            return createReplayPlanInternal(records, false, Arrays.asList(WIKI_NAMESPACE_PREFIX + currentWiki));
        } else {
            // Only the users that have PR can preserve the rights-related properties of the original extension request.
            return createReplayPlanInternal(records, preserveUsers, namespaces);
        }
    }

    /**
     * @param id identifies the replay job
     * @return the status of the specified replay job
     */
    public ReplayJobStatus getReplayJobStatus(String id)
    {
        return (ReplayJobStatus) getJobStatus(getReplayJobId(id));
    }

    private List<ExtensionJobHistoryRecord> createReplayPlanInternal(List<ExtensionJobHistoryRecord> records,
        boolean preserveUsers, Collection<String> namespaces)
    {
        for (ExtensionJobHistoryRecord record : records) {
            if (!preserveUsers) {
                setRightsProperties((AbstractRequest) record.getRequest());
            }
            if (record.getRequest().hasNamespaces() && namespaces != null && namespaces.size() > 0) {
                ((AbstractRequest) record.getRequest()).setProperty("namespaces", namespaces);
            }
        }
        return records;
    }

    private ReplayRequest createReplayRequest(List<ExtensionJobHistoryRecord> records)
    {
        ReplayRequest request = new ReplayRequest();
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        request.setId(getReplayJobId(suffix));
        // There may be questions for which there isn't a specified answer.
        request.setInteractive(true);
        request.setRecords(records);

        // Provide information on what started the job.
        request.setProperty(PROPERTY_CONTEXT_WIKI, this.xcontextProvider.get().getWikiId());
        request.setProperty(PROPERTY_CONTEXT_ACTION, this.xcontextProvider.get().getAction());

        setRightsProperties(request);

        return request;
    }

    private List<String> getReplayJobId(String suffix)
    {
        return Arrays.asList(AbstractExtensionRequest.JOBID_PREFIX, ID, suffix);
    }
}
