package Client;

import Database.LiftRideModel.LiftRidePostJSONBody;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class SkierClientThread implements Runnable {

  private static final int TRY_THRESHOLD = 5;
  private static final int WAIT_TIME_MAX = 10;
  private final int skierIDMin;
  private final int skierIDMax;
  private final int timeMin;
  private final int timeMax;
  private final int numLifts;
  private final long numPosts;
  private final String URL;
  private final RequestCounter counter;
  private final CountDownLatch phaseLatch;
  private final CountDownLatch totalLatch;
  private final CloseableHttpClient httpClient;
  private static final int BACKOFF_MULTIPLIER = 2;

  public SkierClientThread(int skierIDMin, int skierIDMax, int timeMin, int timeMax,
      long numPosts, int numLifts, RequestCounter counter, CountDownLatch phaseLatch,
      CountDownLatch totalLatch, String URL, CloseableHttpClient httpClient) {
    this.skierIDMax = skierIDMax;
    this.skierIDMin = skierIDMin;
    this.timeMin = timeMin;
    this.timeMax = timeMax;
    this.numPosts = numPosts;
    this.numLifts = numLifts;
    this.counter = counter;
    this.phaseLatch = phaseLatch;
    this.totalLatch = totalLatch;
    this.URL = URL;
    this.httpClient = httpClient;
  }

  public void run() {
    int numSuccess = 0;
    int numFail = 0;
    for (int i = 0; i < this.numPosts; i++) {
      int skierID = ThreadLocalRandom.current().nextInt(skierIDMin, skierIDMax + 1);
      int liftID = ThreadLocalRandom.current().nextInt(1, numLifts + 1);
      int time = ThreadLocalRandom.current().nextInt(timeMin, timeMax + 1);
      int waitTime = ThreadLocalRandom.current().nextInt(0, WAIT_TIME_MAX + 1);

      String postUrl = URL + skierID;
      HttpPost httpPost = new HttpPost(postUrl);

      LiftRidePostJSONBody jsonBody = new LiftRidePostJSONBody(skierID, time, liftID, waitTime);
      String json = new Gson().toJson(jsonBody);

      try {
        httpPost.setEntity(new StringEntity(json));
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      try {
        CloseableHttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpServletResponse.SC_CREATED) {
          numSuccess++;
        } else {
          int attempts = 0;
          long initialBackoff = 1;
          while (isResponseInvalid(response.getStatusLine().getStatusCode())
              && attempts++ < TRY_THRESHOLD) {
            EntityUtils.consume(response.getEntity());
            response.close();
            numFail++;
            TimeUnit.SECONDS.sleep(initialBackoff);
            try {
              response = httpClient.execute(httpPost);
            } catch (IOException e) {
              e.printStackTrace();
            }
            initialBackoff *= BACKOFF_MULTIPLIER;
          }
          if (isResponseInvalid(response.getStatusLine().getStatusCode())) {
            numFail++;
          }
        }
        EntityUtils.consume(response.getEntity());
        response.close();
      } catch (IOException e) {
        numFail++;
        System.out.println("Exception occurred when sending httpPost request!");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    totalLatch.countDown();
    if (phaseLatch != null) {
      phaseLatch.countDown();
    }
    counter.incSuccess(numSuccess);
    counter.incFail(numFail);
  }

  private boolean isResponseInvalid(int statusCode) {
    return statusCode != HttpServletResponse.SC_CREATED;
  }
}
