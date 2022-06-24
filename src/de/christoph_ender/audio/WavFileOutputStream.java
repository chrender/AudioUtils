
package de.christoph_ender.audio;

import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FilterOutputStream;


public class WavFileOutputStream extends WavOutputStream {

  protected File outFile;
  protected RandomAccessFile out;


  public WavFileOutputStream(File outFile, mode ignoreMode,
      int bitsPerSample, int numberOfChannels, int sampleRate
      ) throws IOException {
    super(new FileOutputStream(outFile), ignoreMode, bitsPerSample,
        numberOfChannels, sampleRate);
    this.outFile = outFile;
  }


  public void close() throws IOException {
    super.close();

    RandomAccessFile randomOut = new RandomAccessFile(outFile, "rw");
    byte[] lengthBytes = new byte[4];
    randomOut.seek(4);
    writeTotalLengthToArray(lengthBytes, 0);
    randomOut.write(lengthBytes);
    randomOut.seek(40);
    writeDataLengthToArray(lengthBytes, 0);
    randomOut.write(lengthBytes);
    randomOut.close();
  }
}

