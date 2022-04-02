package Client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

//The number of successful requests are: 179800
//The number of failed requests are: 0
//The wall time for all phases to complete are: 118591 milliseconds
//The overall throughput in requests per second is: 1516.1352885126191

public class SkierClient {

  private static final int THREAD_INDEX = 0;
  private static final int SKIERS_INDEX = 1;
  private static final int LIFTS_INDEX = 2;
  private static final int RUNS_INDEX = 3;
  private static final int IP_INDEX = 4;
  private static final int LAUNCHED_THREADS_CONVERT_PHASE1 = 4;
  private static final int LAUNCHED_THREADS_CONVERT_PHASE3 = 10;
  private static final double RUNS_CONVERT_PHASE1 = 0.2;
  private static final double RUNS_CONVERT_PHASE2 = 0.6;
  private static final double RUNS_CONVERT_PHASE3 = 0.1;
  private static final int PHASE1_START = 1;
  private static final int PHASE1_END = 90;
  private static final int PHASE2_START = 91;
  private static final int PHASE2_END = 360;
  private static final int PHASE3_START = 361;
  private static final int PHASE3_END = 420;
  private static final double TWENTY_PERCENT_MARK = 0.2;
  private static final double MILLISECONDS_TO_SECONDS_CONVERT = 1000d;
  private static final String URL_PREFIX = "http://";
  private static final String URL_SUFFIX = ":8080/hw3_war/skiers/1/seasons/2019/days/12/skiers/";

  public static void main(String[] args) {
    RequestCounter counter = new RequestCounter();
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(30);
    connectionManager.setDefaultMaxPerRoute(30);
    CloseableHttpClient httpClient = HttpClientBuilder.create()
        .setConnectionManager(connectionManager).build();

    try {
      int numThreads = Integer.parseInt(args[THREAD_INDEX]);
      int numSkiers = Integer.parseInt(args[SKIERS_INDEX]);
      int numLifts = Integer.parseInt(args[LIFTS_INDEX]);
      int numRuns = Integer.parseInt(args[RUNS_INDEX]);
      String URL = URL_PREFIX + args[IP_INDEX] + URL_SUFFIX;

      int numThreadsLaunchedPhase1 = numThreads / LAUNCHED_THREADS_CONVERT_PHASE1;
      int numThreadsLaunchedPhase3 = numThreads / LAUNCHED_THREADS_CONVERT_PHASE3;
      int totalThreadsLaunchedAllPhase =
          numThreadsLaunchedPhase1 + numThreads + numThreadsLaunchedPhase3;

      int skierIDPerThreadPhase1 = numSkiers / numThreadsLaunchedPhase1;
      int skierIDPerThreadPhase2 = numSkiers / numThreads;
      int skierIDPerThreadPhase3 = numSkiers / numThreadsLaunchedPhase3;

      final int numPostsPerThreadPhase1 = (int) Math.ceil(
          numRuns * RUNS_CONVERT_PHASE1 * skierIDPerThreadPhase1);
      final int numPostsPerThreadPhase2 = (int) Math.ceil(
          numRuns * RUNS_CONVERT_PHASE2 * skierIDPerThreadPhase2);
      final int numPostsPerThreadPhase3 = (int) Math.ceil(
          numRuns * RUNS_CONVERT_PHASE3 * skierIDPerThreadPhase3);

      final int phase1TwentyPercentMark = (int) Math.ceil(
          numThreadsLaunchedPhase1 * TWENTY_PERCENT_MARK);
      final int phase2TwentyPercentMark = (int) Math.ceil(numThreads * TWENTY_PERCENT_MARK);

      CountDownLatch phase1Latch = new CountDownLatch(phase1TwentyPercentMark);
      CountDownLatch phase2Latch = new CountDownLatch(phase2TwentyPercentMark);
      CountDownLatch totalLatch = new CountDownLatch(totalThreadsLaunchedAllPhase);

      long startTime = System.currentTimeMillis();
      int totalRequests =
          numThreadsLaunchedPhase1 * numPostsPerThreadPhase1 + numThreads * numPostsPerThreadPhase2
              + numThreadsLaunchedPhase3 * numPostsPerThreadPhase3;

      launchPhase(numThreadsLaunchedPhase1, skierIDPerThreadPhase1, numPostsPerThreadPhase1,
          numLifts, PHASE1_START, PHASE1_END, counter, phase1Latch, totalLatch, URL, httpClient);
      phase1Latch.await();

      launchPhase(numThreads, skierIDPerThreadPhase2, numPostsPerThreadPhase2, numLifts,
          PHASE2_START, PHASE2_END, counter, phase2Latch, totalLatch, URL, httpClient);
      phase2Latch.await();

      launchPhase(numThreadsLaunchedPhase3, skierIDPerThreadPhase3, numPostsPerThreadPhase3,
          numLifts, PHASE3_START, PHASE3_END, counter, null, totalLatch, URL, httpClient);
      totalLatch.await();

      httpClient.close();
      connectionManager.close();
      long wallTime = System.currentTimeMillis() - startTime;
      printResult(counter, wallTime, totalRequests);
    } catch (NumberFormatException e) {
      System.out.println("Unable to process the command line args!");
    } catch (InterruptedException e) {
      System.out.println("The countdown latch is not working properly!");
    } catch (IOException e) {
      System.out.println("The httpclient is not closed properly!");
    }
  }

  private static void launchPhase(int numThreadsLaunched, int skierIDPerThreadPhase,
      int numPostsPerThreadPhase, int numLifts, int phaseStart, int phaseEnd,
      RequestCounter counter, CountDownLatch phaseLatch, CountDownLatch totalLatch, String URL,
      CloseableHttpClient httpClient) throws InterruptedException {
    for (int i = 0; i < numThreadsLaunched; i++) {
      SkierClientThread phaseThread = new SkierClientThread(
          i * skierIDPerThreadPhase + 1,
          (i + 1) * skierIDPerThreadPhase, phaseStart, phaseEnd, numPostsPerThreadPhase,
          numLifts, counter, phaseLatch, totalLatch, URL, httpClient);
      new Thread(phaseThread).start();
    }
  }

  private static void printResult(RequestCounter counter, long wallTime, int totalRequests) {
    System.out.println("The number of successful requests are: " + counter.getSuccessCount());
    System.out.println("The number of failed requests are: " + counter.getFailCount());
    System.out.println(
        "The wall time for all phases to complete are: " + wallTime + " milliseconds");
    System.out.println("The overall throughput in requests per second is: " + (double) totalRequests
        / (wallTime / MILLISECONDS_TO_SECONDS_CONVERT));
  }
}
