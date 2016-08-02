package local;

/*
 * Clase para errores DB
 */
public class ErrorDB extends RuntimeException {

    private int errorCode = 0;
    private int sqlcode = 0;
    private String sqlstate = "";
    private static final long serialVersionUID = 0;

    public ErrorDB() {
    }

    public ErrorDB(String msg, int code, int sqlcode, String sqlstate) {
        super(msg);
        errorCode = code;
        this.sqlcode = sqlcode;
        if (sqlstate != null)
            this.sqlstate = sqlstate;
    }

    public ErrorDB(String msg, int code, int sqlcode) {
        super(msg);
        errorCode = code;
        this.sqlcode = sqlcode;
    }

    public ErrorDB(String msg, int code) {
        super(msg);
        errorCode = code;
    }

    public ErrorDB(String msg) {
        super(msg);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getSqlCode() {
        return sqlcode;
    }

    public String getSQLState() {
        return sqlstate;
    }

    public String getMessageC () {
        String msg = super.getMessage();
        if (sqlcode != 0)
            msg = msg.concat(" ["+sqlcode +" " + sqlstate +"]");
        return msg;
    }

}

