package wang.ismy.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 1.初始化线程池（初始化空闲线程）
 * 2.getConnection()
 * 3.releaseConnection()
 * 从active转移到free中
 *
 * @author MY
 * @date 2020/1/15 10:42
 */
public class MyDataSource implements DataSource {

    private List<Connection> freeConnection = new CopyOnWriteArrayList<>();
    private List<Connection> activeConnection = new CopyOnWriteArrayList<>();
    private DbBean dbBean = new DbBean();
    private AtomicInteger connCount = new AtomicInteger(0);

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn;
        if (connCount.intValue() < dbBean.getMaxActiveConnections()) {
            // 当前连接数小于最大活动连接数，
            // 如果空闲连接有连接，直接取，如果没有，创建

            if (freeConnection.size() > 0) {
                conn = freeConnection.remove(0);
            } else {
                conn = newConnection();
            }
            if (conn != null && !conn.isClosed()) {
                activeConnection.add(conn);
            } else {
                connCount.decrementAndGet();
                conn = getConnection();
            }
        } else {
            // 当前连接数大于最大活动连接数，进行等待
            try {
                wait(dbBean.getConnTimeOut());
                conn = getConnection();
            } catch (InterruptedException e) {
                e.printStackTrace();
                conn = null;
            }

        }
        return conn;
    }


    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    public void releaseConnection(Connection connection) throws SQLException {
        // 判断连接是否可用
        if (connection == null || connection.isClosed()) {
            return;
        }
        // 判断空闲是否大于活动
        if (freeConnection.size() < dbBean.getMaxConnections()) {
            // 空闲池没满，添加到空闲池
            freeConnection.add(connection);
        }else {
            // 空闲池满了
            connection.close();
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private void init() throws SQLException, ClassNotFoundException {
        Class.forName(dbBean.getDriverName());
        // 获取初始连接数
        // 创建connection存放到free
        for (int i = 0; i < dbBean.getInitConnections(); i++) {
            Connection conn = newConnection();
            if (conn != null) {
                freeConnection.add(conn);
            }
        }
    }

    private Connection newConnection() {
        Connection conn;
        try {
            conn = DriverManager.getConnection(dbBean.getUrl(), dbBean.getUserName(), dbBean.getPassword());
            connCount.incrementAndGet();
            return conn;
        } catch (SQLException e) {
            return null;
        }
    }
}
