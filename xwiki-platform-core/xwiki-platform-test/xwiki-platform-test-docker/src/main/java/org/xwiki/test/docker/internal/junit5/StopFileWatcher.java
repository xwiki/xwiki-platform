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
package org.xwiki.test.docker.internal.junit5;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to wait for a file to be created using WatchService (inotify on Linux) and delete it once detected.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
final class StopFileWatcher
{
    private static final String WAIT_FAILED_MESSAGE = "Failed to wait for file [{}]";

    private static final Logger LOGGER = LoggerFactory.getLogger(StopFileWatcher.class);

    private StopFileWatcher()
    {
        // Utility
    }

    static void waitForFileAndDeleteIfEmpty(Path target)
    {
        if (deleteIfExistsAndEmpty(target)) {
            return;
        }

        if (!watchForCreateAndDelete(target)) {
            fallbackWaitAndDelete(target);
        }
    }

    /**
     * Checks if a file exists, and if it does, deletes it if it's empty.
     *
     * @param target the file to delete
     * @return {@code true} if the file existed, {@code false} otherwise
     */
    private static boolean deleteIfExistsAndEmpty(Path target)
    {
        boolean exists = Files.exists(target);

        if (exists) {
            try {
                // Don't delete non-empty files to avoid data loss.
                if (Files.size(target) == 0) {
                    Files.deleteIfExists(target);
                } else {
                    LOGGER.warn("Not deleting non-empty file [{}], please delete it manually.", target);
                }
            } catch (IOException e) {
                LOGGER.warn("Could not delete existing file [{}]", target, e);
            }
        }

        return exists;
    }

    private static boolean watchForCreateAndDelete(Path target)
    {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            target.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path created = (Path) event.context();
                        if (created != null && created.getFileName().equals(target.getFileName())) {
                            deleteIfExistsAndEmpty(target);
                            return true;
                        }
                    }
                }

                if (!key.reset()) {
                    // Directory no longer accessible, stop watching.
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("WatchService failed, falling back to blocking wait for [{}]", target.getFileName(), e);
            return false;
        } catch (InterruptedException e) {
            LOGGER.error(WAIT_FAILED_MESSAGE, target.getFileName(), e);
            Thread.currentThread().interrupt();
            // Stop processing
            return true;
        }
    }

    private static void fallbackWaitAndDelete(Path target)
    {
        while (!Files.exists(target)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LOGGER.error(WAIT_FAILED_MESSAGE, target.getFileName(), e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        deleteIfExistsAndEmpty(target);
    }
}
