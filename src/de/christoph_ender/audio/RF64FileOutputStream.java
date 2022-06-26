
package de.christoph_ender.audio;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FilterOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class RF64FileOutputStream extends FilterOutputStream {

  public static final long maxUInt32Value = (2L << 31) - 1; // 4,294,967,295

  private static final byte[] maxUInt64ValueByteArray
    = new byte[]
    { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff };

  private static final long maxUInt64Value
    = Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16);

  private File outFile;
  private int bitsPerSample;
  private int numberOfChannels;
  private int sampleRate;
  private long dataLength;
  private boolean headerWritten = false;
  private long byteCounter = 0;
  private long maximumSize = maxUInt64Value;


  public RF64FileOutputStream(File outFile, int bitsPerSample,
      int numberOfChannels, int sampleRate, long dataLength
      ) throws IOException {
    super(new FileOutputStream(outFile));
    this.outFile = outFile;
    this.bitsPerSample = bitsPerSample;
    this.numberOfChannels = numberOfChannels;
    this.sampleRate = sampleRate;
    this.dataLength = dataLength;
  }


  public RF64FileOutputStream(File outFile, int bitsPerSample,
      int numberOfChannels, int sampleRate) throws IOException {
    this(outFile, bitsPerSample, numberOfChannels, sampleRate,
        maxUInt32Value);
  }


  public void close() throws IOException {
    writeHeaderIfRequired();
    super.close();

    RandomAccessFile randomOut = new RandomAccessFile(outFile, "rw");
    byte[] lengthBytes = new byte[8];
    randomOut.seek(20);
    writeTotalLengthToArray(lengthBytes, 0);
    randomOut.write(lengthBytes);
    randomOut.seek(28);
    writeDataLengthToArray(lengthBytes, 0);
    randomOut.write(lengthBytes);
    randomOut.close();
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


  protected void writeUnsignedLongToArray(byte[] b, int off, long data) {
    ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putLong(data);
    bb.flip();
    while (bb.hasRemaining()) {
      b[off++] = bb.get();
    }
  }


  protected void writeUnsignedIntToArray(byte[] b, int off, long data) {
      b[off  ] = (byte) ( data &        0xff);
      b[off+1] = (byte) ((data >>  8) & 0xff);
      b[off+2] = (byte) ((data >> 16) & 0xff);
      b[off+3] = (byte) ((data >> 24) & 0xff);
  }


  protected void writeTotalLengthToArray(byte[] header, int off) {
    writeUnsignedLongToArray(header, off, byteCounter + 36);
  }


  protected void writeDataLengthToArray(byte[] header, int off) {
    writeUnsignedLongToArray(header, off, byteCounter);
  }


  private void increaseAndTestByteCounter(long newBytes) throws IOException {
    long capacityLeft = maxUInt64Value - byteCounter;
    if (Long.compareUnsigned(byteCounter, capacityLeft) > 0) {
      throw new IOException("File too long.");
    }
    byteCounter += newBytes;
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

      byte[] header = new byte[80];

      // --- RIFF-Chunk ---

      header[ 0] = 'R'; 
      header[ 1] = 'F';
      header[ 2] = '6';
      header[ 3] = '4';

      writeUnsignedIntToArray(header, 4, maxUInt32Value);

      header[ 8] = 'W';
      header[ 9] = 'A';
      header[10] = 'V';
      header[11] = 'E';

      // --- DataSize64Chunk ---

      header[12] = 'd'; 
      header[13] = 's';
      header[14] = '6';
      header[15] = '4';

      writeUnsignedIntToArray(header, 16, 28);

      writeUnsignedLongToArray(header, 20, maxUInt64Value);
      writeUnsignedLongToArray(header, 28, maxUInt64Value);
      writeUnsignedLongToArray(header, 36, maxUInt64Value);
      writeUnsignedIntToArray(header, 44, 0);

      // --- 'fmt ' chunk ---

      header[48] = 'f'; 
      header[49] = 'm';
      header[50] = 't';
      header[51] = ' ';

      writeUnsignedIntToArray(header, 52, 16); // length of format data

      header[56] = 1; // PCM data
      header[57] = 0;

      header[58] = (byte) numberOfChannels; 
      header[59] = 0;

      // sample Rate
      writeUnsignedIntToArray(header, 60, sampleRate);

      writeUnsignedIntToArray(header, 64, bitRate);

      header[68] = (byte) frameSize;
      header[69] = 0;

      header[70] = (byte) bitsPerSample;
      header[71] = 0;

      // --- 'data' chunk

      header[72] = 'd';
      header[73] = 'a';
      header[74] = 't';
      header[75] = 'a';

      writeUnsignedIntToArray(header, 76, maxUInt32Value);

      increaseAndTestByteCounter(header.length);
      out.write(header);
      headerWritten = true;
    }
  }
}

