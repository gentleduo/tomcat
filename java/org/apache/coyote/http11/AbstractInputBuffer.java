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
package org.apache.coyote.http11;

import java.io.IOException;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.res.StringManager;

public abstract class AbstractInputBuffer<S> implements InputBuffer{

    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * Associated Coyote request.
     */
    protected Request request;


    /**
     * Headers of the associated request.
     */
    protected MimeHeaders headers;


    /**
     * State.
     */
    protected boolean parsingHeader;


    /**
     * Swallow input ? (in the case of an expectation)
     */
    protected boolean swallowInput;


    /**
     * Pointer to the current read buffer.
     */
    protected byte[] buf;


    /**
     * Last valid byte.
     */
    protected int lastValid;


    /**
     * Position in the buffer.
     */
    protected int pos;


    /**
     * Pos of the end of the header in the buffer, which is also the
     * start of the body.
     */
    protected int end;


    /**
     * Underlying input buffer.
     */
    protected InputBuffer inputStreamInputBuffer;


    /**
     * Filter library.
     * Note: Filter[0] is always the "chunked" filter.
     */
    protected InputFilter[] filterLibrary;


    /**
     * Active filters (in order).
     */
    protected InputFilter[] activeFilters;


    /**
     * Index of the last active filter.
     */
    protected int lastActiveFilter;


    protected boolean rejectIllegalHeaderName;


    protected HttpParser httpParser;


    // ------------------------------------------------------------- Properties


    /**
     * Add an input filter to the filter library.
     */
    public void addFilter(InputFilter filter) {

        // FIXME: Check for null ?

        InputFilter[] newFilterLibrary =
            new InputFilter[filterLibrary.length + 1];
        for (int i = 0; i < filterLibrary.length; i++) {
            newFilterLibrary[i] = filterLibrary[i];
        }
        newFilterLibrary[filterLibrary.length] = filter;
        filterLibrary = newFilterLibrary;

        // filterLibrary数组表示可用的inputfilter

        // activeFilters表示针对每一个特定请求要使用的inputfilter
        activeFilters = new InputFilter[filterLibrary.length];

    }


    /**
     * Get filters.
     */
    public InputFilter[] getFilters() {

        return filterLibrary;

    }


    /**
     * Add an input filter to the filter library.
     */
    public void addActiveFilter(InputFilter filter) {

        if (lastActiveFilter == -1) {
            filter.setBuffer(inputStreamInputBuffer);
        } else {
            // 判断是否重复
            for (int i = 0; i <= lastActiveFilter; i++) {
                if (activeFilters[i] == filter)
                    return;
            }
            filter.setBuffer(activeFilters[lastActiveFilter]);
        }

        activeFilters[++lastActiveFilter] = filter;

        filter.setRequest(request);

    }


    /**
     * Set the swallow input flag.
     */
    public void setSwallowInput(boolean swallowInput) {
        this.swallowInput = swallowInput;
    }


    /**
     * Implementations are expected to call {@link Request#setStartTime(long)}
     * as soon as the first byte is read from the request.
     */
    public abstract boolean parseRequestLine(boolean useAvailableDataOnly)
        throws IOException;

    public abstract boolean parseHeaders() throws IOException;

    protected abstract boolean fill(boolean block) throws IOException;

    protected abstract void init(SocketWrapper<S> socketWrapper,
            AbstractEndpoint<S> endpoint) throws IOException;


    // --------------------------------------------------------- Public Methods


    /**
     * Recycle the input buffer. This should be called when closing the
     * connection.
     */
    public void recycle() {

        // Recycle Request object
        request.recycle();

        // Recycle filters
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        lastValid = 0;
        pos = 0;
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;

    }


    /**
     * End processing of current HTTP request.
     * Note: All bytes of the current request should have been already
     * consumed. This method only resets all the pointers so that we are ready
     * to parse the next HTTP request.
     */
    public void nextRequest() {

        // Recycle Request object
        request.recycle();

        // Copy leftover bytes to the beginning of the buffer
        //
        if (lastValid - pos > 0 && pos > 0) {
            // 把buf中从pos位置开始的数据，复制到buf中的第0个位置，长度就为多读的数据
            System.arraycopy(buf, pos, buf, 0, lastValid - pos);
        }
        // Always reset pos to zero
        // 把lastValid和pos重置为正确的位置
        lastValid = lastValid - pos;
        pos = 0;

        // Recycle filters
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        // Reset pointers
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;
    }


    /**
     * End request (consumes leftover bytes).
     *
     * @throws IOException an underlying I/O error occurred
     */
    public void endRequest() throws IOException {

        if (swallowInput && (lastActiveFilter != -1)) {
            // 多度的数据
            int extraBytes = (int) activeFilters[lastActiveFilter].end();
            pos = pos - extraBytes; // 把pos向前移动
        }
    }


    /**
     * Available bytes in the buffers (note that due to encoding, this may not
     * correspond).
     */
    public int available() {
        int result = (lastValid - pos);
        if ((result == 0) && (lastActiveFilter >= 0)) {
            for (int i = 0; (result == 0) && (i <= lastActiveFilter); i++) {
                result = activeFilters[i].available();
            }
        }
        return result;
    }


    // ---------------------------------------------------- InputBuffer Methods

    /**
     * Read some bytes.
     */
    @Override
    public int doRead(ByteChunk chunk, Request req)
        throws IOException {
        // 如果没有ActiveFilter，则直接从inputStreamInputBuffer中读取
        // 如果有ActiveFilter，则调用对应的ActiveFilter读取
        // 要么是IdentityInputFilter: 每次读多少不确定，看能从操作系统拿到多少
        // 要么是ChunkedInputFilter: 客户端分块发送的，ChunkedInputFilter一次读一块数据
        // 要么是VoidInputFilter：直接读不到数据，不管到底有没有请求体

        if (lastActiveFilter == -1)
            return inputStreamInputBuffer.doRead(chunk, req);
        else
            return activeFilters[lastActiveFilter].doRead(chunk,req);

    }
}
