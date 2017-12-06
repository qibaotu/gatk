package org.broadinstitute.hellbender.utils.codecs.xsvLocatableTable;

import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Codec class to read from XSV (e.g. csv, tsv, etc.) files.
 * Designed specifically with use by {@link org.broadinstitute.hellbender.tools.funcotator.Funcotator} in mind.
 *
 * Files that can be parsed by the {@link XsvLocatableTableCodec} will have a sibling configuration file of the same
 * name and the `.config` extension.  This file will contain the following keys:
 *      contig
 *      start
 *      end
 *      delimiter
 *
 * These tables are assumed to have comment lines that start with `#` and a header that has the names for each
 * column in the table as the top row.
 *
 * Two or three columns will specify the location of each row in the data (contig, start, end; start and end can be the same
 * column).
 *
 * Created by jonn on 12/4/17.
 */
public final class XsvLocatableTableCodec extends AsciiFeatureCodec<XsvTableFeature> {

    private static final Logger logger = LogManager.getLogger(XsvLocatableTableCodec.class);

    //==================================================================================================================
    // Public Static Members:

    //==================================================================================================================
    // Private Static Members:

    private static final String COMMENT_DELIMITER = "#";
    private static final String CONFIG_FILE_EXTENSION = ".config";

    //==================================================================================================================
    // Private Members:

    /** Column number from which to get the contig string for each entry. */
    private int contigColumn;

    /** Column number from which to get the start position for each entry. */
    private int startColumn;

    /** Column number from which to get the end position for each entry. */
    private int endColumn;

    /** Delimiter for entries in this XSV Table. */
    private String delimiter;

    /** The XSV Table Header */
    private List<String> header;

    /** The current position in the file that is being read. */
    private long currentLine = 0;

    //==================================================================================================================
    // Constructors:

    public XsvLocatableTableCodec() {
        super(XsvTableFeature.class);
    }

    //==================================================================================================================
    // Override Methods:

    @Override
    public boolean canDecode(final String path) {
        // Check for a sibling config file with the same name, .config as extension
        // Open that config file
        // Validate config file
        //     Expected keys present
        //     Key values are valid
        // Get delimiter
        // Get columns for:
        //      contig
        //      start
        //      end

        // Get the paths to our file and the config file:
        final Path inputFilePath = IOUtils.getPath(path);
        final Path configFilePath = getConfigFilePath(inputFilePath);

        // Check that our files are good for eating... I mean reading...
        if ( validateInputDataFile(inputFilePath) && validateInputConfigFile(configFilePath) ) {

            // Get our metadata and set up our internals so we can read from this file:
            readMetadataFromConfigFile(configFilePath);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public XsvTableFeature decode(final String s) {

        // Increment our line counter:
        ++currentLine;

        if (s.startsWith(COMMENT_DELIMITER)) {
            return null;
        }

        final List<String> split = new ArrayList<>(Arrays.asList(s.split(delimiter)));
        if (split.size() < 1) {
            throw new UserException.BadInput("XSV file has a line with no delimiter at line number: " + currentLine);
        }
        else if ( split.size() < header.size() ) {
            logger.warn("WARNING: Line " + currentLine + "does not have the same number of fields as header!  Padding with empty fields to end...");
            while (split.size() < header.size() ) {
                split.add("");
            }
        }
        else if ( split.size() > header.size() ) {
            logger.warn("WARNING: Line " + currentLine + "does not have the same number of fields as header!  Truncating fields from end...");
            while (split.size() > header.size() ) {
                split.remove( split.size() - 1 );
            }
        }

        return new XsvTableFeature(contigColumn, startColumn, endColumn, header, split);
    }

    @Override
    public List<String> readActualHeader(final LineIterator reader) {
        // All leading lines with comments / header info are headers:
        while ( reader.hasNext() ) {

            final String line = reader.next();
            ++currentLine;

            // Ignore commented out lines:
            if ( !line.startsWith(COMMENT_DELIMITER) ) {
                // The first non-commented line is the column header:
                Collections.addAll(header, line.split(delimiter));

                return header;
            }
        }

        throw new UserException.BadInput("Given file is malformed - does not contain a header!");
    }

    //==================================================================================================================
    // Static Methods:

    //==================================================================================================================
    // Instance Methods:

    /**
     * Asserts that the given {@code filePath} is a valid file from which to read.
     * @param filePath The {@link Path} to the data file to validate.
     * @return {@code true} if the given {@code filePath} is valid; {@code false} otherwise.
     */
    private boolean validateInputDataFile(final Path filePath) {
        return Files.exists(filePath) && Files.isReadable(filePath) && !Files.isDirectory(filePath);
    }

    /**
     * Asserts that the given {@code filePath} is a valid configuration file from which to read.
     * @param filePath The {@link Path} to the configuration file to validate.
     * @return {@code true} if the given {@code filePath} is valid; {@code false} otherwise.
     */
    private boolean validateInputConfigFile(final Path filePath) {
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.config");
        return validateInputDataFile(filePath) && pathMatcher.matches(filePath);
    }

    /**
     * Gets the path to the corresponding configuration file for the given {@code inputFilePath}.
     * The resulting path may or may not exist.
     * @param inputFilePath The data file {@link Path} from which to construct the path to the configuration file.
     * @return The {@link Path} for the configuration file associated with {@code inputFilePath}.
     */
    private Path getConfigFilePath(final Path inputFilePath) {
        final String configFilePath = IOUtils.replaceExtension( inputFilePath.toUri().toString(), CONFIG_FILE_EXTENSION );
        return inputFilePath.resolveSibling(configFilePath);
    }

    /**
     * Reads the metadata required for parsing from the given {@code configFilePath}.
     * @param configFilePath {@link Path} to the configuration file from which to read in and setup metadata values.
     */
    private void readMetadataFromConfigFile(final Path configFilePath) {
        throw new UserException("UNIMPLEMENTED METHOD: XsvLocatableTableCodec::readMetadataFromConfigFile !");
    }

    //==================================================================================================================
    // Helper Data Types:

}