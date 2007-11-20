package com.xpn.xwiki.tool.xar;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiveFileFilter;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Base class for xar and unxar mojos.
 * 
 * @version $Id: $
 */
abstract class AbstractXarMojo extends AbstractMojo
{
    /**
     * Open hook.
     */
    protected static final String HOOK_OPEN = "[";

    /**
     * Close hook.
     */
    protected static final String HOOK_CLOSE = "]";

    /**
     * The name of the file in the package when to find general informations.
     */
    protected static final String PACKAGE_XML = "package.xml";

    /**
     * Unpacks the XAR file (exclude the package.xml file if it exists).
     * 
     * @param file the file to be unpacked.
     * @param location the location where to put the unpacket files.
     * @param logName the name use with {@link ConsoleLogger}.
     * @param overwrite indicate if extracted files has to overwrite existing ones.
     * @throws MojoExecutionException error when unpacking the file.
     */
    protected void unpack(File file, File location, String logName, boolean overwrite)
        throws MojoExecutionException
    {
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, logName));
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);

            // Ensure that we don't overwrite XML document files present in this project since
            // we want those to be used and not the ones in the dependent XAR.
            unArchiver.setOverwrite(overwrite);

            if (!overwrite) {
                // Do not unpack any package.xml file in dependant XARs. We'll generate a complete
                // one automatically.
                List filters = new ArrayList();
                filters.add(new ArchiveFileFilter()
                {
                    public boolean include(InputStream dataStream, String entryName)
                    {
                        return (!entryName.equals(PACKAGE_XML));
                    }
                });
                unArchiver.setArchiveFilters(filters);
            }

            unArchiver.extract();
        } catch (Exception e) {
            throw new MojoExecutionException("Error unpacking file " + HOOK_OPEN + file
                + HOOK_CLOSE + " to " + HOOK_OPEN + location + HOOK_CLOSE, e);
        }
    }
}
