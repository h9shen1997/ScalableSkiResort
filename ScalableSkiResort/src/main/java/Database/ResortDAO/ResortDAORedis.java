package Database.ResortDAO;

import Database.LiftRideModel.LiftRide;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ResortDAORedis {

  private static final int TOTAL_HOURS = 7;
  private static final int HOUR_TO_MINUTE = 60;
  private static JedisPool pool;
  private final Gson gson = new Gson();

  public ResortDAORedis() {
    pool = new JedisPool(buildPoolConfig(), "172.31.7.21", 6379);
  }

  private JedisPoolConfig buildPoolConfig() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(128);
    poolConfig.setMaxIdle(128);
    poolConfig.setMinIdle(16);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setNumTestsPerEvictionRun(3);
    poolConfig.setBlockWhenExhausted(true);
    return poolConfig;
  }

  public void createResortInfo(LiftRide liftRide) {
    int resortID = liftRide.getResortID();
    String jsonString = gson.toJson(liftRide);
    try (Jedis jedis = pool.getResource()) {
      jedis.select(1);
      jedis.lpush(String.valueOf(resortID), jsonString);
    }
  }

  /**
   * Get the number of unique skiers that visited resort X on day N.
   *
   * @param resortID - resort X's ID.
   * @param dayID    - day N's ID.
   * @return the number of unique skiers.
   */
  public int getUniqueSkiersForDay(Integer resortID, Integer dayID) {
    Set<Integer> skiers = new HashSet<>();
    try (Jedis jedis = pool.getResource()) {
      jedis.select(1);
      List<String> data = jedis.lrange(String.valueOf(resortID), 0, -1);
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        if (liftRide.getResortID().equals(resortID) && liftRide.getDayID().equals(dayID)) {
          skiers.add(liftRide.getSkierID());
        }
      }
    }
    return skiers.size();
  }

  /**
   * Get the number of rides on lift N on day N.
   *
   * @param liftID   - lift N's ID.
   * @param dayID    - day N's ID.
   * @param resortID - resort's ID.
   * @return the number of rides on lift N on day N.
   */
  public int getNumOfRidesOnDay(Integer liftID, Integer dayID, Integer resortID) {
    int liftCounter = 0;
    try (Jedis jedis = pool.getResource()) {
      jedis.select(1);
      List<String> data = jedis.lrange(String.valueOf(resortID), 0, -1);
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        if (liftRide.getLiftID().equals(liftID) && liftRide.getDayID().equals(dayID)) {
          liftCounter++;
        }
      }
    }
    return liftCounter;
  }

  /**
   * Get the number of lift rides for every hour (total of 7 hours) on day N.
   *
   * @param resortID - resort's ID.
   * @param dayID    - day N's ID.
   * @return the number of lift rides for every hour on day N.
   */
  public int[] getNumOfLiftRidesEveryHourOfDay(Integer resortID, Integer dayID) {
    int[] liftsPerHour = new int[TOTAL_HOURS];
    try (Jedis jedis = pool.getResource()) {
      jedis.select(1);
      List<String> data = jedis.lrange(String.valueOf(resortID), 0, -1);
      for (String d : data) {
        LiftRide liftRide = gson.fromJson(d, LiftRide.class);
        if (liftRide.getDayID().equals(dayID)) {
          int timeInMinutes = liftRide.getTime();
          liftsPerHour[timeInMinutes / HOUR_TO_MINUTE]++;
        }
      }
    }
    return liftsPerHour;
  }
}
