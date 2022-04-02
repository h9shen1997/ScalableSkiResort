package Servlet.ResortServlet;

import Database.ResortModel.Resort;
import Database.ResortModel.ResortPostJSONBody;
import Servlet.ChannelImplementation.ChannelFactory;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class ResortDataServlet extends HttpServlet {

  private static final int RESORTS_LONG_URL_LENGTH = 7;
  private static final int RESORTS_MED_URL_LENGTH = 3;
  private static final int RESORTS_MED_URL_RESORT_ID_INDEX = 1;
  private static final int RESORTS_MED_URL_SEASONS_INDEX = 2;
  private static final int RESORTS_LONG_URL_RESORT_ID_INDEX = 1;
  private static final int RESORTS_LONG_URL_SEASONS_INDEX = 2;
  private static final int RESORTS_LONG_URL_SEASON_ID_INDEX = 3;
  private static final int RESORTS_LONG_URL_DAYS_INDEX = 4;
  private static final int RESORTS_LONG_URL_DAY_ID_INDEX = 5;
  private static final int RESORTS_LONG_URL_SKIERS_INDEX = 6;
  private static final String RESORT_QUEUE_NAME = "resort_queue";
  //  private static final String RABBITMQ_USERNAME = System.getProperty("RABBITMQ_USERNAME");
//  private static final String RABBITMQ_PASSWORD = System.getProperty("RABBITMQ_PASSWORD");
//  private static final String RABBITMQ_HOST = System.getProperty("RABBITMQ_HOST");
  private static final Gson gson = new Gson();
  private ObjectPool<Channel> channelPool;

  @Override
  public void init() throws ServletException {
    super.init();
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
    String urlPath = request.getPathInfo();

    if (urlPath == null) {
      // get a list of ski resorts in the database
      response.getWriter().write("resort doGet works");
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isResortsGetUrlValid(urlParts)) {
      response.getWriter().write("resort data get url is incorrect");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (urlParts.length == RESORTS_MED_URL_LENGTH) {
      // get a list of seasons for the specified resort
      int resortID = Integer.parseInt(urlParts[RESORTS_MED_URL_RESORT_ID_INDEX]);
    } else {
      // get a number of unique skiers at resort/season/day
      int resortID = Integer.parseInt(urlParts[RESORTS_LONG_URL_RESORT_ID_INDEX]);
      int seasonID = Integer.parseInt(urlParts[RESORTS_LONG_URL_SEASON_ID_INDEX]);
      int dayID = Integer.parseInt(urlParts[RESORTS_LONG_URL_DAY_ID_INDEX]);
    }
    response.getWriter().write("resort doGet works");
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
    if (!isResortsPostUrlValid(urlParts)) {
      response.getWriter().write("resort data post url is incorrect");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String jsonBodyString = request.getReader().lines().collect(Collectors.joining());
    int resortID = Integer.parseInt(urlParts[RESORTS_MED_URL_RESORT_ID_INDEX]);
    try {
      ResortPostJSONBody jsonBody = gson.fromJson(jsonBodyString, ResortPostJSONBody.class);
      Resort resort = new Resort(resortID, jsonBody.getSeasonID(), jsonBody.getVertical());
      String resortInfo = gson.toJson(resort);
      sendPayloadToQueue(resortInfo);
    } catch (JsonParseException e) {
      e.printStackTrace();
    }
    response.getWriter().write("resort doPost works");
    response.setStatus(HttpServletResponse.SC_CREATED);
  }

  private void sendPayloadToQueue(String payload) {
    Channel channel = null;
    try {
      channel = channelPool.borrowObject();
      channel.queueDeclare(ResortDataServlet.RESORT_QUEUE_NAME, true, false, false, null);
      channel.basicPublish("", ResortDataServlet.RESORT_QUEUE_NAME,
          MessageProperties.PERSISTENT_TEXT_PLAIN,
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

  private boolean isResortsGetUrlValid(String[] urlParts) {
    return isResortsMedUrlValid(urlParts) || isResortsLongUrlValid(urlParts);
  }

  private boolean isResortsPostUrlValid(String[] urlParts) {
    return isResortsMedUrlValid(urlParts);
  }

  private boolean isResortsMedUrlValid(String[] urlParts) {
    if (urlParts.length != RESORTS_MED_URL_LENGTH) {
      return false;
    }
    try {
      Integer.parseInt(urlParts[RESORTS_MED_URL_RESORT_ID_INDEX]);
    } catch (NumberFormatException e) {
      return false;
    }
    return urlParts[RESORTS_MED_URL_SEASONS_INDEX].equals("seasons");
  }

  private boolean isResortsLongUrlValid(String[] urlParts) {
    if (urlParts.length != RESORTS_LONG_URL_LENGTH) {
      return false;
    }
    try {
      Integer.parseInt(urlParts[RESORTS_LONG_URL_RESORT_ID_INDEX]);
      Integer.parseInt(urlParts[RESORTS_LONG_URL_SEASON_ID_INDEX]);
      Integer.parseInt(urlParts[RESORTS_LONG_URL_DAY_ID_INDEX]);
    } catch (NumberFormatException e) {
      return false;
    }
    if (!urlParts[RESORTS_LONG_URL_SEASONS_INDEX].equals("seasons")) {
      return false;
    }
    if (!urlParts[RESORTS_LONG_URL_DAYS_INDEX].equals("days")) {
      return false;
    }
    return urlParts[RESORTS_LONG_URL_SKIERS_INDEX].equals("skiers");
  }
}
