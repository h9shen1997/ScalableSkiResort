package Database.ResortModel;

public class Resort {

  private final Integer seasonID;
  private final Integer resortID;
  private final Integer vertical;

  public Resort(Integer resortID, Integer year, Integer vertical) {
    this.resortID = resortID;
    this.seasonID = year;
    this.vertical = vertical;
  }

  public Integer getSeasonID() {
    return seasonID;
  }

  public Integer getResortID() {
    return resortID;
  }

  public Integer getVertical() {
    return vertical;
  }
}
