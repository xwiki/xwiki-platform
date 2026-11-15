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
package org.xwiki.tool.utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import com.xpn.xwiki.XWiki;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link OldCoreHelper}, starting a real XWiki instance on an in-memory HSQLDB database.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class OldCoreHelperTest
{
    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @XWikiTempDir
    private File permanentDirectory;

    @Test
    void closeReleasesXWiki() throws Exception
    {
        WeakReference<XWiki> xwiki = createAndCloseOldCoreHelper();

        // The embedded Solr implementation isn't in the test classpath, so the components requiring it fail to load.
        this.logCapture.ignoreAllMessages(List.of(OldCoreHelperTest::isCausedByMissingSolr));

        // The packager runs inside the Maven JVM: anything still referencing the XWiki instance once the packager is
        // done (e.g., a thread started by XWiki and never stopped) keeps it in memory until the end of the build.
        // Request a garbage collection every 100ms, for at most 10s.
        await("The XWiki instance to be garbage collected after closing the OldCoreHelper")
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(100))
            .atMost(Duration.ofSeconds(10))
            .until(() -> {
                System.gc();
                return xwiki.get() == null;
            });
    }

    private static boolean isCausedByMissingSolr(ILoggingEvent event)
    {
        for (IThrowableProxy throwable = event.getThrowableProxy(); throwable != null;
            throwable = throwable.getCause()) {
            if (StringUtils.contains(throwable.getMessage(), "org.xwiki.search.solr")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Mimic the lifecycle of the component manager and the execution context in the packager mojos.
     */
    private WeakReference<XWiki> createAndCloseOldCoreHelper() throws Exception
    {
        EmbeddableComponentManager componentManager =
            (EmbeddableComponentManager) org.xwiki.environment.System.initialize(this.permanentDirectory);
        Execution execution = componentManager.getInstance(Execution.class);
        componentManager.<ExecutionContextManager>getInstance(ExecutionContextManager.class)
            .initialize(new ExecutionContext());

        File hibernateConfig = new File(getClass().getResource("/hibernate.cfg.xml").toURI());
        try (OldCoreHelper oldCoreHelper = OldCoreHelper.create(componentManager, "xwiki", hibernateConfig)) {
            return new WeakReference<>(oldCoreHelper.getXWikiContext().getWiki());
        } finally {
            execution.removeContext();
            org.xwiki.environment.System.dispose(componentManager);
        }
    }
}
