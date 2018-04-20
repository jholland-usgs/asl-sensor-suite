package asl.sensor.test;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import asl.sensor.experiment.SineExperiment;
import asl.sensor.input.DataStore;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.mseed.SeedFormatException;

public class SineTest {

  public static String folder = TestUtils.TEST_DATA_LOCATION + TestUtils.SUBPAGE;

  private static DataStore ds;

  @Before
  public void setUpData() {
    ds = new DataStore();
    String calFname = folder + "sine-test/" + "_BC0.512.seed";
    String outFname = folder + "sine-test/" + "00_BHZ.512.seed";
    try {
      ds.setBlock(0, calFname);
      ds.setBlock(1, outFname);
      String startTimeString = "2015-166T20:23:08.5";
      String endTimeString = "2015-166T21:02:46.7";
      long start = TestUtils.timeStringToEpochMilli(startTimeString);
      long end = TestUtils.timeStringToEpochMilli(endTimeString);
      ds.trim(start, end);
    } catch (SeedFormatException | CodecException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  @Test
  public void testCalFrequency() {
    SineExperiment sexp = new SineExperiment();
    sexp.runExperimentOnData(ds);
    double ratio = sexp.getCalAmplitude() / sexp.getOutAmplitude();
    assertEquals(0.02768, ratio, 1E-3);
    double freq = sexp.getEstSineFreq();
    assertEquals(250, freq, 2.);
  }

}
