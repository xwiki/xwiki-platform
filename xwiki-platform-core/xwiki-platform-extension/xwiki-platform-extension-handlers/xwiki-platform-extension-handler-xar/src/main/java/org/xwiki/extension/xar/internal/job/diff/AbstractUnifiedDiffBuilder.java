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
package org.xwiki.extension.xar.internal.job.diff;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.DiffManager;
import org.xwiki.diff.DiffResult;
import org.xwiki.diff.display.Splitter;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffConfiguration;
import org.xwiki.diff.display.UnifiedDiffDisplayer;

import com.xpn.xwiki.XWikiContext;

/**
 * Provides utility methods for computing the differences, in unified format, between two versions of an entity.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public abstract class AbstractUnifiedDiffBuilder
{
    /**
     * The content field.
     */
    protected static final String CONTENT = "content";

    @Inject
    protected Logger logger;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * The component used to split a text into lines.
     */
    @Inject
    @Named("line")
    private Splitter<String, String> lineSplitter;

    /**
     * The component used to split a text into its characters.
     */
    @Inject
    private Splitter<String, Character> charSplitter;

    /**
     * The component used to compute the differences.
     */
    @Inject
    private DiffManager diffManager;

    /**
     * The component used to display the differences in unified format.
     */
    @Inject
    private UnifiedDiffDisplayer unifiedDiffDisplayer;

    protected boolean maybeAddDiff(Map<String, List<UnifiedDiffBlock<String, Character>>> diffs, String key,
        Object previousValue, Object nextValue)
    {
        if (!Objects.equals(previousValue, nextValue)) {
            List<UnifiedDiffBlock<String, Character>> diff =
                createUnifiedDiff(previousValue == null ? null : previousValue.toString(), nextValue == null ? null
                    : nextValue.toString());
            if (diff.size() > 0) {
                diffs.put(key, diff);
                return true;
            }
        }
        return false;
    }

    private List<UnifiedDiffBlock<String, Character>> createUnifiedDiff(String previous, String next)
    {
        try {
            DiffResult<String> diffResult =
                this.diffManager.diff(this.lineSplitter.split(previous), this.lineSplitter.split(next), null);
            UnifiedDiffConfiguration<String, Character> config = this.unifiedDiffDisplayer.getDefaultConfiguration();
            config.setSplitter(this.charSplitter);
            return this.unifiedDiffDisplayer.display(diffResult, config);
        } catch (DiffException e) {
            this.logger
                .warn("Failed to compute the differences. Root cause: {}", ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }
}
