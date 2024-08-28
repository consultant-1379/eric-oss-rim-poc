/*******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.apps.data.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class OutputStreamWriterWithBuf.
 * This gives access the 'Write' method and allows the number of bytes read/written and # records to be counted.
 */
@Slf4j
public class OutputStreamWriterWithWriterAccess extends OutputStreamWriter {

    AtomicInteger numBytesWritten = new AtomicInteger(0);
    AtomicInteger numLinesWritten = new AtomicInteger(0);

    /**
     * Instantiates a new output stream writer with writer access.
     *
     * @param outputStream
     *     the Output Stream
     * @param utf8
     *     the char set to use.
     */
    public OutputStreamWriterWithWriterAccess(OutputStream outputStream, Charset utf8) {
        super(outputStream, utf8);
    }

    /**
     * Write.
     *
     * @param str
     *     the str to write.
     * @param off
     *     the starting byte offset.
     * @param len
     *     the number of bytes to write
     *
     * @throws IOException
     *     Signals that an I/O exception has occurred.
     */
    @Override
    public void write(String str, int off, int len) throws IOException {
        numBytesWritten.getAndAdd(len);
        numLinesWritten.getAndIncrement();
        log.trace("WRITING LINE : {} : {} , off '{}' len = '{}', total bytes written '{}'", numLinesWritten.get(), str, off, len, numBytesWritten);
        super.write(str, off, len);

    }

    public int getNumberBytesWritten() {
        log.trace("Total number bytes written = {} ", numBytesWritten.get());
        return numBytesWritten.get();
    }

    public int getNumberLinesWritten() {
        log.trace("Total number lines written = {} ", numLinesWritten.get());
        return numLinesWritten.get();
    }

}
