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
package org.xwiki.formula.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.formula.AbstractFormulaRenderer;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.ImageData;

/**
 * Implementation of the {@link FormulaRenderer} component, which uses console commands to build a formula image. Best
 * results, but requires the presence of external programs, and involves a slight overhead for starting new processes
 * and for working with the disk.
 * <p>
 * Required commands: latex, dvips and convert (ImageMagick)
 * </p>
 * <p>
 * Performance tip: Try to mount a RAM drive/tmpfs on the folder where this component creates its temporary files
 * ([webapp work dir]/formulae/).
 * </p>
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Named("native")
@Singleton
public class NativeFormulaRenderer extends AbstractFormulaRenderer implements Initializable
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeFormulaRenderer.class);

    /** Application container, needed for retrieving the work directory where temporary files can be created. */
    @Inject
    private Environment environment;

    /** Temporary parent directory for storing files created during the image rendering process. */
    private File tempDirectory;

    @Override
    public void initialize() throws InitializationException
    {
        this.tempDirectory = new File(this.environment.getTemporaryDirectory(), "formulae");
        this.tempDirectory.mkdir();
    }

    @Override
    protected ImageData renderImage(String formula, boolean inline, FormulaRenderer.FontSize size,
        FormulaRenderer.Type type) throws IllegalArgumentException, IOException
    {
        File tmpDirectory = null;
        try {
            String texContent =
                "\\documentclass[10pt]{article}\n" + "\\usepackage[paperheight=1000in]{geometry}\n"
                    + "\\usepackage{amsmath}\n" + "\\usepackage{amsfonts}\n" + "\\usepackage{amssymb}\n"
                    + "\\usepackage{pst-plot}\n" + "\\usepackage{color}\n" + "\\pagestyle{empty}\n"
                    + "\\begin{document}\n" + size.getCommand() + "\n" + wrapFormula(formula, inline)
                    + "\\end{document}\n";
            do {
                tmpDirectory = new File(this.tempDirectory, RandomStringUtils.randomAlphanumeric(8));
            } while (tmpDirectory.exists());
            tmpDirectory.mkdir();

            final String baseName = tmpDirectory.getAbsolutePath() + "/file";
            final String texFileName = baseName + ".tex";
            final String dviFileName = baseName + ".dvi";
            final String psFileName = baseName + ".ps";
            final String imageFileName = baseName + type.getExtension();

            // Write the formula in a tex file
            FileWriter fw = new FileWriter(texFileName);
            fw.write(texContent);
            fw.close();

            // TeX to DVI
            String[] commandLine = new String[] {"latex", "--interaction=nonstopmode", texFileName};
            executeCommand(commandLine, tmpDirectory);
            // DVI to PS
            commandLine = new String[] {"dvips", "-E", dviFileName, "-o", psFileName};
            executeCommand(commandLine, tmpDirectory);
            // PS to image
            commandLine = new String[] {"convert", "-density", "120", psFileName, imageFileName};
            executeCommand(commandLine, tmpDirectory);
            return new ImageData(FileUtils.readFileToByteArray(new File(imageFileName)), type);
        } finally {
            FileUtils.deleteQuietly(tmpDirectory);
        }
    }

    /**
     * Execute a system command.
     * 
     * @param commandLine the command and its arguments
     * @param cwd the directory to use as the current working directory for the executed process
     * @return {@code true} if the command succeeded (return code 0), {@code false} otherwise
     * @throws IOException if the process failed to start
     */
    private boolean executeCommand(String[] commandLine, File cwd) throws IOException
    {
        List<String> commandList = new Vector<String>(commandLine.length);
        Collections.addAll(commandList, commandLine);

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(cwd);
        Process process = processBuilder.start();
        IOUtils.copy(process.getInputStream(), new NullOutputStream());

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (process.exitValue() != 0) {
            LOGGER.debug("Error generating image: " + IOUtils.toString(process.getErrorStream()));
        }

        return process.exitValue() == 0;
    }
}
