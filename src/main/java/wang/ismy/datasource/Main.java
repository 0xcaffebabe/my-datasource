package wang.ismy.datasource;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author MY
 * @date 2020/1/15 11:59
 */
public class Main {
    public static void main(String[] args) throws SQLException {
        MyDataSource dataSource = new MyDataSource();
        for (int i = 0; i < 3; i++) {
            new Thread(()->{
                for (int i1 = 0; i1 < 10; i1++) {
                    Connection connection = null;
                    try {
                        connection = dataSource.getConnection();
                        System.out.println(connection);
                        dataSource.releaseConnection(connection);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            }).start();
        }


    }
}
