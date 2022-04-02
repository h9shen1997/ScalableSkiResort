package Database.LiftRideDAO;

import Database.LiftRideModel.LiftRide;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;

public class LiftRideDAOMySQL {

  private static BasicDataSource dataSource;

  public LiftRideDAOMySQL() {
    dataSource = LiftRideDataSource.getDataSource();
  }

  public static void main(String[] args) {
    LiftRideDAOMySQL liftRideDAO = new LiftRideDAOMySQL();
    //liftRideDAO.getDaysForSeasonForSkier(2, 2019);
    System.out.println();
    liftRideDAO.getLiftsForSkierForEachSkiDay(12);
  }

  public void createLiftRide(LiftRide newLiftRide) {
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO LiftRides (resortID, seasonID, dayID, skierID, liftID, time, waitTime) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try {
      connection = dataSource.getConnection();
      preparedStatement = connection.prepareStatement(insertQueryStatement);
      preparedStatement.setInt(1, newLiftRide.getResortID());
      preparedStatement.setInt(2, newLiftRide.getSeasonID());
      preparedStatement.setInt(3, newLiftRide.getDayID());
      preparedStatement.setInt(4, newLiftRide.getSkierID());
      preparedStatement.setInt(5, newLiftRide.getLiftID());
      preparedStatement.setInt(6, newLiftRide.getTime());
      preparedStatement.setInt(7, newLiftRide.getWaitTime());
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Get the days the skier N has skied for this season.
   *
   * @param skierID  the skier ID
   * @param seasonID the season ID
   */
  public void getDaysForSeasonForSkier(Integer skierID, Integer seasonID) {
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    String selectQueryStatement = "SELECT skierID, COUNT(DISTINCT dayID) day_count FROM LiftRides WHERE ? = skierID AND ? = seasonID GROUP BY skierID";
    try {
      connection = dataSource.getConnection();
      preparedStatement = connection.prepareStatement(selectQueryStatement);
      preparedStatement.setInt(1, skierID);
      preparedStatement.setInt(2, seasonID);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        int dayCount = resultSet.getInt("day_count");
        System.out.printf("Skier %d has skied %d day(s) in season %d.%n", skierID, dayCount,
            seasonID);
      }
      resultSet.close();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public void getLiftsForSkierForEachSkiDay(Integer skierID) {
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    String selectQueryStatement = "SELECT skierID, dayID, liftID FROM LiftRides WHERE ? = skierID GROUP BY skierID, dayID ORDER BY dayID, liftID ASC";
    try {
      connection = dataSource.getConnection();
      preparedStatement = connection.prepareStatement(selectQueryStatement);
      preparedStatement.setInt(1, skierID);
      ResultSet resultSet = preparedStatement.executeQuery();
      System.out.printf("Skier %d lift records for each day is the following: \n", skierID);
      while (resultSet.next()) {
        int dayID = resultSet.getInt("dayID");
        int liftID = resultSet.getInt("liftID");
        System.out.printf("Skied liftID %d on day %d.\n", liftID, dayID);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
