package asl.sensor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.RealVector;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;
import asl.sensor.CalProcessingServer;
import asl.sensor.CalProcessingServer.RandData;
import asl.sensor.experiment.ExperimentEnum;
import asl.sensor.experiment.ExperimentFactory;
import asl.sensor.experiment.RandomizedExperiment;
import asl.sensor.gui.RandomizedPanel;
import asl.sensor.input.DataBlock;
import asl.sensor.input.DataStore;
import asl.sensor.input.InstrumentResponse;
import asl.sensor.utils.ReportingUtils;
import asl.sensor.utils.TimeSeriesUtils;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.mseed.SeedFormatException;

public class RandomizedExperimentTest {

  public static String folder = TestUtils.TEST_DATA_LOCATION + TestUtils.SUBPAGE;
  String testRespName = folder + "random-high-32+70i/RESP.XX.NS088..BHZ.STS1.360.2400";

  public DataStore getFromList(List<String> setUpFilenames) throws IOException {

    String respName = setUpFilenames.get(0);
    String calName = setUpFilenames.get(1);
    String sensOutName = setUpFilenames.get(2);

    System.out.println(respName);
    System.out.println(calName);
    System.out.println(sensOutName);

    InstrumentResponse ir = new InstrumentResponse(respName);

    DataStore ds = new DataStore();
    try {
      ds.setBlock(0, calName);
      ds.setBlock(1, sensOutName);
    } catch (SeedFormatException | CodecException e) {
      e.printStackTrace();
      fail();
    }

    ds.setResponse(1, ir);

    return ds;

  }

  @Test
  public void TestRandomCalCurves() {
    String fname = folder + "kiev-random-lowfrq/";
    String cal = "_BC0.512.seed";
    String out = "00_BH1.512.seed";
    try {
      InstrumentResponse ir = InstrumentResponse.loadEmbeddedResponse("STS25_Q330HR");
      DataBlock calB = TimeSeriesUtils.getFirstTimeSeries(fname + cal);
      DataBlock outB = TimeSeriesUtils.getFirstTimeSeries(fname + out);
      DataStore ds = new DataStore();
      ds.setBlock(0, calB);
      ds.setBlock(1, outB);
      ds.setResponse(1, ir);

      String startString = "2018-044T23:37:00.0";
      // String endString = "2018-045T07:37:00.0";
      long st = TestUtils.timeStringToEpochMilli(startString);
      long ed = st + (8 * 60 * 60 * 1000);
      ds.trim(st, ed);
      System.out.println("DATA LENGTH: " + ds.getBlock(0).getData().length);

      RandomizedExperiment re = new RandomizedExperiment();
      re.setLowFreq(true);
      re.runExperimentOnData(ds);

      Complex[] smooth = re.getSmoothedCalcResp();
      Complex[] unsmooth = re.getUnsmoothedCalcResp();
      double[] freqs = re.getFreqList();

      XYSeries smoothPlotA = new XYSeries("Smoothed response curve (amp)");
      XYSeries unsmoothPlotA = new XYSeries("Unsmoothed response curve (amp)");
      for (int i = 0; i < smooth.length; ++i) {
        double point = 20 * Math.log10(smooth[i].abs());
        smoothPlotA.add(freqs[i], point);
        unsmoothPlotA.add(freqs[i], 20 * Math.log10(unsmooth[i].abs()));
      }

      System.out.println("PSD data length? " + unsmooth.length);
      assertEquals(262144/2 + 1, re.getUntrimmedPSDLength());
      XYSeriesCollection xysc = new XYSeriesCollection();
      xysc.addSeries(unsmoothPlotA);
      xysc.addSeries(smoothPlotA);
      JFreeChart chart = ChartFactory.createXYLineChart(
          ExperimentEnum.RANDM.getName(),
          "Frequency (Hz)",
          "Power Amplitude (20 * log10)",
          xysc,
          PlotOrientation.VERTICAL,
          true,
          false,
          false);
      chart.getXYPlot().setDomainAxis( new LogarithmicAxis("Frequency (Hz) [log]") );

      BufferedImage bi = ReportingUtils.chartsToImage(1280, 960, chart);
      ImageIO.write(bi, "png", new File("testResultImages/smoothing-comparison.png") );

      StringBuilder smt = new StringBuilder("AMPLITUDE (smoothed):\t");
      StringBuilder unsmt = new StringBuilder("AMPLITUDE (unsmoothed):\t");
      DecimalFormat df = new DecimalFormat("#.########");
      for (int i = 0; i < 10; ++i) {
        double s = 20 * Math.log10(smooth[i].abs());
        double u = 20 * Math.log10(unsmooth[i].abs());
        unsmt.append(df.format(u));
        unsmt.append("\t");
        smt.append(df.format(s));
        smt.append("\t");
      }

      System.out.println(unsmt);

      Complex ref = new Complex(-0.01243, -0.01176);
      Complex got = re.getFitPoles().get(0);

      String msg = "Expected " + ref + " and got " + got;
      assertTrue(msg, Complex.equals(ref, got, 5E-4));

    } catch (IOException | SeedFormatException | CodecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      fail();
    }

  }

  // @Test
  public void TestRandomCal30s() {
    String fname = folder + "kiev-random-lowfrq/";
    String cal = "_BC0.512.seed";
    String out = "00_BH1.512.seed";
    try {
      InstrumentResponse ir = InstrumentResponse.loadEmbeddedResponse("STS25_Q330HR");
      DataBlock calB = TimeSeriesUtils.getFirstTimeSeries(fname + cal);
      DataBlock outB = TimeSeriesUtils.getFirstTimeSeries(fname + out);
      DataStore ds = new DataStore();
      ds.setBlock(0, calB);
      ds.setBlock(1, outB);
      ds.setResponse(1, ir);

      String startString = "2018-044T23:37:00.0";
      long st = TestUtils.timeStringToEpochMilli(startString);
      long ed = st + (8 * 60 * 60 * 1000);
      ds.trim(st, ed);
      System.out.println("DATA LENGTH: " + ds.getBlock(0).getData().length);

      RandomizedExperiment re = new RandomizedExperiment();
      re.setLowFreq(true);
      re.runExperimentOnData(ds);

      Complex[] smooth = re.getSmoothedCalcResp();
      Complex[] unsmooth = re.getUnsmoothedCalcResp();
      double[] freqs = re.getFreqList();

      assertEquals(freqs.length, unsmooth.length);

      double deltaFreq = freqs[1] - freqs[0];
      int indexOfInterest = (int) ( (.02 - freqs[0])/deltaFreq);

      for (int i = 0; i < freqs.length; ++i) {

        if (i == indexOfInterest) {
          assertEquals(0.02, freqs[i], 1E-3);
          assertEquals(0., 20 * Math.log10(unsmooth[i].abs()), 1E-4);
        }

        if (i == 423) {
          assertEquals(1./30., freqs[i], deltaFreq);
          assertEquals(0.159, 20 * Math.log10(unsmooth[i].abs()), 1E-4);
        }
      }


    } catch (IOException | SeedFormatException | CodecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      fail();
    }

  }

  @Test
  public void ResponseCorrectConvertedToVectorHighFreq() {
    String fname = folder + "resp-parse/TST5_response.txt";
    boolean lowFreq = false;
    InstrumentResponse ir;
    try {

      ir = new InstrumentResponse(fname);
      List<Complex> poles = new ArrayList<>(ir.getPoles());
      // using an unnecessarily high nyquist rate here
      RealVector high = ir.polesToVector(lowFreq, 1E8);

      int complexIndex = 2; // start at second pole
      int vectorIndex = 0;

      while ( vectorIndex < high.getDimension() ) {
        // return current index
        double real = high.getEntry(vectorIndex++);
        double imag = high.getEntry(vectorIndex++);

        double poleImag = poles.get(complexIndex).getImaginary();

        assertEquals( real, poles.get(complexIndex).getReal(), 0.0 );
        assertEquals( imag, poleImag, 0.0 );

        if (poleImag != 0) {
          // complex conjugate case
          ++complexIndex;
          assertEquals( real, poles.get(complexIndex).getReal(), 0.0 );
          assertEquals( imag, -poles.get(complexIndex).getImaginary(), 0.0 );
        }

        ++complexIndex;

      }

    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }

  @Test
  public void ResponseCorrectlyConvertedToVectorLowFreq() {
    String fname = folder + "resp-parse/TST5_response.txt";
    boolean lowFreq = true;
    InstrumentResponse ir;
    try {

      ir = new InstrumentResponse(fname);
      List<Complex> poles = new ArrayList<>(ir.getPoles());
      // again, use a very high nyquist rate
      RealVector low = ir.polesToVector(lowFreq, 1E8);

      // only test lower two poless
      assertEquals( low.getEntry(0), poles.get(0).getReal(), 0.0 );
      assertEquals( low.getEntry(1), poles.get(0).getImaginary(), 0.0 );

      assertEquals( low.getEntry(0), poles.get(1).getReal(), 0.0 );
      assertEquals( low.getEntry(1), -poles.get(1).getImaginary(), 0.0 );

    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }

  @Test
  public void ResponseSetCorrectlyHighFreq() {
    String fname = folder + "resp-parse/TST5_response.txt";
    InstrumentResponse ir;

    try {
      ir = new InstrumentResponse(fname);
      boolean lowFreq = false;

      List<Complex> poles = new ArrayList<>(ir.getPoles());
      List<Complex> replacements = new ArrayList<>();

      int start = 2;
      if ( poles.get(0).getImaginary() == 0 ) {
        start = 1;
      }

      for (int i = start; i < poles.size(); ++i) {
        if ( poles.get(i).getImaginary() == 0 ) {
          Complex c = poles.get(i);
          replacements.add(c.subtract(1));
          int next = i+1;
          while (next < poles.size() && poles.get(next).equals(c)) {
            ++next; // skip duplicates
          }
        } else {
          Complex c = poles.get(i);
          c = c.subtract( new Complex(1, 1) );
          replacements.add(c);
          ++i;
        }
      }

      //System.out.println(poles);
      //System.out.println(replacements);

      double[] newPoles = new double[replacements.size() * 2];
      for (int i = 0; i < newPoles.length; i += 2) {
        int poleIdx = i / 2;
        Complex c = replacements.get(poleIdx);
        newPoles[i] = c.getReal();
        newPoles[i + 1] = c.getImaginary();
      }

      InstrumentResponse ir2 =
          ir.buildResponseFromFitVector(newPoles, lowFreq, 0);

      List<Complex> testList = ir2.getPoles();
      //System.out.println(testList);
      int offsetIdx = 0;
      for (int i = 0; i < poles.size(); ++i) {
        if (i < start) {
          assertTrue( poles.get(i).equals( testList.get(i) ) );
        } else {
          Complex c = replacements.get(offsetIdx);
          assertTrue( testList.get(i).equals(c) );
          if ( poles.get(i).getImaginary() != 0 ) {
            Complex c1 = new Complex(1, 1);
            assertTrue(poles.get(i).equals( c.add(c1) ));
            ++i;
            Complex c2 = new Complex(1, -1);
            assertTrue( testList.get(i).equals( c.conjugate() ) );
            assertTrue( poles.get(i).equals( c.conjugate().add(c2) ) );
          } else {
            assertTrue( poles.get(i).equals(c.add(1)) );
          }
          ++offsetIdx;
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void responseSetCorrectlyLowFreq() {
    String fname = folder + "resp-parse/TST5_response.txt";
    InstrumentResponse ir;
    try {
      ir = new InstrumentResponse(fname);
      boolean lowFreq = true;
      List<Complex> poles = new ArrayList<>(ir.getPoles());

      double[] newPoles = new double[2];
      newPoles[0] = 0.;
      newPoles[1] = 1.;

      Complex c = new Complex( newPoles[0], newPoles[1] );

      InstrumentResponse ir2 =
          ir.buildResponseFromFitVector(newPoles, lowFreq, 0);
      List<Complex> poles2 = ir2.getPoles();

      List<Complex> testList = new ArrayList<>(poles);
      testList.set(0, c);
      testList.set( 1, c.conjugate() );

      // System.out.println(testList);
      // System.out.println(poles);
      // System.out.println(poles2);

      for (int i = 0; i < poles.size(); ++i) {
        if (i < 2) {
          assertFalse( poles.get(i).equals( poles2.get(i) ) );
          assertTrue( poles2.get(i).equals( testList.get(i) ) );
        }
      }


    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }

  }

  public DataStore setUpTest1() throws IOException {

    List<String> fileList = new ArrayList<>();
    String respName = testRespName;
    String dataFolderName = folder + "random-high-32+70i/";
    String calName =  dataFolderName + "_EC0.512.seed";
    String sensOutName = dataFolderName + "00_EHZ.512.seed";

    fileList.add(respName);
    fileList.add(calName);
    fileList.add(sensOutName);

    DataStore ds = getFromList(fileList);
    OffsetDateTime cCal = TestUtils.getStartCalendar(ds);

    cCal = cCal.withMinute(36);
    cCal = cCal.withSecond(0);
    long start = cCal.toInstant().toEpochMilli();

    cCal = cCal.withMinute(41);
    // System.out.println( "end: " + sdf.format( cCal.getTime() ) );
    long end = cCal.toInstant().toEpochMilli();

    ds.trim(start, end);

    return ds;
  }

  @Test
  public void testCalculationResult1() {

    String currentDir = System.getProperty("user.dir");
    // int testNumber = 3; // use to switch automated report data
    boolean lowFreq = false;

    try {

      DataStore ds = setUpTest1();
      InstrumentResponse ir = ds.getResponse(1);

      double nyq = ds.getBlock(0).getSampleRate() / 2.;
      System.out.println("NYQUIST RATE: " + nyq);

      RandomizedExperiment rCal = (RandomizedExperiment)
          ExperimentFactory.createExperiment(ExperimentEnum.RANDM);

      rCal.setLowFreq(lowFreq);

      assertTrue( rCal.hasEnoughData(ds) );
      rCal.runExperimentOnData(ds);

      double bestResid = rCal.getFitResidual();

      int width = 1280;
      int height = 960;

      List<XYSeriesCollection> xysc = rCal.getData();
      String[] yAxisTitles = new String[]{"Resp(f), dB", "Angle / TAU"};
      JFreeChart[] jfcl = new JFreeChart[yAxisTitles.length];

      /*
      // one-time set of coding to placate Ringler, outputting data
      double[][] ampRespIn = xysc.get(0).getSeries(0).toArray();
      double[][] phsRespIn = xysc.get(1).getSeries(0).toArray();
      double[] freqs = ampRespIn[0];
      double[] amps = ampRespIn[1];
      double[] phas = phsRespIn[1];
      StringBuilder sbTemp = new StringBuilder();
      for (int i = 0; i < freqs.length; ++i) {
        sbTemp.append(freqs[i]);
        sbTemp.append(": ");
        sbTemp.append(amps[i]);
        sbTemp.append(", ");
        sbTemp.append(phas[i]);
        sbTemp.append("\n");
      }
      PrintWriter strOut = new PrintWriter("testResultImages/STS1-power.txt");
      strOut.write( sbTemp.toString() );
      strOut.close();
       */

      String xAxisTitle = "Frequency (Hz)";
      NumberAxis xAxis = new LogarithmicAxis(xAxisTitle);
      Font bold = xAxis.getLabelFont().deriveFont(Font.BOLD);
      xAxis.setLabelFont(bold);

      StringBuilder sb = new StringBuilder();

      String[] resultString = RandomizedPanel.getInsetString(rCal);
      for (String resultPart : resultString) {
        sb.append( resultPart );
        sb.append('\n');
      }
      sb.append( RandomizedPanel.getTimeStampString(rCal) );
      sb.append('\n');
      sb.append("Input files:\n");
      sb.append( ds.getBlock(0).getName() );
      sb.append(" (calibration)\n");
      sb.append( ds.getBlock(1).getName() );
      sb.append(" (sensor output)\n");
      sb.append("Response file used:\n");
      sb.append( ds.getResponse(1).getName() );
      sb.append("\n \n");

      String page1 = sb.toString();

      String[] addtlPages = ( RandomizedPanel.getAdditionalReportPages(rCal) );
      // technically 'page 2' but really second part of first dataset report
      // and I'm too lazy to rename everything to reflect that
      String page1Part2 = addtlPages[0];

      sb = new StringBuilder();

      // expected best fit params, for debugging
      sb.append("BELOW RESULTS FOR EXPECTED BEST FIT (YELLOW CURVE)\n");
      double[] expectedParams = new double[]{-3.580104E+1, +7.122400E+1};
      ir = ir.buildResponseFromFitVector(expectedParams, lowFreq, 0);
      ir.setName("Best-fit params");
      ds.setResponse(1, ir);
      rCal.runExperimentOnData(ds);

      // residual from other code's best-fit parameters
      // compare to best-fit residual and assume difference is < 5%
      double expectedResid = rCal.getInitResidual();

      double pctDiff =
          Math.abs( 100 * (bestResid - expectedResid) / bestResid );

      if (pctDiff > 15) {
        System.out.println(rCal.getFitPoles());
        System.out.println(rCal.getInitialPoles());
        System.out.println(bestResid + ", " + expectedResid);
      }

      // TODO: add corrected assert here to compare best-fit and expected result
      //assertTrue("PCT DIFF EXPECTED <15%, GOT " + pctDiff, pctDiff < 15);

      // add initial curve from expected fit params to report
      XYSeries expectedInitialCurve = rCal.getData().get(0).getSeries(0);
      xysc.get(0).addSeries(expectedInitialCurve);
      XYSeries expectedInitialAngle = rCal.getData().get(1).getSeries(0);
      xysc.get(1).addSeries(expectedInitialAngle);

      resultString = RandomizedPanel.getInsetString(rCal);
      for (String resultPart : resultString) {
        sb.append( resultPart );
        sb.append('\n');
      }

      for (int i = 0; i < jfcl.length; ++i) {

        jfcl[i] = ChartFactory.createXYLineChart(
            ExperimentEnum.RANDM.getName(),
            xAxisTitle,
            yAxisTitles[i],
            xysc.get(i),
            PlotOrientation.VERTICAL,
            true,
            false,
            false);

        XYPlot xyp = jfcl[i].getXYPlot();

        //xyp.clearAnnotations();
        //xyp.addAnnotation(xyt);

        xyp.setDomainAxis(xAxis);
      }

      String page2 = sb.toString();

      PDDocument pdf = new PDDocument();
      ReportingUtils.chartsToPDFPage(width, height, pdf, jfcl);
      ReportingUtils.textListToPDFPages(pdf, page1, page1Part2, page2);

      String testResultFolder = currentDir + "/testResultImages/";
      File dir = new File(testResultFolder);
      if ( !dir.exists() ) {
        dir.mkdir();
      }

      String testResult =
          testResultFolder + "Random-Calib-Test-1.pdf";
      pdf.save( new File(testResult) );
      pdf.close();
      System.out.println("Output result has been written");

    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testCalServerEntryMethods() {
    String testFolder = folder + "test-crashed-on-cal/";
    String calInFile = testFolder + "_BC0.512.seed";
    String sensorOutFile = testFolder + "00_BHZ.512.seed";
    String respFile = testFolder + "RESP.US.MVCO.00.BHZ";
    String start = "2018-01-30T07:55:00+00:00";
    String end = "2018-01-30T11:55:00+00:00";
    try {
      CalProcessingServer cps = new CalProcessingServer();
      RandData rd = cps.populateDataAndRun(calInFile, sensorOutFile, respFile,
          false, start, end, true);
      System.out.println( Arrays.toString(rd.getFitPoles()) );
      System.out.println( Arrays.toString(rd.getFitZeros()) );
    } catch (IOException | SeedFormatException | CodecException e) {
      e.printStackTrace();
      fail();
    }
  }

}