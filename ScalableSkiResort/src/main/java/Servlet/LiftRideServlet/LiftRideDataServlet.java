package Servlet.LiftRideServlet;

import Database.LiftRideModel.LiftRide;
import Database.LiftRideModel.LiftRidePostJSONBody;
import Servlet.ChannelImplementation.ChannelFactory;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class LiftRideDataServlet extends HttpServlet {

  private static final int SKIERS_LONG_URL_LENGTH = 8;
  private static final int SKIERS_SHORT_URL_LENGTH = 3;
  private static final int SKIERS_SHORT_URL_SKIER_ID_INDEX = 1;
  private static final int SKIERS_SHORT_URL_VERTICAL_INDEX = 2;
  private static final int SKIERS_LONG_URL_RESORT_ID_INDEX = 1;
  private static final int SKIERS_LONG_URL_SEASONS_INDEX = 2;
  private static final int SKIERS_LONG_URL_SEASON_ID_INDEX = 3;
  private static final int SKIERS_LONG_URL_DAYS_INDEX = 4;
  private static final int SKIERS_LONG_URL_DAY_ID_INDEX = 5;
  private static final int SKIERS_LONG_URL_SKIERS_INDEX = 6;
  private static final int SKIERS_LONG_URL_SKIER_ID_INDEX = 7;
  private static final int BACKOFF_UPPER = 800;
  private static final int BACKOFF_LOWER = 600;
  private static final int DAY_MIN = 1;
  private static final int DAY_MAX = 366;

  private static final String SKIER_QUEUE_NAME = "ski_queue";
  private static final String RABBITMQ_USERNAME = System.getProperty("RABBITMQ_USERNAME");
  private static final String RABBITMQ_PASSWORD = System.getProperty("RABBITMQ_PASSWORD");
  private static final String RABBITMQ_HOST = System.getProperty("RABBITMQ_HOST");
  private static final Gson gson = new Gson();
  private ObjectPool<Channel> channelPool;
  private EventCountCircuitBreaker circuitBreaker;

  @Override
  public void init() throws ServletException {
    super.init();
    circuitBreaker = new EventCountCircuitBreaker(BACKOFF_UPPER, 1, TimeUnit.SECONDS, BACKOFF_LOWER);
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.4.81");
    factory.setUsername("admin");
    factory.setPassword("rabbitmq123");
    try {
      Connection connection = factory.newConnection();
      channelPool = new GenericObjectPool<>(new ChannelFactory(connection));
    } catch (TimeoutException | IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/plain");
    if(!circuitBreaker.incrementAndCheckState()) {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      response.getWriter().write("Too many request sent per second");
      return;
    }

    String urlPath = request.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("ski data request missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isSkiersGetUrlValid(urlParts)) {
      response.getWriter().write("lift ride data get url is incorrect");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (urlParts.length == SKIERS_SHORT_URL_LENGTH) {
      // get the total vertical for the skier for specified seasons at the specified resort
      int skierID = Integer.parseInt(urlParts[SKIERS_SHORT_URL_SKIER_ID_INDEX]);
    } else {
      // get ski day vertical for a skier
      int resortID = Integer.parseInt(urlParts[SKIERS_LONG_URL_RESORT_ID_INDEX]);
      int seasonID = Integer.parseInt(urlParts[SKIERS_LONG_URL_SEASON_ID_INDEX]);
      int dayID = Integer.parseInt(urlParts[SKIERS_LONG_URL_DAY_ID_INDEX]);
      int skierID = Integer.parseInt(urlParts[SKIERS_LONG_URL_SKIER_ID_INDEX]);
    }
    response.getWriter().write("lift ride doGet works");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/plain");
    String urlPath = request.getPathInfo();
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isSkiersPostUrlValid(urlParts)) {
      response.getWriter().write("lift ride data post url is incorrect");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String jsonBodyString = request.getReader().lines().collect(Collectors.joining());
    int resortID = Integer.parseInt(urlParts[SKIERS_LONG_URL_RESORT_ID_INDEX]);
    int seasonID = Integer.parseInt(urlParts[SKIERS_LONG_URL_SEASON_ID_INDEX]);
    int dayID = Integer.parseInt(urlParts[SKIERS_LONG_URL_DAY_ID_INDEX]);
    try {
      LiftRidePostJSONBody jsonBody = gson.fromJson(jsonBodyString, LiftRidePostJSONBody.class);
      LiftRide liftRide = new LiftRide(resortID, seasonID, dayID, jsonBody.getSkierID(),
          jsonBody.getTime(), jsonBody.getLiftID(), jsonBody.getWaitTime());
      String liftRideInfo = gson.toJson(liftRide);
      sendPayloadToQueue(liftRideInfo);
    } catch (JsonParseException e) {
      e.printStackTrace();
    }
    response.getWriter().write("lift ride doPost works");
    response.setStatus(HttpServletResponse.SC_CREATED);
  }

  private void sendPayloadToQueue(String payload) {
    Channel channel = null;
    try {
      channel = channelPool.borrowObject();
      int messageCount = channel.queueDeclare(SKIER_QUEUE_NAME, true, false, false, null)
          .getMessageCount();
      channel.basicPublish("", SKIER_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,
          payload.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (channel != null) {
          channelPool.returnObject(channel);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private boolean isSkiersGetUrlValid(String[] urlParts) {
    return isSkiersShortUrlValid(urlParts) || isSkiersLongUrlValid(urlParts);
  }

  private boolean isSkiersPostUrlValid(String[] urlParts) {
    return isSkiersLongUrlValid(urlParts);
  }

  private boolean isSkiersShortUrlValid(String[] urlParts) {
    if (urlParts.length != SKIERS_SHORT_URL_LENGTH) {
      return false;
    }
    try {
      Integer.parseInt(urlParts[SKIERS_SHORT_URL_SKIER_ID_INDEX]);
    } catch (NumberFormatException e) {
      return false;
    }
    return urlParts[SKIERS_SHORT_URL_VERTICAL_INDEX].equals("vertical");
  }

  private boolean isSkiersLongUrlValid(String[] urlParts) {
    if (urlParts.length != SKIERS_LONG_URL_LENGTH) {
      return false;
    }
    try {
      Integer.parseInt(urlParts[SKIERS_LONG_URL_RESORT_ID_INDEX]);
      Integer.parseInt(urlParts[SKIERS_LONG_URL_SEASON_ID_INDEX]);
      int days = Integer.parseInt(urlParts[SKIERS_LONG_URL_DAY_ID_INDEX]);
      if (days < DAY_MIN || days > DAY_MAX) {
        return false;
      }
      Integer.parseInt(urlParts[SKIERS_LONG_URL_SKIER_ID_INDEX]);
    } catch (NumberFormatException e) {
      return false;
    }
    if (!urlParts[SKIERS_LONG_URL_SEASONS_INDEX].equals("seasons")) {
      return false;
    }
    if (!urlParts[SKIERS_LONG_URL_DAYS_INDEX].equals("days")) {
      return false;
    }
    return urlParts[SKIERS_LONG_URL_SKIERS_INDEX].equals("skiers");
  }
}
