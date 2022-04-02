package Database.LiftRideDAO;

import org.apache.commons.dbcp2.BasicDataSource;

public class LiftRideDataSource {

  private static final String HOST = "skidata.cdwwe3dpzm1m.us-west-2.rds.amazonaws.com";
  private static final String PORT = "3306";
  private static final String CONFIG = "serverTimezone=UTC";
  private static final String SCHEMA = "SkiData";
  private static final String USERNAME = "admin";
  private static final String PASSWORD = "database";
  private static final int DATASOURCE_INIT_SIZE = 10;
  private static final int DATASOURCE_MAX_TOTAL = 60;
  private static final BasicDataSource dataSource;

  static {
    dataSource = new BasicDataSource();
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    String url = String.format("jdbc:mysql://%s:%s/%s?%s", HOST, PORT, SCHEMA, CONFIG);
    dataSource.setUrl(url);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
    dataSource.setInitialSize(DATASOURCE_INIT_SIZE);
    dataSource.setMaxTotal(DATASOURCE_MAX_TOTAL);
  }

  public static BasicDataSource getDataSource() {
    return dataSource;
  }
}
