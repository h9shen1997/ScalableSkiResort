package Database.LiftRideModel;

public class LiftRide {

  private final Integer resortID;
  private final Integer seasonID;
  private final Integer dayID;
  private final Integer skierID;
  private final Integer time;
  private final Integer liftID;
  private final Integer waitTime;

  public LiftRide(Integer resortID, Integer seasonID, Integer dayID, Integer skierID, Integer time,
      Integer liftID, Integer waitTime) {
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.skierID = skierID;
    this.time = time;
    this.liftID = liftID;
    this.waitTime = waitTime;
  }

  public Integer getResortID() {
    return resortID;
  }

  public Integer getSeasonID() {
    return seasonID;
  }

  public Integer getDayID() {
    return dayID;
  }

  public Integer getSkierID() {
    return skierID;
  }

  public Integer getTime() {
    return time;
  }

  public Integer getLiftID() {
    return liftID;
  }

  public Integer getWaitTime() {
    return waitTime;
  }
}
