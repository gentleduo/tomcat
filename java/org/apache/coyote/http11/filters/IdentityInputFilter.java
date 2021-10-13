/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote.http11.filters;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.coyote.http11.InputFilter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.res.StringManager;

/**
 * Identity input filter.
 *
 * @author Remy Maucherat
 */
public class IdentityInputFilter implements InputFilter {

    private static final StringManager sm = StringManager.getManager(
            IdentityInputFilter.class.getPackage().getName());


    // -------------------------------------------------------------- Constants


    protected static final String ENCODING_NAME = "identity";
    protected static final ByteChunk ENCODING = new ByteChunk();


    // ----------------------------------------------------- Static Initializer


    static {
        ENCODING.setBytes(ENCODING_NAME.getBytes(Charset.defaultCharset()), 0,
                ENCODING_NAME.length());
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Content length.
     */
    protected long contentLength = -1;


    /**
     * Remaining bytes.
     */
    protected long remaining = 0;


    /**
     * Next buffer in the pipeline.
     */
    protected InputBuffer buffer;


    /**
     * Chunk used to read leftover bytes.
     */
    protected ByteChunk endChunk = new ByteChunk();


    private final int maxSwallowSize;


    // ------------------------------------------------------------- Properties

    /**
     * Get content length.
     *
     * @deprecated  Unused - will be removed in 8.0.x
     */
    @Deprecated
    public long getContentLength() {
        return contentLength;
    }


    /**
     * Get remaining bytes.
     *
     * @deprecated  Unused - will be removed in 8.0.x
     */
    @Deprecated
    public long getRemaining() {
        return remaining;
    }


    // ------------------------------------------------------------ Constructor

    public IdentityInputFilter(int maxSwallowSize) {
        this.maxSwallowSize = maxSwallowSize;
    }


    // ---------------------------------------------------- InputBuffer Methods


    /**
     * Read bytes.
     *
     * @return If the filter does request length control, this value is
     * significant; it should be the number of bytes consumed from the buffer,
     * up until the end of the current request body, or the buffer length,
     * whichever is greater. If the filter does not do request body length
     * control, the returned value should be -1.
     */
    @Override
    public int doRead(ByteChunk chunk, Request req)
        throws IOException {
        // 当servlet中读取请求体时，会进入到这个方法，该方法返回

        int result = -1;

        // contentLength表示请求体的长度，当读取请求体时，只会返回这么长的数据
        // remaining
        if (contentLength >= 0) {  // 100
            // 可能会多次读取请求体，所以记录一下请求体还剩下多少
            if (remaining > 0) { // 10
                // 这里的buffer是InputSteamInputBuffer，会从操作系统的RecvBuf中读取数据，nRead表示读到了多少了数据
                int nRead = buffer.doRead(chunk, req); // 20

                // 如果读到的数据超过了剩余部分，那么将chunk的标记缩小，缩小为剩余部分的最后一个位置，多余数据不属于请求体了
                if (nRead > remaining) {
                    // The chunk is longer than the number of bytes remaining
                    // in the body; changing the chunk length to the number
                    // of bytes remaining
                    chunk.setBytes(chunk.getBytes(), chunk.getStart(),
                                   (int) remaining);
                    result = (int) remaining;
                } else {
                    // 如果真实读到的数据小于剩下的
                    result = nRead;
                }
                // 记录一下还需要读多少数据
                if (nRead > 0) {
                    // 10 - 20==10
                    remaining = remaining - nRead; // 如果剩余数据比真实读到的数据小，remaining将为负数
                }
            } else {
                // 如果没有剩余数据了，返回-1
                // No more bytes left to be read : return -1 and clear the
                // buffer
                chunk.recycle();
                result = -1;
            }
        }

        return result;

    }


    // ---------------------------------------------------- InputFilter Methods


    /**
     * Read the content length from the request.
     */
    @Override
    public void setRequest(Request request) {
        contentLength = request.getContentLengthLong();
        remaining = contentLength;
    }


    @Override
    public long end() throws IOException {
        // 本次http请求已经处理完了，做收尾工作
        // 主要处理看请求体是否有剩余数据没有读完

        // 判断剩余数据是否超过了限制
        final boolean maxSwallowSizeExceeded = (maxSwallowSize > -1 && remaining > maxSwallowSize);
        long swallowed = 0;

        // remaining==contentlengt
        // Consume extra bytes.
        // 还有剩余数据
        while (remaining > 0) {

            // 从操作系统读取数据
            int nread = buffer.doRead(endChunk, null);
            if (nread > 0 ) {
                // 如果读到了数据
                swallowed += nread;
                // 更新剩余数据
                remaining = remaining - nread;
                // 如果在遍历剩余数据时，读到的数据超过了maxSwallowSize，则会抛异常，后续逻辑就会把socket关掉
                if (maxSwallowSizeExceeded && swallowed > maxSwallowSize) {
                    // 我们不会提早失败，因此客户端可以去读取响应在连接关闭之前
                    // Note: We do not fail early so the client has a chance to
                    // read the response before the connection is closed. See:
                    // https://httpd.apache.org/docs/2.0/misc/fin_wait_2.html#appendix
                    throw new IOException(sm.getString("inputFilter.maxSwallow"));
                }
            } else { // errors are handled higher up.
                // 如果本来认为还有剩余数据，但是真正去读的时候没有数据了，nread等于-1，索引剩余数据为0
                remaining = 0;
            }
        }

        // 读到的真实数据超过了剩余数据，则remaining为负数
        // If too many bytes were read, return the amount.
        return -remaining;

    }


    /**
     * Amount of bytes still available in a buffer.
     */
    @Override
    public int available() {
        return 0;
    }


    /**
     * Set the next buffer in the filter pipeline.
     */
    @Override
    public void setBuffer(InputBuffer buffer) {
        this.buffer = buffer;
    }


    /**
     * Make the filter ready to process the next request.
     */
    @Override
    public void recycle() {
        contentLength = -1;
        remaining = 0;
        endChunk.recycle();
    }


    /**
     * Return the name of the associated encoding; Here, the value is
     * "identity".
     */
    @Override
    public ByteChunk getEncodingName() {
        return ENCODING;
    }


}
