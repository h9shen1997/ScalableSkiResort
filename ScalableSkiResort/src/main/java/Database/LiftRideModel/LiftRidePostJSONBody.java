package Database.LiftRideModel;

public class LiftRidePostJSONBody {

  private final Integer skierID;
  private final Integer time;
  private final Integer liftID;
  private final Integer waitTime;

  public LiftRidePostJSONBody(Integer skierID, Integer time, Integer liftID, Integer waitTime) {
    this.skierID = skierID;
    this.time = time;
    this.liftID = liftID;
    this.waitTime = waitTime;
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
