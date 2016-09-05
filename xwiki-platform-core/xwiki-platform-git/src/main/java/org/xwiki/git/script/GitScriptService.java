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
package org.xwiki.git.script;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.gitective.core.stat.UserCommitActivity;
import org.joda.time.DateTime;
import org.xwiki.component.annotation.Component;
import org.xwiki.git.GitManager;
import org.xwiki.script.service.ScriptService;

/**
 * Various APIs to make it easy to perform Git commands from within scripts.
 * <p>
 * Example usage from Velocity:
 * <pre><code>
 *   {{velocity}}
 *   #set ($calendar = $datetool.calendar)
 *   #set ($now = $datetool.date)
 *   #set ($discard = $calendar.add(6, -10))
 *   #set ($repository = $services.git.getRepository("https://github.com/xwiki/xwiki-commons.git",
 *     "xwiki/xwiki-commons"))
 *   #set ($data = $services.git.countAuthorCommits($calendar.getTime(), $repository))
 *   #foreach ($authorCommits in $data)
 *     * $authorCommits.email = $authorCommits.count
 *   #end
 *   {{/velocity}}
 * </code></pre>
 *
 * @version $Id$
 * @since 4.2M1
 */
@Component
@Named("git")
@Singleton
public class GitScriptService implements ScriptService
{
    @Inject
    private GitManager gitManager;

    /**
     * Clone a Git repository by storing it locally in the XWiki Permanent directory. If the repository is already
     * cloned, no action is done.
     *
     * @param repositoryURI the URI to the Git repository to clone (eg "git://github.com/xwiki/xwiki-commons.git")
     * @param localDirectoryName the name of the directory where the Git repository will be cloned (this directory is
     *        relative to the permanent directory
     * @return the cloned Repository instance
     */
    public Repository getRepository(String repositoryURI, String localDirectoryName)
    {
        return this.gitManager.getRepository(repositoryURI, localDirectoryName);
    }

    /**
     * Find all authors who have ever committed code in the passed repository.
     *
     * @param repositories the list of repositories in which to look for authors
     * @return the list of authors who have ever contributed code in the passed repository
     * @since 5.3M2
     */
    public Set<PersonIdent> findAuthors(Repository... repositories)
    {
        return this.gitManager.findAuthors(Arrays.asList(repositories));
    }

    /**
     * Count commits done by all authors in the passed repositories and since the passed date.
     *
     * @param sinceDays the number of days to look back in the past or look from the beginning if set to 0
     * @param repositories the list of repositories in which to look for commits
     * @return the author commit activity
     * @since 5.3M2
     */
    public UserCommitActivity[] countAuthorCommits(int sinceDays, Repository... repositories)
    {
        return countAuthorCommits(sinceDays, Arrays.asList(repositories));
    }

    /**
     * Count commits done by all authors in the passed repositories and since the passed date.
     *
     * @param sinceDays the number of days to look back in the past or look from the beginning if set to 0
     * @param repositories the list of repositories in which to look for commits
     * @return the author commit activity
     * @since 5.3M2
     */
    public UserCommitActivity[] countAuthorCommits(int sinceDays, List<Repository> repositories)
    {
        Date date = null;
        if (sinceDays > 0) {
            // Compute today - since days
            DateTime now = new DateTime();
            date = now.minusDays(sinceDays).toDate();
        }
        return this.gitManager.countAuthorCommits(date, repositories);
    }
}
