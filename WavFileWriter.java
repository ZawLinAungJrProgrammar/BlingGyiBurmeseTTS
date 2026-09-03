package com.blindgyi.tts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WavFileWriter {
    private File file;

    public WavFileWriter(File file) {
        this.file = file;
    }

    public void writeWavFile(byte[] pcmData, int sampleRate) throws IOException {
        int channels = 1;
        int bitDepth = 16;
        int byteRate = sampleRate * channels * (bitDepth / 8);
        int dataSize = pcmData.length;
        int totalDataLen = dataSize + 36;

        FileOutputStream out = new FileOutputStream(file);
        
        // WAV Header ရေးသားခြင်း
        out.write("RIFF".getBytes());
        out.write(intToByteArray(totalDataLen), 0, 4);
        out.write("WAVE".getBytes());
        out.write("fmt ".getBytes());
        out.write(intToByteArray(16), 0, 4); // SubChunk1Size (PCM = 16)
        out.write(shortToByteArray((short) 1), 0, 2); // AudioFormat (PCM = 1)
        out.write(shortToByteArray((short) channels), 0, 2);
        out.write(intToByteArray(sampleRate), 0, 4);
        out.write(intToByteArray(byteRate), 0, 4);
        out.write(shortToByteArray((short) (channels * (bitDepth / 8))), 0, 2); // BlockAlign
        out.write(shortToByteArray((short) bitDepth), 0, 2); // BitsPerSample
        out.write("data".getBytes());
        out.write(intToByteArray(dataSize), 0, 4);
        
        // Raw PCM Data ထည့်သွင်းခြင်း
        out.write(pcmData);
        out.close();
    }

    private byte[] intToByteArray(int i) {
        return new byte[] {
            (byte) (i & 0xFF),
            (byte) ((i >> 8) & 0xFF),
            (byte) ((i >> 16) & 0xFF),
            (byte) ((i >> 24) & 0xFF)
        };
    }

    private byte[] shortToByteArray(short s) {
        return new byte[] {
            (byte) (s & 0xFF),
            (byte) ((s >> 8) & 0xFF)
        };
    }
}
