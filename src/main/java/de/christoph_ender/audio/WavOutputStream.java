
package de.christoph_ender.audio;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FilterOutputStream;


public class WavOutputStream extends FilterOutputStream {

  public static enum mode {
    DONT_IGNORE_MAX_SIZE,
    IGNORE_MAX_SIZE
  };

  public static final long maxUInt32Value = (2L << 31) - 1; // 4,294,967,295

  private int bitsPerSample;
  private int numberOfChannels;
  private int sampleRate;
  private long dataLength;
  private boolean headerWritten = false;
  private long byteCounter = 0;
  private long maximumSize = maxUInt32Value;


  public WavOutputStream(OutputStream out, mode ignoreMode, int bitsPerSample,
      int numberOfChannels, int sampleRate, long dataLength
      ) throws IOException {
    super(out);
    if (dataLength > maxUInt32Value && ignoreMode != mode.IGNORE_MAX_SIZE) {
      throw new IOException("File too long.");
    }
    this.bitsPerSample = bitsPerSample;
    this.numberOfChannels = numberOfChannels;
    this.sampleRate = sampleRate;
    this.dataLength = dataLength;
  }


  public WavOutputStream(OutputStream out, mode ignoreMode, int bitsPerSample,
      int numberOfChannels, int sampleRate) throws IOException {
    this(out, ignoreMode, bitsPerSample, numberOfChannels, sampleRate,
        maxUInt32Value);
  }


  public void close() throws IOException {
    writeHeaderIfRequired();
    super.close();
  }


  public void flush()  throws IOException {
    writeHeaderIfRequired();
    super.flush();
  }


  public void write(byte[] b) throws IOException {
    writeHeaderIfRequired();
    increaseAndTestByteCounter(b.length);
    out.write(b);
  }


  public void write(byte[] b, int off, int len) throws IOException {
    writeHeaderIfRequired();
    increaseAndTestByteCounter(len);
    out.write(b);
  }


  public void write(byte b) throws IOException {
    writeHeaderIfRequired();
    increaseAndTestByteCounter(1);
    out.write(b);
  }


  protected void writeUnsignedIntToArray(byte[] b, int off, long data) {
      b[off  ] = (byte) ( data &        0xff);
      b[off+1] = (byte) ((data >>  8) & 0xff);
      b[off+2] = (byte) ((data >> 16) & 0xff);
      b[off+3] = (byte) ((data >> 24) & 0xff);
  }


  protected void writeTotalLengthToArray(byte[] header, int off) {
    writeUnsignedIntToArray(header, off, byteCounter + 36);
  }


  protected void writeDataLengthToArray(byte[] header, int off) {
    writeUnsignedIntToArray(header, off, byteCounter);
  }


  private void increaseAndTestByteCounter(long newBytes) throws IOException {
    byteCounter += newBytes;
    if (byteCounter > maximumSize) {
      throw new IOException("File too long.");
    }
  }


  private void writeHeaderIfRequired() throws IOException {
    if (headerWritten == false) {
      if (dataLength > maxUInt32Value - 36) {
        dataLength = maxUInt32Value - 36;
      }
      long totalDataLength
        = dataLength + 36;
      long bitRate
        = sampleRate * numberOfChannels * bitsPerSample / 8;  // bytes/second
      int frameSize
        = numberOfChannels * ((bitsPerSample + 7) / 8);  // +7 for rounding

      byte[] header = new byte[44];

      header[ 0] = 'R'; 
      header[ 1] = 'I';
      header[ 2] = 'F';
      header[ 3] = 'F';

      writeUnsignedIntToArray(header, 4, totalDataLength);

      header[ 8] = 'W';
      header[ 9] = 'A';
      header[10] = 'V';
      header[11] = 'E';

      header[12] = 'f'; 
      header[13] = 'm';
      header[14] = 't';
      header[15] = ' ';

      header[16] = 16; // fixed, length of format data above.
      header[17] = 0;
      header[18] = 0;
      header[19] = 0;

      header[20] = 1; // PCM data
      header[21] = 0;

      header[22] = (byte) numberOfChannels; 
      header[23] = 0;

      writeUnsignedIntToArray(header, 24, sampleRate);

      writeUnsignedIntToArray(header, 28, bitRate);

      header[32] = (byte) frameSize;
      header[33] = 0;

      header[34] = (byte) bitsPerSample;
      header[35] = 0;

      header[36] = 'd';
      header[37] = 'a';
      header[38] = 't';
      header[39] = 'a';

      writeUnsignedIntToArray(header, 40, dataLength);

      increaseAndTestByteCounter(header.length);
      out.write(header);
      headerWritten = true;
    }
  }
}

