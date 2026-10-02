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
package org.xwiki.rest.internal.resources.pages;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.pages.PagesResource;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PagesResourceImpl")
public class PagesResourceImpl extends XWikiResource implements PagesResource
{
    /**
     * The maximum time spent evaluating the parent filter, for all the pages of a request together.
     */
    private static final Duration PARENT_FILTER_TIMEOUT = Duration.ofSeconds(1);

    /**
     * The number of characters read between two checks of the deadline of the parent filter. Reading the clock takes
     * tens of nanoseconds while reading a character takes about one, so checking the clock at each read makes matching
     * an order of magnitude slower. With one check every 1024 reads, the overhead of the check is negligible, and a
     * timeout is still detected at most 1024 matching steps late, which is a few microseconds, far below the
     * {@link #PARENT_FILTER_TIMEOUT}.
     * This constant is preferably a power of two as it can be optimized into a mask when combined with a modulo `%`
     * operation in {@link ParentFilter#checkDeadline()}.
     */
    private static final int DEADLINE_CHECK_INTERVAL = 1024;

    @Override
    public Pages getPages(String wikiName, String spaceName, Integer start, Integer number,
            String parentFilterExpression, String order, Boolean withPrettyNames)
            throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();
        List<String> spaces = parseSpaceSegments(spaceName);
        String spaceId = Utils.getLocalSpaceId(spaces);

        Pages pages = objectFactory.createPages();
        int limit = validateAndGetLimit(number);
        ParentFilter parentFilter = compileParentFilter(parentFilterExpression);

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            Query query = ("date".equals(order)) ? queryManager.createQuery(
                    "select doc.name from Document doc where doc.space=:space and language='' order by doc.date desc",
                    "xwql") : queryManager.getNamedQuery("getSpaceDocsName");

            /* Use an explicit query to improve performance */
            List<String> pageNames =
                query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "hidden"))
                    .bindValue("space", spaceId)
                    .setOffset(start)
                    .setLimit(limit)
                    .execute();

            for (String pageName : pageNames) {
                String pageFullName = Utils.getPageId(wikiName, spaces, pageName);

                if (!Utils.getXWikiApi(componentManager).exists(pageFullName)) {
                    getLogger().warn("Page [{}] appears to be in space [{}] but no information is available.", pageName,
                        spaceId);
                } else {
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);

                    /* We only add pages we have the right to access */
                    if (doc != null) {
                        boolean add = true;

                        Document parent = Utils.getParentDocument(doc, Utils.getXWikiApi(componentManager));

                        if (parentFilter != null) {
                            String parentId = "";
                            if (parent != null && !parent.isNew()) {
                                parentId = parent.getPrefixedFullName();
                            }
                            add = parentFilter.matches(parentId);
                        }

                        if (add) {
                            pages.getPageSummaries().add(DomainObjectFactory.createPageSummary(objectFactory,
                                    uriInfo.getBaseUri(), doc, Utils.getXWikiApi(componentManager), withPrettyNames));
                        }
                    }
                }
            }
        } catch (ParentFilterTimeoutException | StackOverflowError e) {
            // Reject the whole request so that the client never receives a silently partial result.
            throw createInvalidParentFilterException("The parentId filter is too expensive to evaluate.");
        } catch (Exception e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }

        return pages;
    }

    private ParentFilter compileParentFilter(String parentFilterExpression)
    {
        ParentFilter parentFilter = null;
        if (parentFilterExpression != null) {
            if ("null".equals(parentFilterExpression)) {
                parentFilter = new ParentFilter(Pattern.compile(""));
            } else {
                try {
                    parentFilter = new ParentFilter(Pattern.compile(parentFilterExpression));
                } catch (PatternSyntaxException | StackOverflowError e) {
                    throw createInvalidParentFilterException("Invalid parentId filter.");
                }
            }
        }
        return parentFilter;
    }

    private WebApplicationException createInvalidParentFilterException(String message)
    {
        return new WebApplicationException(
            Response.status(Status.BAD_REQUEST).entity(message).type("text/plain").build());
    }

    /**
     * Thrown when the evaluation of the parent filter exceeds its deadline.
     */
    private static final class ParentFilterTimeoutException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        ParentFilterTimeoutException()
        {
            // The stack trace is never used, so there's no need to fill it.
            super(null, null, false, false);
        }
    }

    /**
     * Matches the parent of each page against the parent filter, within a time budget shared by all the pages of a
     * request. Only the time spent matching is counted, so that loading the documents never consumes the budget.
     */
    private static final class ParentFilter
    {
        private final Pattern pattern;

        private long remainingTime = PARENT_FILTER_TIMEOUT.toNanos();

        private long deadline;

        private int reads;

        ParentFilter(Pattern pattern)
        {
            this.pattern = pattern;
        }

        boolean matches(String parentId)
        {
            long start = System.nanoTime();
            // Compute the deadline based on the remaining time budget.
            this.deadline = start + this.remainingTime;
            try {
                return this.pattern.matcher(new DeadlineCharSequence(parentId, this)).matches();
            } finally {
                // Decreases the remaining time budget by what was spent running the matching this time.
                this.remainingTime -= System.nanoTime() - start;
            }
        }

        void checkDeadline()
        {
            // Reading the clock costs much more than reading a character, so it's done only once every
            // DEADLINE_CHECK_INTERVAL reads, which delays the detection of a timeout by a few microseconds at most.
            if (this.reads++ % DEADLINE_CHECK_INTERVAL == 0 && System.nanoTime() - this.deadline > 0) {
                throw new ParentFilterTimeoutException();
            }
        }
    }

    /**
     * A {@link CharSequence} that fails once the deadline of its {@link ParentFilter} is passed.
     * {@link java.util.regex} has no timeout, but it reads its input through {@link #charAt(int)} at each matching
     * step, so checking the deadline there bounds the time spent matching a regular expression, whatever its
     * complexity.
     */
    private static final class DeadlineCharSequence implements CharSequence
    {
        private final CharSequence wrapped;

        private final ParentFilter filter;

        DeadlineCharSequence(CharSequence wrapped, ParentFilter filter)
        {
            this.wrapped = wrapped;
            this.filter = filter;
        }

        @Override
        public char charAt(int index)
        {
            this.filter.checkDeadline();
            return this.wrapped.charAt(index);
        }

        @Override
        public int length()
        {
            return this.wrapped.length();
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            return this.wrapped.subSequence(start, end);
        }

        @Override
        public String toString()
        {
            return this.wrapped.toString();
        }
    }
}
