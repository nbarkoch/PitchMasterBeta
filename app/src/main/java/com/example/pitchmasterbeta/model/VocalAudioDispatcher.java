package com.example.pitchmasterbeta.model;

import android.annotation.SuppressLint;
import android.media.AudioRecord;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;


public class VocalAudioDispatcher implements Runnable {
    private static final Logger LOG = Logger.getLogger(be.tarsos.dsp.AudioDispatcher.class.getName());
    private final TarsosDSPAudioInputStream audioInputStream;
    private float[] audioFloatBuffer;
    private byte[] audioByteBuffer;
    private final List<AudioProcessor> audioProcessors = new CopyOnWriteArrayList<>();
    private final TarsosDSPAudioFloatConverter converter;
    private final TarsosDSPAudioFormat format;
    private int floatOverlap;
    private int floatStepSize;
    private int byteOverlap;
    private int byteStepSize;
    private long bytesToSkip;
    private long bytesProcessed;
    private AudioEvent audioEvent;
    private boolean stopped;
    private boolean paused;
    private boolean zeroPadFirstBuffer;
    private boolean zeroPadLastBuffer;

    public VocalAudioDispatcher(TarsosDSPAudioInputStream var1, int var2, int var3) {
        this.audioInputStream = var1;
        this.format = this.audioInputStream.getFormat();
        this.setStepSizeAndOverlap(var2, var3);
        this.audioEvent = new AudioEvent(this.format);
        this.audioEvent.setFloatBuffer(this.audioFloatBuffer);
        this.audioEvent.setOverlap(var3);
        this.converter = TarsosDSPAudioFloatConverter.getConverter(this.format);
        this.stopped = false;
        this.bytesToSkip = 0L;
        this.zeroPadLastBuffer = true;
    }

    public void skip(double var1) {
        this.bytesToSkip = Math.round(var1 * (double) this.format.getSampleRate()) * (long) this.format.getFrameSize();
    }

    public void setStepSizeAndOverlap(int var1, int var2) {
        this.audioFloatBuffer = new float[var1];
        this.floatOverlap = var2;
        this.floatStepSize = this.audioFloatBuffer.length - this.floatOverlap;
        this.audioByteBuffer = new byte[this.audioFloatBuffer.length * this.format.getFrameSize()];
        this.byteOverlap = this.floatOverlap * this.format.getFrameSize();
        this.byteStepSize = this.floatStepSize * this.format.getFrameSize();
    }

    public void setZeroPadFirstBuffer(boolean var1) {
        this.zeroPadFirstBuffer = var1;
    }

    public void setZeroPadLastBuffer(boolean var1) {
        this.zeroPadLastBuffer = var1;
    }

    public void addAudioProcessor(AudioProcessor var1) {
        this.audioProcessors.add(var1);
        LOG.fine("Added an audioprocessor to the list of processors: " + var1);
    }

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
                AudioProcessor var7 = (AudioProcessor) var2.next();
                if (!var7.process(this.audioEvent)) {
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
            var1 = this.audioInputStream.skip(this.bytesToSkip);
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

        try {
            this.audioInputStream.close();
        } catch (IOException var3) {
            LOG.log(Level.SEVERE, "Closing audio stream error.", var3);
        }
    }

    private int readNextAudioBlock() throws IOException {
        assert this.floatOverlap < this.audioFloatBuffer.length;

        boolean var1 = this.bytesProcessed == 0L || this.bytesProcessed == this.bytesToSkip;
        int var2;
        int var3;
        int var4;
        if (var1 && !this.zeroPadFirstBuffer) {
            var4 = this.audioByteBuffer.length;
            var2 = 0;
            var3 = 0;
        } else {
            var4 = this.byteStepSize;
            var2 = this.byteOverlap;
            var3 = this.floatOverlap;
        }

        if (!var1 && this.audioFloatBuffer.length == this.floatOverlap + this.floatStepSize) {
            System.arraycopy(this.audioFloatBuffer, this.floatStepSize, this.audioFloatBuffer, 0, this.floatOverlap);
        }

        int var5 = 0;
        boolean var6 = false;
        boolean var7 = false;

        while (!this.paused && !this.stopped && !var7 && var5 < var4) {
            int var11;
            try {
                var11 = this.audioInputStream.read(this.audioByteBuffer, var2 + var5, var4 - var5);
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
                for (int var8 = var2 + var5; var8 < this.audioByteBuffer.length; ++var8) {
                    this.audioByteBuffer[var8] = 0;
                }

                this.converter.toFloatArray(this.audioByteBuffer, var2, this.audioFloatBuffer, var3, this.floatStepSize);
            } else {
                byte[] var12 = this.audioByteBuffer;
                this.audioByteBuffer = new byte[var2 + var5];

                int var9;
                for (var9 = 0; var9 < this.audioByteBuffer.length; ++var9) {
                    this.audioByteBuffer[var9] = var12[var9];
                }

                var9 = var5 / this.format.getFrameSize();
                this.audioFloatBuffer = new float[var3 + var5 / this.format.getFrameSize()];
                this.converter.toFloatArray(this.audioByteBuffer, var2, this.audioFloatBuffer, var3, var9);
            }
        } else if (var4 == var5) {
            if (var1 && !this.zeroPadFirstBuffer) {
                this.converter.toFloatArray(this.audioByteBuffer, 0, this.audioFloatBuffer, 0, this.audioFloatBuffer.length);
            } else {
                this.converter.toFloatArray(this.audioByteBuffer, var2, this.audioFloatBuffer, var3, this.floatStepSize);
            }
        } else if (!this.stopped) {
            throw new IOException(String.format("The end of the audio stream has not been reached and the number of bytes read (%d) is not equal to the expected amount of bytes(%d).", var5, var4));
        }

        this.audioEvent.setFloatBuffer(this.audioFloatBuffer);
        this.audioEvent.setOverlap(var3);
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


    public static VocalAudioDispatcher fromDefaultMicrophone(int var0, int var1, int var2) {
        int var3 = AudioRecord.getMinBufferSize(var0, 16, 2);
        int var4 = var3 / 2;
        if (var4 <= var1) {
            @SuppressLint("MissingPermission")
            AudioRecord var5 = new AudioRecord(1, var0, 16, 2, var1 * 2);
            TarsosDSPAudioFormat var6 = new TarsosDSPAudioFormat((float) var0, 16, 1, true, false);
            AndroidAudioInputStream var7 = new AndroidAudioInputStream(var5, var6);
            var5.startRecording();
            return new VocalAudioDispatcher(var7, var1, var2);
        } else {
            throw new IllegalArgumentException("Buffer size too small should be at least " + var3 * 2);
        }
    }
}
