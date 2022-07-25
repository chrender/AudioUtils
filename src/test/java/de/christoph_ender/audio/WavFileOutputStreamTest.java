
package de.christoph_ender.audio;


import java.io.File;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class WavFileOutputStreamTest {

  private WavFileOutputStream wavFileOutputStream = null;

  @BeforeEach
  void setUp() {
    File file = new File("testfile");
    try {
      wavFileOutputStream = new WavFileOutputStream(
          file,
          WavOutputStream.mode.DONT_IGNORE_MAX_SIZE,
          16,
          2,
          44100);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Test simple file")
  void testSimpleFile() {
  }
}

