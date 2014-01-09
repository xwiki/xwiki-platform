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
package org.xwiki.git.internal;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gitective.core.CommitFinder;
import org.gitective.core.filter.commit.AndCommitFilter;
import org.gitective.core.filter.commit.AuthorDateFilter;
import org.gitective.core.filter.commit.AuthorSetFilter;
import org.gitective.core.filter.commit.CommitCountFilter;
import org.gitective.core.stat.AuthorHistogramFilter;
import org.gitective.core.stat.CommitCountComparator;
import org.gitective.core.stat.UserCommitActivity;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.git.GitManager;

/**
 * Provides services to access a Git repository by storing the data in the XWiki permanent directory.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultGitManager implements GitManager
{
    /**
     * Required to get access to the Environment's permanent directory, where the Script service will clone Git
     * repositories.
     */
    @Inject
    private Environment environment;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public Repository getRepository(String repositoryURI, String localDirectoryName)
    {
        Repository repository;

        File localGitDirectory = new File(this.environment.getPermanentDirectory(), "git");
        File localDirectory = new File(localGitDirectory, localDirectoryName);
        File gitDirectory = new File(localDirectory, ".git");
        this.logger.debug("Local Git repository is at [{}]", gitDirectory);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        try {
            // Step 1: Initialize Git environment
            repository = builder.setGitDir(gitDirectory)
                .readEnvironment()
                .findGitDir()
                .build();
            Git git = new Git(repository);

            // Step 2: Verify if the directory exists and isn't empty.
            if (!gitDirectory.exists()) {
                // Step 2.1: Need to clone the remote repository since it doesn't exist
                git.cloneRepository()
                    .setDirectory(localDirectory)
                    .setURI(repositoryURI)
                    .call();
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to execute Git command in [%s]", gitDirectory), e);
        }

        return repository;
    }

    @Override
    public Set<PersonIdent> findAuthors(List<Repository> repositories)
    {
        CommitFinder finder = new CommitFinder(repositories);
        AuthorSetFilter authors = new AuthorSetFilter();
        finder.setFilter(authors).find();

        return authors.getPersons();
    }

    @Override
    public UserCommitActivity[] countAuthorCommits(Date since, List<Repository> repositories)
    {
        if (repositories.isEmpty()) {
            return new UserCommitActivity[0];
        }

        CommitFinder finder = new CommitFinder(repositories);
        CommitCountFilter countFilter = new CommitCountFilter();
        AuthorHistogramFilter histogramFilter = new AuthorHistogramFilter();
        AuthorSetFilter authorFilter = new AuthorSetFilter();
        AndCommitFilter filters = new AndCommitFilter(countFilter, authorFilter, histogramFilter);

        if (since != null) {
            AuthorDateFilter dateFilter = new AuthorDateFilter(since);
            finder.setFilter(dateFilter);
        }

        finder.setMatcher(filters).find();

        return histogramFilter.getHistogram().getUserActivity(new CommitCountComparator());
    }
}
