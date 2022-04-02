package Database.LiftRideDAO;

import Database.LiftRideModel.LiftRide;
import com.google.gson.Gson;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class LiftRideDAORedis {

  private static final int VERTICAL_MULTIPLIER = 10;
  private static JedisPool pool;
  private final Gson gson = new Gson();

  public LiftRideDAORedis() {
    pool = new JedisPool(buildPoolConfig(), "54.201.178.79", 6379);
  }

  private JedisPoolConfig buildPoolConfig() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(64);
    poolConfig.setMaxIdle(64);
    poolConfig.setMinIdle(16);
//    poolConfig.setTestOnBorrow(true);
//    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setMaxWait(Duration.ofSeconds(6));
    return poolConfig;
  }

  public void createLiftRide(LiftRide liftRide) {
    int skierID = liftRide.getSkierID();
    String jsonString = gson.toJson(liftRide);
    try (Jedis jedis = pool.getResource()) {
      jedis.select(0);
      jedis.lpush(String.valueOf(skierID), jsonString);
    }
  }

  /**
   * Get the number of days the skier N has skied this season.
   *
   * @param skierID  - skier N's ID.
   * @param seasonID - season ID to look up.
   * @return the number of days.
   */
  public int getNumOfDaysForSkierForSeason(Integer skierID, Integer seasonID) {
    Set<Integer> days = new HashSet<>();
    try (Jedis jedis = pool.getResource()) {
      jedis.select(0);
      List<String> data = jedis.lrange(String.valueOf(skierID), 0, -1);
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        if (liftRide.getSkierID().equals(skierID) && liftRide.getSeasonID().equals(seasonID)) {
          days.add(liftRide.getDayID());
        }
      }
    }
    return days.size();
  }

  /**
   * Get the vertical totals for skier N on ski day X.
   *
   * @param skierID  - skier N's ID.
   * @param seasonID - season ID to look up.
   * @param dayID    - day X's ID.
   * @return the total vertical for skier N on day X.
   */
  public int getVerticalForSkiDay(Integer skierID, Integer seasonID, Integer dayID) {
    int vertical = 0;
    try (Jedis jedis = pool.getResource()) {
      jedis.select(0);
      List<String> data = jedis.lrange(String.valueOf(skierID), 0, -1);
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        if (liftRide.getSkierID().equals(skierID) && liftRide.getSeasonID().equals(seasonID)
            && liftRide.getDayID().equals(dayID)) {
          vertical += liftRide.getLiftID() * VERTICAL_MULTIPLIER;
        }
      }
    }
    return vertical;
  }


  /**
   * Get the unique lift IDs skier N rode on each ski day for this season.
   *
   * @param skierID  - skier N's id.
   * @param seasonID - season ID to look up.
   * @return the unique lift IDs skier N rode on each ski day.
   */
  public Map<Integer, List<Integer>> getLiftsForSkierForSkiDay(Integer skierID, Integer seasonID) {
    Map<Integer, List<Integer>> result = new HashMap<>();
    try (Jedis jedis = pool.getResource()) {
      jedis.select(0);
      List<String> data = jedis.lrange(String.valueOf(skierID), 0, -1);
      Map<Integer, Set<Integer>> lifts = new HashMap<>();
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        int dayID = liftRide.getDayID();
        if (!lifts.containsKey(dayID)) {
          lifts.put(dayID, new HashSet<>());
        }
        if (!result.containsKey(dayID)) {
          result.put(dayID, new ArrayList<>());
        }
        if (Objects.equals(liftRide.getSkierID(), skierID) && Objects.equals(liftRide.getSeasonID(),
            seasonID)) {
          int liftID = liftRide.getLiftID();
          if (!lifts.get(dayID).contains(liftID)) {
            result.get(dayID).add(liftID);
            lifts.get(dayID).add(liftID);
          }
        }
      }
    }
    return result;
  }
}
