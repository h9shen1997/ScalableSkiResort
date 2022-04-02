package Database.ResortModel;

public class ResortPostJSONBody {

  int seasonID;
  int vertical;

  public ResortPostJSONBody(int seasonID, int vertical) {
    this.seasonID = seasonID;
    this.vertical = vertical;
  }

  public int getSeasonID() {
    return seasonID;
  }

  public int getVertical() {
    return vertical;
  }
}
