package org.yamcs.web.rest;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.api.MediaType;
import org.yamcs.parameter.ParameterValueWithId;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.web.HttpException;
import org.yamcs.web.HttpRequestHandler;
import org.yamcs.web.HttpRequestHandler.ChunkedTransferStats;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;

/**
 * Reads a yamcs replay and maps it directly to an output buffer. If that buffer grows larger
 * than the treshold size for one chunk, this will cause a chunk to be written out.
 * Could maybe be replaced by using built-in netty functionality, but would need to investigate.
 */
public abstract class ParameterReplayToChunkedTransferEncoder extends RestParameterReplayListener {

    private static final Logger log = LoggerFactory.getLogger(ParameterReplayToChunkedTransferEncoder.class);
    private static final int CHUNK_TRESHOLD = 8096;

    private ByteBuf buf;
    protected ByteBufOutputStream bufOut;


    protected MediaType contentType;
    protected List<NamedObjectId> idList;
    protected boolean failed = false;
    private ChunkedTransferStats stats;

    public ParameterReplayToChunkedTransferEncoder(RestRequest req, MediaType contentType, List<NamedObjectId> idList) throws HttpException {
        super(req);
        this.contentType = contentType;
        this.idList = idList;
        resetBuffer();
        HttpRequestHandler.startChunkedTransfer(req.getChannelHandlerContext(), req.getHttpRequest(), contentType, null);
        stats = req.getChannelHandlerContext().attr(HttpRequestHandler.CTX_CHUNK_STATS).get();
    }

    protected void resetBuffer() {
        buf = req.getChannelHandlerContext().alloc().buffer();
        bufOut = new ByteBufOutputStream(buf);
    }

    protected void closeBufferOutputStream() throws IOException {
        bufOut.close();
    }

    @Override
    public void onParameterData(List<ParameterValueWithId> params) {
        if (failed) {
            log.warn("Already failed. Ignoring parameter data");
            return;
        }
        try {
            processParameterData(params, bufOut);
            if (buf.readableBytes() >= CHUNK_TRESHOLD) {
                closeBufferOutputStream();
                writeChunk();
                resetBuffer();
            }
        } catch (ClosedChannelException e) {
            log.info("Closing replay due to channel being closed");
            failed = true;
            requestReplayAbortion();
        } catch (IOException e) {
            log.error("Closing replay due to IO error", e);
            failed = true;
            requestReplayAbortion();
        }
    }

    public abstract void processParameterData(List<ParameterValueWithId> params, ByteBufOutputStream bufOut) throws IOException;

    @Override
    public void replayFinished() {
        if (failed) {
            Channel ch = req.getChannelHandlerContext().channel();
            if(ch.isOpen()) {
                log.warn("Closing channel because transfer failed");
                req.getChannelHandlerContext().channel().close();
            }
            return;
        }
        try {
            closeBufferOutputStream();
            if (buf.readableBytes() > 0) {
                writeChunk();
            }
            RestHandler.completeChunkedTransfer(req);
        } catch (IOException e) {
            log.error("Could not write final chunk of data", e);
        }
    }

    private void writeChunk() throws IOException {
        int txSize = buf.readableBytes();
        req.addTransferredSize(txSize);
        stats.totalBytes += txSize;
        stats.chunkCount++;
        HttpRequestHandler.writeChunk(req.getChannelHandlerContext(), buf);
    }
}
