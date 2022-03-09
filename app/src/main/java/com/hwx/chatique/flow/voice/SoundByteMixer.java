package com.hwx.chatique.flow.voice;

//https://stackoverflow.com/a/60721038
//https://stackoverflow.com/a/65836852
public class SoundByteMixer {

    public static byte[] mix(final byte[] a, final byte[] b, final boolean bigEndian) {
        final byte[] aa;
        final byte[] bb;

        final int length = Math.max(a.length, b.length);
        // ensure same lengths
        if (a.length != b.length) {
            aa = new byte[length];
            bb = new byte[length];
            System.arraycopy(a, 0, aa, 0, a.length);
            System.arraycopy(b, 0, bb, 0, b.length);
        } else {
            aa = a;
            bb = b;
        }

        // convert to samples
        final int[] aSamples = toSamples(aa, bigEndian);
        final int[] bSamples = toSamples(bb, bigEndian);

        // mix by adding
        final int[] mix = new int[aSamples.length];
        for (int i = 0; i < mix.length; i++) {
            // calculating the average
            mix[i] = (aSamples[i] + bSamples[i]) >> 1;
            // enforce min and max (may introduce clipping)
            //mix[i] = Math.min(Short.MAX_VALUE, mix[i]);
            //mix[i] = Math.max(Short.MIN_VALUE, mix[i]);
        }

        // convert back to bytes
        return toBytes(mix, bigEndian);
    }

    private static int[] toSamples(final byte[] byteSamples, final boolean bigEndian) {
        final int bytesPerChannel = 2;
        final int length = byteSamples.length / bytesPerChannel;
        if ((length % 2) != 0)
            throw new IllegalArgumentException("For 16 bit audio, length must be even: " + length);
        final int[] samples = new int[length];
        for (int sampleNumber = 0; sampleNumber < length; sampleNumber++) {
            final int sampleOffset = sampleNumber * bytesPerChannel;
            final int sample = bigEndian
                    ? byteToIntBigEndian(byteSamples, sampleOffset, bytesPerChannel)
                    : byteToIntLittleEndian(byteSamples, sampleOffset, bytesPerChannel);
            samples[sampleNumber] = sample;
        }
        return samples;
    }

    private static byte[] toBytes(final int[] intSamples, final boolean bigEndian) {
        final int bytesPerChannel = 2;
        final int length = intSamples.length * bytesPerChannel;
        final byte[] bytes = new byte[length];
        for (int sampleNumber = 0; sampleNumber < intSamples.length; sampleNumber++) {
            final byte[] b = bigEndian
                    ? intToByteBigEndian(intSamples[sampleNumber], bytesPerChannel)
                    : intToByteLittleEndian(intSamples[sampleNumber], bytesPerChannel);
            System.arraycopy(b, 0, bytes, sampleNumber * bytesPerChannel, bytesPerChannel);
        }
        return bytes;
    }

    // from https://github.com/hendriks73/jipes/blob/master/src/main/java/com/tagtraum/jipes/audio/AudioSignalSource.java#L238
    private static int byteToIntLittleEndian(final byte[] buf, final int offset, final int bytesPerSample) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            final int aByte = buf[offset + byteIndex] & 0xff;
            sample += aByte << 8 * (byteIndex);
        }
        return (short) sample;
    }

    // from https://github.com/hendriks73/jipes/blob/master/src/main/java/com/tagtraum/jipes/audio/AudioSignalSource.java#L247
    private static int byteToIntBigEndian(final byte[] buf, final int offset, final int bytesPerSample) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            final int aByte = buf[offset + byteIndex] & 0xff;
            sample += aByte << (8 * (bytesPerSample - byteIndex - 1));
        }
        return (short) sample;
    }

    private static byte[] intToByteLittleEndian(final int sample, final int bytesPerSample) {
        byte[] buf = new byte[bytesPerSample];
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            buf[byteIndex] = (byte) ((sample >>> (8 * byteIndex)) & 0xFF);
        }
        return buf;
    }

    private static byte[] intToByteBigEndian(final int sample, final int bytesPerSample) {
        byte[] buf = new byte[bytesPerSample];
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            buf[byteIndex] = (byte) ((sample >>> (8 * (bytesPerSample - byteIndex - 1))) & 0xFF);
        }
        return buf;
    }
}