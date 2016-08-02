package local;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Clase que implementa mecanismos y utilidades de conexion a BD.
 * En general, los metodos pueden devolver un codigo RET_xxx
 *
 * Notas de uso:
 *  - Cada objeto representa una conexion a un RDBMS con usuario/pass
 *  - Un objeto Dbcon puede ser persistente en un request
 *    o en una sesion. Sin embargo una transaccion no deberia
 *    prolongarse entre requests.
 *  
 * @author sgs 15.05.11
 */
 
public class Dbcon {

    public final static int RET_OK = 0; // OK
    public final static int RET_DUP = -1; // Clave/Indice duplicado
    public final static int RET_NCO = -2; // No conectado
    public final static int RET_REF = -3; // Integridad referencial
    public final static int RET_SQL = -10; // Error SQL generico
    public final static int RET_ERR = -12; // Excepción generica
    public final static int RET_UNK = -14; // Error logico
    public final static int RET_NOF = -100; // No encontrada ninguna fila

    public final static int SQLNOTFOUND = 100;

    public final static int DUPLICATE_KEY   = 1062;
    public final static int DUPLICATE_KEY2  = -803; // db2
    public final static int INTEGRITY_REF   = -530; // db2
    public final static int INTEGRITY_REF2  = -532; // db2
    public final static int TIMEOUT_DEAD_RB = -911; // db2
    public final static int TIMEOUT_DEAD    = -913; // db2
    public final static int DBINUSE         = -1035; // db2

    public final static String SQLSTAT_RB = "40001"; // db2

    private static String driver_db2 = "COM.ibm.db2.jdbc.app.DB2Driver";
    private static String jdbc_db2 = "jdbc:db2:";

    private static String driver_db2u = "com.ibm.db2.jcc.DB2Driver";
    private static String jdbc_db2u = "jdbc:db2://";

    private static String driver_ora = "oracle.jdbc.OracleDriver";
    private static String jdbc_ora = "jdbc:oracle:thin:";

    private static String driver_mysql = "com.mysql.jdbc.Driver";
    private static String jdbc_mysql = "jdbc:mysql://";

    private static String driver_mf = "mf.jdbc.DFDriver";
    private static String jdbc_mf = "jdbc:mf:";

    private static String driver_odbc = "sun.jdbc.odbc.JdbcOdbcDriver";
    private static String jdbc_odbc = "jdbc:odbc:";

    private static String rdbmsDef = "odbc";

    private String rdbms = rdbmsDef;
    private String driver, jdbc;
    private Connection conn = null;

    private static boolean errorTh = true; // generar ErrorDB

    private static boolean ckconn = true;  // validar conexion
    private String  ckquery = null;        // query de validacion


    public Dbcon() {}

    //
    // Constructor
    // Obtiene una conexion con DriverManager
    //
    public Dbcon(
        String rdbms,
        String host,
        String db,
        String user,
        String passw)
        throws ErrorDB {
        String url = null;

        try {
            this.rdbms = (rdbms != null) ? rdbms : rdbmsDef;

            if (this.rdbms.equalsIgnoreCase("db2")) {
                driver = driver_db2;
                jdbc = jdbc_db2;
                this.rdbms = "db2";
                host = null;
                ckquery="select 1 from sysibm.sysdummy1";
            }
            else if (this.rdbms.equalsIgnoreCase("db2u")) {
                driver = driver_db2u;
                jdbc = jdbc_db2u;
                this.rdbms = "db2";
                ckquery="select 1 from sysibm.sysdummy1";
            }
            else if (this.rdbms.equalsIgnoreCase("oracle")) {
                driver = driver_ora;
                jdbc = jdbc_ora;
                this.rdbms = "oracle";
                String port = "1521";
                int i = host.lastIndexOf(':');
                if (i > 0) {
                    port = host.substring(i + 1);
                    host = host.substring(0, i);
                }
                // se usa formato tnsnames.ora
                url =
                    jdbc
                        + "@(description=(address_list=(address="
                        + "(protocol=tcp)(host="
                        + host
                        + ")(port="
                        + port
                        + ")))"
                        + "(connect_data=(sid="
                        + db
                        + ")))";
                ckquery="select 1 from dual";
            }
            else if (this.rdbms.equalsIgnoreCase("mysql")) {
                driver = driver_mysql;
                jdbc = jdbc_mysql;
                this.rdbms = "mysql";
            }
            else if (this.rdbms.equalsIgnoreCase("mf")) {
                driver = driver_mf;
                jdbc = jdbc_mf;
                this.rdbms = "mf";
                url = jdbc + host + "@" + db;
            }
            else {
                driver = driver_odbc;
                jdbc = jdbc_odbc;
                this.rdbms = "odbc";
            }

            if (url == null)
                url = (host != null) ? jdbc + host + "/" + db : jdbc + db;

            // Cargar driver
            ApW.trace("driver=" + driver,2);
            Class.forName(driver);

            ApW.trace("Conectando " + url + " rdbms="+rdbms + " user=" + user + " ...", 2);
            if (user != null)
                conn = DriverManager.getConnection(url, user, passw);
            else
                conn = DriverManager.getConnection(url);
            if (rdbms != "odbc")
                conn.setAutoCommit(false);
        }
        catch (Exception ex) {
            ApW.error(ex);
            close();
            int ret = RET_NCO;
            throw new ErrorDB(litret(ret), ret, 0, "");
        }
    }

    public Dbcon(String host, String db, String user, String passw)
        throws ErrorDB {
        this(null, host, db, user, passw);
    }

    //
    // Constructor
    // Obtiene una conexion de un pool con DataSource
    //
    public Dbcon(String db) throws ErrorDB {
        int sqlcode = 0;
        try {
            ApW.trace("DataSource " + db);
            Context ctx = new InitialContext();
            //if (ctx == null) 
            //  throw new RuntimeException("No Context");
            String initctx = ctx.getNameInNamespace();
            String dbs = db;
            if (initctx.indexOf("java:") == 0)  
            if (db.indexOf("java:") < 0)
                dbs = "java:comp/env/" + db;
            ApW.trace("initial context=" + initctx +" dbs="+dbs);
            DataSource ds = (DataSource) ctx.lookup(dbs);
            int i = 0; 
            do { 
                if (++i > 50)
                    throw new RuntimeException("Imposible adquirir conexión Base de Datos.");
                conn = ds.getConnection();
                if (conn==null)
                  continue;
                DatabaseMetaData md = conn.getMetaData();
                String str = md.getDriverName().toLowerCase();
                if (str.indexOf("mysql") >= 0)
                    rdbms = "mysql";
                else if (str.indexOf("db2") >= 0) {
                    rdbms = "db2";
                    ckquery="select 1 from sysibm.sysdummy1";
                }
                else if (str.indexOf("oracle") >= 0) {
                    rdbms = "oracle";
                    ckquery="select 1 from dual";
                }                
                ApW.trace("i="+i+" "+str+" rdbms="+rdbms);                
                testconn();
            }
            while (conn == null || conn.isClosed());
            conn.setAutoCommit(false);
        }
        catch (SQLException ex) {
            sqlcode = ex.getErrorCode();
            ApW.error(ex);
            close();
            int ret = RET_NCO;
            throw new ErrorDB(litret(ret,sqlcode), ret, 0, "");
        }
        catch (Exception ex) {
            ApW.error(ex);
            close();
            int ret = RET_NCO;
            throw new ErrorDB(litret(ret,sqlcode), ret, 0, "");
        }
    }

    public static void setRdbmsDef(String rdbms) {
        rdbmsDef = rdbms;
    }

    public String getRdbms() {
        return rdbms;
    }

    public String info() {
        String nin = "No hay información disponible";
        String buf;
        try {
            if (conn == null)
                return nin;
            DatabaseMetaData md = conn.getMetaData();
            if (md == null)
                return nin;
            buf =
                   "Database  : " +md.getDatabaseProductName()
                                          +" (" +md.getDatabaseProductVersion()
                                          +")"
                    + "\n"
                    + "Driver    : "
                    + md.getDriverName()
                    + " "
                    + md.getDriverVersion()
                    + "\n"
                    + "Usuario   : "
                    + md.getUserName()
                    + "\n";                    
            if (rdbms != "odbc")
              buf +=""
                + "Isolation : "
                    + isol(conn.getTransactionIsolation())
                    + "\n"                
                    + "AutoCommit: "
                    + (conn.getAutoCommit() ? "Yes" : "No");
            return buf;
        }
        catch (Exception ex) {
            ApW.error("info", ex);
        }
        return "";
    }

    //
    private static String isol(int level) {
        return level == Connection.TRANSACTION_READ_COMMITTED
            ? "CS"
            : level == Connection.TRANSACTION_REPEATABLE_READ
            ? "RS"
            : level == Connection.TRANSACTION_SERIALIZABLE
            ? "RR"
            : level == Connection.TRANSACTION_READ_UNCOMMITTED
            ? "UR"
            : "??";
    }

    //
    public Connection getConn() {
        try {
            if (conn != null)
                if (conn.isClosed())
                    conn = null;
                testconn();
        }
        catch (Exception e) {
            close();
        }
        return conn;
    }

    //
    private void testconn() {
        try {
            if (conn==null)
                return;
            if (!ckconn)
              return;
            if (ckquery==null)
                return;
            if (select(ckquery,null) <= 0)
                close();
        }
        catch (Throwable e) {
            close();
        }
    }

    //
    public void close() {
        try {
            if (conn != null) {
                ApW.trace("Connection closed conn="+conn);
                conn.close();
                conn = null;
            }
        }
        catch (Exception e) {
            conn = null;
            ApW.error(e);
        }
    }

    //
    public String toString() {
        return "Dbcon_conn=" + conn;
    }

    //
    public int commit() {
        int ret = RET_OK;
        try {
            if (conn == null) {
                ret = RET_NCO;
            }
            else if (!conn.getAutoCommit()) {
                conn.commit();
            }
        }
        catch (SQLException ex) {
            ret = RET_SQL;
        }
        catch (Exception ex) {
            ret = RET_ERR;
        }
        ApW.trace("commit ret="+ret);
        return ret;
    }

    //
    public int rollback() {
        int ret = RET_OK;
        try {
            if (conn == null) {
                ret = RET_NCO;
            }
            else {
                conn.rollback();
            }
        }
        catch (SQLException ex) {
            ret = RET_SQL;
        }
        catch (Exception ex) {
            ret = RET_ERR;
        }
        ApW.trace("rollback ret="+ret);        
        return ret;
    }

    //
    public int insert (String sql) throws ErrorDB {
        int ret = RET_OK;
        int sqlcode = 0;
        String sqlstate = null;
        try {
            Statement stm = conn.createStatement();
            ApW.trace(sql, 2);
            stm.execute(sql);
            stm.close();
        }
        catch (SQLException ex) {
            sqlcode = ex.getErrorCode();
            sqlstate = ex.getSQLState();
            if (sqlcode == DUPLICATE_KEY || sqlcode == DUPLICATE_KEY2) {
                ret = RET_DUP;
            }
            else if (sqlcode == INTEGRITY_REF || sqlcode == INTEGRITY_REF2) {
                ret = RET_REF;
            }
            else {
                ret = RET_SQL;
                ApW.error(sql, ex);
            }
        }
        catch (Exception ex) {
            ret = RET_ERR;
            ApW.error("insert", ex);
        }
        ApW.trace(litret(ret), 2);
        if (ret != RET_OK)
            if (errorTh)
                throw new ErrorDB(litret(ret), ret, sqlcode, sqlstate);
        return ret;
    }

    //
    public int update (String sql) throws ErrorDB {
        int ret = RET_OK;
        int sqlcode = 0;
        String sqlstate = null;
        int retry = 5;
        int nrows = 0;
        do {
            ret = RET_OK;
            try {
                Statement stm = conn.createStatement();
                ApW.trace(sql, 2);
                nrows = stm.executeUpdate(sql);
                stm.close();
                retry=0;
            }
            catch (SQLException ex) {
                sqlcode = ex.getErrorCode();
                sqlstate = ex.getSQLState().intern();
                if (rdbms == "db2"
                    && sqlcode == TIMEOUT_DEAD
                    && sqlstate != SQLSTAT_RB) {
                    --retry; // reintentar
                    ApW.trace("Reintentando: " + sql, 1);
                }
                else {
                    retry = 0;
                    if (sqlcode == DUPLICATE_KEY
                        || sqlcode == DUPLICATE_KEY2) {
                        ret = RET_DUP;
                    }
                    else {
                        ret = RET_SQL;
                        ApW.error(sql, ex);
                    }
                }
            }
            catch (Exception e) {
                ret = RET_ERR;
                ApW.error(e);
            }
        }
        while (retry > 0);

        ApW.trace(litret(ret) + " " + nrows, 2);
        if (ret != RET_OK)
            if (errorTh)
                throw new ErrorDB(litret(ret), ret, sqlcode, sqlstate);
        return ret == RET_OK ? nrows : ret;
    }

    //
    public int delete (String sql) throws ErrorDB {
        int ret = RET_OK;
        int sqlcode = 0;
        String sqlstate = null;
        int nrows = 0;
        try {
            Statement stm = conn.createStatement();
            ApW.trace(sql, 2);
            nrows = stm.executeUpdate(sql);
            stm.close();
        }
        catch (SQLException ex) {
            sqlcode = ex.getErrorCode();
            sqlstate = ex.getSQLState();
            if (sqlcode == INTEGRITY_REF || sqlcode == INTEGRITY_REF2) {
                ret = RET_REF;
            }
            else {
                ret = RET_SQL;
                ApW.error(sql, ex);
            }
        }
        catch (Exception ex) {
            ret = RET_ERR;
            ApW.error("delete", ex);
        }
        if (ret == RET_OK) {
            if (nrows == 0)
                ret = RET_NOF;
        }
        ApW.trace(litret(ret) + " rows=" + nrows, 2);
        if (ret != RET_OK && ret != RET_NOF)
            if (errorTh)
                throw new ErrorDB(litret(ret), ret, sqlcode, sqlstate);
        return ret == RET_OK || ret == RET_NOF ? nrows : ret;
    }

    //
    public int select (String sql, Vector<Object> m) throws ErrorDB {
        int ret = RET_OK;
        int sqlcode = 0;
        String sqlstate = "";
        int nrows = 0; 
        Vector<Object> r;
        try {
            ApW.trace("sql=" + sql, 2);
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            ResultSetMetaData rm = rs.getMetaData();
            int cols = rm.getColumnCount();
            while (rs.next()) {
                r = new Vector<Object>();
                for (int i = 1; i <= cols; i++) {
                    r.addElement(rs.getObject(i));
                }
                if (m != null)
                    m.addElement(r);
                ++nrows;
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e) {
            sqlcode = e.getErrorCode();
            sqlstate = e.getSQLState();
            if (sqlcode != SQLNOTFOUND) {
                ret = RET_SQL;
                ApW.error(sql, e);
            }
        }
        catch (Exception e) {
            ret = RET_ERR;
            ApW.fatal(e);
        }
        if (ret == RET_OK) {
            if (nrows == 0)
                ret = RET_NOF;
        }
        ApW.trace(litret(ret) + " sqlcode="+sqlcode + " sqlstate=" + sqlstate +" nrows=" + nrows, 2);
        if (ret != RET_OK && ret != RET_NOF)
            if (errorTh)
                throw new ErrorDB(litret(ret), ret, sqlcode, sqlstate);
        return ret == RET_OK ? nrows : ret;
    }

    //
    // Devuelve una tabla de resultados de una query
    // fmt - formato general
    //     1 Ajustar longitudes
    //     2 No generar filas cabecera. Por defecto se generan:
    //       La fila 0 contiene los nombres de las columnas
    //       La fila 1 contiene subrayados
    // cfmt - formatos especiales columna
    //     2 int -> ip4 address
    //
    public String[][] select (String sql, int fmt, int[] cfmt) throws ErrorDB {
        int ret = RET_OK;
        int sqlcode = 0;
        String sqlstate = "";
        int i,j,k,l,n=0;
        ArrayList<String[]> m = new ArrayList<String[]>();
        String[] r;
        String[][] t = null;
        try {
            ApW.trace("sql=" + sql, 2);
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            ResultSetMetaData rm = rs.getMetaData();
            int cols = rm.getColumnCount();
            int cfmtw[] = new int[cols];
            for (j=0;j<cols;j++) {
                cfmtw[j] = 0;
                if (cfmt != null)
                if (j < cfmtw.length)
                    cfmtw[j] = cfmt[j];              
            }
            if ((fmt&0x02)==0) {
                r = new String[cols];
                for (j=0;j<cols;j++)
                    r[j]=rm.getColumnLabel(j+1);
                m.add(r);
                m.add(new String[cols]);
            }
            while (rs.next()) {
                r = new String[cols];
                for (j=0;j<cols;j++) {
                    if ((cfmtw[j]&0x02) != 0) {
                        k=rs.getInt(j+1);
                        r[j]=Wutil.ipstr(k);
                    }
                    else {
                        r[j]=rs.getString(j+1);
                        if (r[j]==null)
                            r[j]="-";
                    }
                }
                m.add(r);
            }
            rs.close();
            stm.close();
            if (m.size() > 0) {
                t = (String[][])m.toArray(new String[1][]);
                n = t.length;      
                ApW.trace("t: "+t.length+" "+t[0].length,3);
            }
            if ((fmt&0x01) != 0 && n > 1)
            for (j=0;j<cols;j++) {
                l=0;
                for (i=0;i<n;i++) {
                    if (i != 1)
                    if (t[i][j].length() > l)
                        l=t[i][j].length();
                }
                for (i=0;i<n;i++)
                    if (i != 1)
                        t[i][j]=spad(t[i][j],l);
                t[1][j]=spad("",l,'-');
           }
        }
        catch (SQLException e) {
            sqlcode = e.getErrorCode();
            sqlstate = e.getSQLState();
            if (sqlcode != SQLNOTFOUND) {
                ret = RET_SQL;
                ApW.error(sql, e);
            }
        }
        catch (Exception e) {
            ret = RET_ERR;
            ApW.fatal(e);
        }

        if ((fmt&0x02)==0)
            n-=2;
        ApW.trace(litret(ret) + " sqlcode="+sqlcode + " sqlstate=" + sqlstate +" nrows=" + n, 2);
        if (ret != RET_OK && ret != RET_NOF)
            if (errorTh)
                throw new ErrorDB(litret(ret), ret, sqlcode, sqlstate);

        return t;
    }

    public String[][] select (String sql, int fmt) throws ErrorDB {
        return select(sql,fmt,null);
    }

    public String[][] select(String sql) throws ErrorDB {
        return select(sql,1,null);
    }

    //
    private static String spad (String s, int l) {
        String w=s;
        ++l;
        while (w.length() < l)
            w=w.concat("                         ");
        return w.substring(0,l);
    }

    //
    static String spad (String s, int l, char c) {
        String w = spad(s,l);
        return w.replace(' ',c);
    }

    //
    // Obtiene la ultima clave insertada
    //
    public int getLastInsertId(String seq) throws Exception {
        int lastid = 0;
        String sql;
        if (rdbms == "db2")
            sql = "select identity_val_local() from sysibm.sysdummy1";
        else if (rdbms == "mysql")
            sql = "select last_insert_id();";
        else
            sql = "select " + seq + ".currval from dual";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        lastid = rs.getInt(1);
        rs.close();
        stm.close();
        return lastid;
    }

    //
    public int updateBlob(String sql, byte[] b) {
        int ret = RET_OK;
        try {
            int l = (b != null) ? b.length : 0;
            ApW.trace("length=" + l);
            /*****
                         PreparedStatement stm = conn.prepareStatement(sql);
                         stm.setBytes(1,b);
                         if (b == null)
                stm.setBinaryStream(1,null,0);
                         else {
                ByteArrayInputStream bais = new ByteArrayInputStream(b);
                stm.setBinaryStream(1,bais,bais.available());
                         }
                         stm.executeUpdate(sql);
                         stm.close();
             ******/
            Statement stm = conn.createStatement();
            stm.execute(sql);
            stm.close();
        }
        catch (SQLException ex) {
            if (ex.getErrorCode() == DUPLICATE_KEY) {
                ret = RET_DUP;
            }
            else {
                ret = RET_SQL;
                ApW.error(sql, ex);
            }
        }
        catch (Exception ex) {
            ret = RET_ERR;
            ApW.error(ex);
        }
        ApW.trace(litret(ret), 2);
        return ret;
    }

    //
    public int getBlob(String sql, String file) {
        int ret = RET_OK;
        byte[] b = null;
        try {
            ApW.trace("sql=" + sql, 2);
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                b = rs.getBytes(1);
            }
            else {
                ret = RET_NOF;
            }
            if (b != null) {
                int l = (int) b.length;
                ApW.trace("length=" + l + " file=" + file);
                if (file != null) {
                    File bfile = new File(file);
                    FileOutputStream fos = new FileOutputStream(bfile);
                    fos.write(b, 0, l);
                    fos.close();
                }
            }
            rs.close();
            stm.close();
        }
        catch (SQLException ex) {
            ret = RET_SQL;
            ApW.error(sql, ex);
        }
        catch (Exception ex) {
            ret = RET_ERR;
            ApW.error(ex);
        }
        ApW.trace(litret(ret), 2);
        return ret;
    }

    //
    private String litret(int ret) {
        if (ret > 0)
            return "rows=" + ret;
        switch (ret) {
            case RET_OK :
                return "OK";
            case RET_NOF :
                return "No Encontrado";
            case RET_DUP :
                return "Registro Duplicado";
            case RET_REF :
                return "Restriccion de integridad";
            case RET_SQL :
                return "Error SQL";
            case RET_ERR :
                return "Error";
            case RET_NCO :
                return "No hay Conexion";
            case RET_UNK :
                return "Error Desconocido";
            default :
                return "ERROR ?";
        }
    }

    //
    private String litret(int ret, int sqlcode) {
        if (sqlcode == DBINUSE)
            return "La Base de Datos está temporalmente en uso. Vuelva a intentarlo en unos minutos.";
        return litret(ret);
    }

    // Liberar la conexión (si existe) cuando se destruye el objeto
    public void finalize() throws Throwable {
        ApW.trace("conn=" + conn, 3);
        close();
    }

    /**
     * Devuelve termindor de sentencia según RDBMS
     */
    public String endsql() {
        if (rdbms == "mysql")
            return ";";
        else
            return "";
    }

    /**
     * Devuelve literal sql obtención fecha actual
     */
    public String sqlGetDate() {
        if (rdbms == "mysql")
            return "NOW()";
        else if (rdbms == "oracle")
            return "SYSDATE";
        else if (rdbms == "db2")
            return "CURRENT DATE";
        else
            return "";
    }
    
    /**
     * Devuelve literal sql obtención timestamp actual
     */
    public String sqlGetTimestamp() {
        if (rdbms == "mysql")
            return "NOW()";
        else if (rdbms == "oracle")
            return "SYSDATE";
        else if (rdbms == "db2")
            return "CURRENT TIMESTAMP";
        else
            return "";
    }

    //
    // Prepara un literal con posibles ' para ser usado por SQL
    //
    public static String litsql(String src) {
        if (src == null)
            return "null";
        String s = src.replaceAll("'", "''");
        return "'" + s + "'";
    }

    public static String litsql(Integer src) {
        if (src == null)
            return "null";
        return src.toString();
    }

    public static String litsql(java.sql.Date src) {
        if (src == null)
            return "null";
        return "DATE('" + src.toString() + "')";
    }

    //
    // Convierte a sql.Timestamp
    //
    public static java.sql.Timestamp toTimestamp(Object o) throws SQLException {
        if (o == null)
            return null;
        Class c = o.getClass();
        String cn = c.getName();
        ApW.trace("cn=" + cn, 2);
        if (cn.indexOf("oracle") >= 0)
            return ((oracle.sql.TIMESTAMP) o).timestampValue();
        else
            return ((java.sql.Timestamp) o);
    }

    //
    // Convierte a int
    //
    public static int toInt(Object o) {
        if (o == null)
            return 0;
        Class c = o.getClass();
        String cn = c.getName();
        if (cn.indexOf("BigDecimal") > 0)
            return ((java.math.BigDecimal) o).intValue();
        else
            return ((Integer) o).intValue();
    }

    //
    // Convierte a long
    //
    public static long toLong(Object o) {
        if (o == null)
            return 0;
        Class c = o.getClass();
        String cn = c.getName();
        if (cn.indexOf("BigDecimal") > 0)
            return ((java.math.BigDecimal) o).longValue();
        else
            return ((Long) o).longValue();
    }
}
