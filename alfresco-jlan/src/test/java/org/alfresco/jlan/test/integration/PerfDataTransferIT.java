package org.alfresco.jlan.test.integration;

import static org.testng.Assert.*;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

import java.io.OutputStream;

import org.alfresco.jlan.util.MemorySize;

import jcifs.smb.SmbFile;

/**
 * Data Transfer Performance Test Class
 *
 * @author gkspencer
 */
public class PerfDataTransferIT extends ParameterizedJcifsTest {

    // Maximum/minimum allowed file size
    private static final long MIN_FILESIZE = 50 * MemorySize.MEGABYTE;
    private static final long MAX_FILESIZE = 5 * MemorySize.TERABYTE;

    // Maximum/minimum allowed file write size
    private static final int MIN_WRITESIZE = 128;
    private static final int MAX_WRITESIZE = (int) (64 * MemorySize.KILOBYTE);

    /**
     * Default constructor
     */
    public PerfDataTransferIT() {
        super("PerfDataTransferIT");
    }

    private void doTest(final int iteration, final long fileSize, final int writeSize) throws Exception {
        final String testFileName = getPerTestFileName(iteration);
        final SmbFile sf = new SmbFile(getRoot(), testFileName);
        if (sf.exists()) {
            LOGGER.info("File {} exists", testFileName);
        } else {
            // Allocate the I/O buffer
            final byte[] ioBuf = new byte[writeSize];
            // Record the start time
            final long startTime = System.currentTimeMillis();
            long endTime = 0L;
            // Create a new file
            sf.createNewFile();
            assertTrue(sf.exists(), "File exists after create");
            try (final OutputStream os = sf.getOutputStream()) {
                // Write to the file until we hit the required file size
                long fs = 0L;
                while (fs < fileSize) {
                    // Write to the file
                    os.write(ioBuf);
                    // Update the file size
                    fs += ioBuf.length;
                }
                // Make sure all data has been written to the file
                // File is closed via autoClose
                os.flush();
                // Save the end time
                endTime = System.currentTimeMillis();

                // Output the elapsed time
                long elapsedMs = endTime - startTime;
                int ms = (int) (elapsedMs % 1000L);
                long elapsedSecs = elapsedMs / 1000;
                int secs = (int) (elapsedSecs % 60L);
                int mins = (int) ((elapsedSecs / 60L) % 60L);
                int hrs  = (int) (elapsedSecs / 3600L);

                // Calculate the average throughput
                long throughput = fileSize / elapsedSecs;

                String msg = String.format("Created %s (size %s/writes %s) in %d:%d:%d.%d (%dms) with average speed of %s/sec",
                        testFileName, MemorySize.asScaledString(fileSize), MemorySize.asScaledString(writeSize),
                        hrs, mins, secs, ms, elapsedMs, MemorySize.asScaledString(throughput));
                LOGGER.info(msg);
                Reporter.log(msg + "<br/>\n");
            }
        }
    }

    @Parameters({"iterations", "filesize", "writesize"})
        @Test(groups = "perftest", singleThreaded = true)
        public void test(@Optional("1") final int iterations,
                @Optional("500M") final String fs, @Optional("32K") final String ws) throws Exception {
            long fileSize = 0;
            int writeSize = 0;
            try {
                fileSize = MemorySize.getByteValue(fs);
                if (fileSize < MIN_FILESIZE || fileSize > MAX_FILESIZE) {
                    fail("Invalid file size (" + MIN_FILESIZE + " - " + MAX_FILESIZE + ")");
                }
            } catch (NumberFormatException ex) {
                fail("Invalid file size " + fs);
            }
            try {
                writeSize = MemorySize.getByteValueInt(ws);
                if (writeSize < MIN_WRITESIZE || writeSize > MAX_WRITESIZE) {
                    fail("Invalid write buffer size (" + MIN_WRITESIZE + " - " + MAX_WRITESIZE + ")");
                }
            } catch (NumberFormatException ex) {
                fail("Invalid write size " + ws);
            }
            for (int i = 0; i < iterations; i++) {
                doTest(i, fileSize, writeSize);
            }
        }
}
