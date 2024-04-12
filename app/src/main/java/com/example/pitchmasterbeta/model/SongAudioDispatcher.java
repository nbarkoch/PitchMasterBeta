package com.example.pitchmasterbeta.model;


import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;


public class SongAudioDispatcher implements Runnable {
    private static final Logger LOG = Logger.getLogger(be.tarsos.dsp.AudioDispatcher.class.getName());
    private final TarsosDSPAudioInputStream audioInputStream1;
    private final TarsosDSPAudioInputStream audioInputStream2;
    private float[] audioFloatBuffer;
    private byte[] audioByteBuffer;
    private byte[] audioByteBuffer2;
    private final List<MicAudioProcessor> audioProcessors = new CopyOnWriteArrayList<>();
    private final TarsosDSPAudioFloatConverter converter;
    private final TarsosDSPAudioFormat format;
    private int floatOverlap;
    private int floatStepSize;
    private int byteOverlap;
    private int byteStepSize;
    private long bytesToSkip;
    private long bytesProcessed;
    private final AudioEvent audioEvent;
    private boolean stopped;
    private boolean paused;
    private boolean zeroPadFirstBuffer;
    private boolean zeroPadLastBuffer;

    public interface MicAudioProcessor extends AudioProcessor{
        boolean process(AudioEvent var1, byte[] secondFloatingBuffer);
    }

    public SongAudioDispatcher(TarsosDSPAudioInputStream stream1, TarsosDSPAudioInputStream stream2, int var2, int var3) {
        this.audioInputStream1 = stream1;
        this.audioInputStream2 = stream2;
        // have to be the same configuration for both streams!!
        this.format = this.audioInputStream1.getFormat();
        this.setStepSizeAndOverlap(var2, var3);
        this.audioEvent = new AudioEvent(this.format);
        this.audioEvent.setFloatBuffer(this.audioFloatBuffer);
        this.audioEvent.setOverlap(var3);
        this.converter = TarsosDSPAudioFloatConverter.getConverter(this.format);
        this.stopped = false;
        this.bytesToSkip = 0L;
        this.zeroPadLastBuffer = true;
    }

    public void skipBytes(long bytesToSkip) {
        try {
            this.audioInputStream1.skip(bytesToSkip);
            this.audioInputStream2.skip(bytesToSkip);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        this.bytesProcessed = bytesToSkip;
        this.audioEvent.setBytesProcessed(this.bytesProcessed);
    }

    public void setStepSizeAndOverlap(int var1, int var2) {
        this.audioFloatBuffer = new float[var1];
        this.floatOverlap = var2;
        this.floatStepSize = this.audioFloatBuffer.length - this.floatOverlap;
        this.audioByteBuffer = new byte[this.audioFloatBuffer.length * this.format.getFrameSize()];
        this.audioByteBuffer2 = new byte[this.audioFloatBuffer.length * this.format.getFrameSize()];
        this.byteOverlap = this.floatOverlap * this.format.getFrameSize();
        this.byteStepSize = this.floatStepSize * this.format.getFrameSize();
    }

    public void setZeroPadFirstBuffer(boolean var1) {
        this.zeroPadFirstBuffer = var1;
    }

    public void setZeroPadLastBuffer(boolean var1) {
        this.zeroPadLastBuffer = var1;
    }

    public void addAudioProcessor(MicAudioProcessor var1) {
        this.audioProcessors.add(var1);
        LOG.fine("Added an audioprocessor to the list of processors: " + var1);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void removeAudioProcessor(AudioProcessor var1) {
        this.audioProcessors.remove(var1);
        var1.processingFinished();
        LOG.fine("Remove an audioprocessor to the list of processors: " + var1);
    }

    public void run() {
        boolean var1 = false;
        if (this.bytesToSkip != 0L) {
            this.skipToStart();
        }

        String var3;
        int var6;
        try {
            this.audioEvent.setBytesProcessed(this.bytesProcessed);
            var6 = this.readNextAudioBlock();
        } catch (IOException var5) {
            var3 = "Error while reading audio input stream: " + var5.getMessage();
            LOG.warning(var3);
            throw new Error(var3);
        }

        while (var6 != 0 && !this.stopped) {
            if (paused) continue;

            Iterator var2 = this.audioProcessors.iterator();

            while (var2.hasNext()) {
                MicAudioProcessor var7 = (MicAudioProcessor) var2.next();
                if (!var7.process(this.audioEvent, this.audioByteBuffer2)) {
                    break;
                }
            }

            if (!this.paused && !this.stopped) {
                this.bytesProcessed += (long) var6;
                this.audioEvent.setBytesProcessed(this.bytesProcessed);

                try {
                    var6 = this.readNextAudioBlock();
                    this.audioEvent.setOverlap(this.floatOverlap);
                } catch (IOException var4) {
                    var3 = "Error while reading audio input stream: " + var4.getMessage();
                    LOG.warning(var3);
                    throw new Error(var3);
                }
            }
        }

        if (!this.stopped) {
            this.stop();
        }

    }

    private void skipToStart() {
        long var1 = 0L;

        try {
            var1 = this.audioInputStream1.skip(this.bytesToSkip);
            if (var1 != this.bytesToSkip) {
                throw new IOException();
            } else {
                this.bytesProcessed += this.bytesToSkip;
            }
        } catch (IOException var5) {
            String var4 = String.format("Did not skip the expected amount of bytes,  %d skipped, %d expected!", var1, this.bytesToSkip);
            LOG.warning(var4);
            throw new Error(var4);
        }
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public void stop() {
        this.stopped = true;
        Iterator var1 = this.audioProcessors.iterator();

        while (var1.hasNext()) {
            AudioProcessor var2 = (AudioProcessor) var1.next();
            var2.processingFinished();
        }

//        try {
//            this.audioInputStream1.close();
//            this.audioInputStream2.close();
//        } catch (IOException var3) {
//            LOG.log(Level.SEVERE, "Closing audio stream error.", var3);
//        }
    }

    //BufferSize, ByteBuffer(we can skip this)

    private int readNextAudioBlock() throws IOException {
        assert this.floatOverlap < this.audioFloatBuffer.length;

        boolean var1 = this.bytesProcessed == 0L || this.bytesProcessed == this.bytesToSkip;
        int byteOverlap;
        int floatOverlap;
        int byteStepSize;
        if (var1 && !this.zeroPadFirstBuffer) {
            byteStepSize = this.audioByteBuffer.length;
            byteOverlap = 0;
            floatOverlap = 0;
        } else {
            byteStepSize = this.byteStepSize;
            byteOverlap = this.byteOverlap;
            floatOverlap = this.floatOverlap;
        }

        if (!var1 && this.audioFloatBuffer.length == this.floatOverlap + this.floatStepSize) {
            System.arraycopy(this.audioFloatBuffer, this.floatStepSize, this.audioFloatBuffer, 0, this.floatOverlap);
        }

        int var5 = 0;
        boolean var6 = false;
        boolean var7 = false;

        while (!this.paused && !this.stopped && !var7 && var5 < byteStepSize) {
            int var11;
            try {
                var11 = this.audioInputStream1.read(this.audioByteBuffer, byteOverlap + var5, byteStepSize - var5);
                this.audioInputStream2.read(this.audioByteBuffer2, byteOverlap + var5, byteStepSize - var5);
            } catch (IndexOutOfBoundsException var10) {
                var11 = -1;
            }

            if (var11 == -1) {
                var7 = true;
            } else {
                var5 += var11;
            }
        }

        if (var7) {
            if (this.zeroPadLastBuffer) {
                for (int var8 = byteOverlap + var5; var8 < this.audioByteBuffer.length; ++var8) {
                    this.audioByteBuffer[var8] = 0;
                }

                this.converter.toFloatArray(this.audioByteBuffer, byteOverlap, this.audioFloatBuffer, floatOverlap, this.floatStepSize);
            } else {
                byte[] var12 = this.audioByteBuffer;
                this.audioByteBuffer = new byte[byteOverlap + var5];

                int var9;
                for (var9 = 0; var9 < this.audioByteBuffer.length; ++var9) {
                    this.audioByteBuffer[var9] = var12[var9];
                }

                var9 = var5 / this.format.getFrameSize();
                this.audioFloatBuffer = new float[floatOverlap + var5 / this.format.getFrameSize()];
                this.converter.toFloatArray(this.audioByteBuffer, byteOverlap, this.audioFloatBuffer, floatOverlap, var9);
            }
        } else if (byteStepSize == var5) {
            if (var1 && !this.zeroPadFirstBuffer) {
                this.converter.toFloatArray(this.audioByteBuffer, 0, this.audioFloatBuffer, 0, this.audioFloatBuffer.length);
            } else {
                this.converter.toFloatArray(this.audioByteBuffer, byteOverlap, this.audioFloatBuffer, floatOverlap, this.floatStepSize);
            }
        } else if (!this.stopped) {
            throw new IOException(String.format("The end of the audio stream has not been reached and the number of bytes read (%d) is not equal to the expected amount of bytes(%d).", var5, byteStepSize));
        }

        this.audioEvent.setFloatBuffer(this.audioFloatBuffer);
        this.audioEvent.setOverlap(floatOverlap);
        return var5;
    }

    public TarsosDSPAudioFormat getFormat() {
        return this.format;
    }

    public float secondsProcessed() {
        return (float) (this.bytesProcessed / (long) (this.format.getSampleSizeInBits() / 8)) / this.format.getSampleRate() / (float) this.format.getChannels();
    }

    public void setAudioFloatBuffer(float[] var1) {
        this.audioFloatBuffer = var1;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}

