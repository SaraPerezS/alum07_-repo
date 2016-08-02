package local;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.EventObject;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.swing.JOptionPane;

/**
 * Utilidades para mensajes de error, log, y trace. Todos los metodos son static
 * Son posibles 4 salidas: SystemOut SystemErr Swing trace.
 * Por defecto estan activas: out err
 *
 * @author sgs 15.05.06
 */
public class ApW {

    // Activacion de salidas

    private static boolean wout = true;
    private static boolean werr = true;
    private static boolean wgph = false;
    private static boolean wtrc = false;
    private static boolean trlg = false;    // grabar trace como out
    private static boolean trcp = false;    // copiar trace en out

    // Niveles de log y trace
    private static int logLevel = 1;
    private static int trcLevel = 1;

    // El archivo de trace se coloca por defecto en el
    // directorio raiz de la unidad actual
    private static String ftname = "/wtrace.txt";

    private static BufferedWriter ft = null;
    private static SimpleDateFormat formato
            = new SimpleDateFormat("yyMMdd HH:mm:ss:SSS ");

    private static boolean errTrace = false;
    private static boolean closeTrc = false;

    private static String urlerr = "error.jsp";

    private static int fatalExit = 0;

    private static String cname = null;

    /**
     * Activar/Desactivar salida System.out
     */
    public static void setWout(boolean wout) {
        ApW.wout = wout;
    }

    /**
     * Activar/Desactivar salida System.err
     */
    public static void setWerr(boolean werr) {
        ApW.werr = werr;
    }

    /**
     * Activar/Desactivar salida grafica
     */
    public static void setWgph(boolean wgph) {
        ApW.wgph = wgph;
    }

    /**
     * Activar/Desactivar salida trace
     */
    public static void setWtrc(boolean wtrc) {
        ApW.wtrc = wtrc;
    }

    /**
     * Activar/Desactivar salida trace como out
     */
    public static void setTrlg(boolean trlg) {
        ApW.trlg = trlg;
    }

    /**
     * Activar/Desactivar copia out salida trace
     */
    public static void setTrcp(boolean trcp) {
        ApW.trcp = trcp;
    }

    /**
     * Asignar fichero de trace
     */
    public static void setFtname(String name) {
        ftname = name;
    }

    /**
     * Niveles de log y trace
     */
    public static void setLogLevel(int level) {
        logLevel = level;
        if (level > 0) {
            setWout(true);
        }
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setTrcLevel(int level) {
        trcLevel = level;
        if (level > 0) {
            setWtrc(true);
        }
    }

    public static int getTrcLevel() {
        return trcLevel;
    }

    /**
     * Devuelve clase/metodo/linea de invocacion
     */
    public static String strcall() {
        try {
            int i, f;
            StackTraceElement stk[] = Thread.currentThread().getStackTrace();
            for (i = 0, f = 0; i < stk.length; i++) {
                if (cname == null) {
                    if (stk[i].getMethodName().equals("strcall")) {
                        cname = stk[i].getClassName();
                    }
                } else if (stk[i].getClassName().equals(cname)) {
                    ++f;
                } else if (f > 0) {
                    break;
                }
            }
            if (i < stk.length) {
                String txt = stk[i].getClassName() + "." + stk[i].getMethodName() + "." + stk[i].getLineNumber() + ": ";
                return txt;
            }
        } catch (Throwable t) {
        };
        return "";
    }

    /**
     * Devuelve datos de error
     */
    public static String strerr(Throwable t) {
        String buf = "";
        String ename = t.getClass().getName();
        buf += "[" + ename + "] ";
        if (ename.indexOf("jdbc") >= 0
                || ename.indexOf("sql") >= 0
                || ename.indexOf("jcc") >= 0
                || ename.indexOf("StaleConnectionException") >= 0) {
            SQLException e1 = (SQLException) t;
            while (e1 != null) {
                buf = ""
                        + e1.getMessage().trim()
                        + " [sqlcode="
                        + e1.getErrorCode()
                        + " sqlstate="
                        + e1.getSQLState()
                        + "]\n";
                e1 = e1.getNextException();
            }
        } else {
            buf = buf + t.getMessage() + "\n";
        }
        return buf;
    }

    //
    private static void writeOut(String msg) {
        if (!wout) {
            return;
        }
        System.out.println(msg);
    }

    //
    private static void writeErr(String msg) {
        if (!werr) {
            return;
        }
        System.err.println(msg);
    }

    //
    private static void writeGph(String msg, String tit) {
        if (!wgph) {
            return;
        }
        JOptionPane.showMessageDialog(
                null,
                msg,
                tit,
                JOptionPane.ERROR_MESSAGE);
    }

    //
    private static synchronized void writeTrc(String msg) {
        try {
            if (!wtrc) {
                return;
            }
            if (trlg) {
                writeOut(msg);
                return;
            }
            if (errTrace) {
                return;
            }
            if (ft == null) {
                ft = new BufferedWriter(new FileWriter(ftname, true));
            }
            writeLine(ft, formato.format(new java.util.Date()));
            writeLine(ft, msg);
            ft.newLine();
            ft.flush();
            if (closeTrc) {
                ft.close();
                ft = null;
            }
        } catch (java.security.AccessControlException e) {
            errTrace = true;
            String error = "Excepcion de seguridad: " + e.getMessage();
            writeErr(error);
            writeOut(error);
        } catch (Exception e) {
            String error = "Error al grabar trace: " + e.getMessage();
            writeErr(error);
            writeOut(error);
        }
    }

    private static synchronized void writeLine(BufferedWriter f, String s)
            throws Exception {
        f.write(s, 0, s.length());
    }

    /**
     * Errores fatales. Errores internos o que impiden el seguimiento del
     * proceso. Generan salida out err gph trc (si no estan desactivadas)
     */
    public static void fatal(String msg) {
        try {
            String error = strcall() + msg;
            writeErr(error);
            writeOut(error);
            if (!trlg || !wout) {
                writeTrc(error);
            }
            writeGph(error, "Error irrecuperable");
            if (fatalExit > 0) {
                System.exit(fatalExit);
            }
        } catch (Throwable t) {
            if (werr) {
                t.printStackTrace();
            }
        }
    }

    public static void fatal(String msg, Throwable t) {
        try {
            String stre = strerr(t);
            fatal(msg + ": " + stre);
            if (werr) {
                t.printStackTrace();
            }
        } catch (Throwable w) {
        };
    }

    public static void fatal(Throwable t) {
        fatal("", t);
    }

    /**
     * Errores. Errores posibles previstos en alguna forma. Generan salida out
     * gph trc (si no estan desactivadas)
     */
    public static void error(String msg) {
        try {
            String error = strcall() + msg;
            writeOut(error);
            if (!trlg || !wout) {
                writeTrc(error);
            }
            writeGph(error, "Error de aplicacion");
        } catch (Throwable t) {
            if (werr) {
                t.printStackTrace();
            }
        }
    }

    public static void error(Throwable e) {
        error(strerr(e));
    }

    public static void error(String msg, Throwable e) {
        error(msg + ": " + strerr(e));
    }

    /**
     * Avisos. Solo salida gph (si no esta desactivada)
     */
    public static void aviso(String msg, String tit, Object o) {
        if (!wgph) {
            return;
        }
        JOptionPane.showMessageDialog(
                (Component) o,
                msg,
                tit,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Log. mensajes de log. Sujetos al nivel de log. Generan salida out trc (si
     * no estan desactivadas)
     */
    public static void log(String msg, int level) {
        try {
            if (level > logLevel) {
                return;
            }
            String wmsg = strcall() + msg;
            writeOut(wmsg);
            if (!trlg || !wout) {
                writeTrc(wmsg);
            }
        } catch (Throwable t) {
            if (werr) {
                t.printStackTrace();
            }
        }
    }

    public static void log(String msg) {
        log(msg, 1);
    }

    /**
     * Trace. mensajes de trace. Sujetos al nivel de trace. Generan salida trc
     * (si no esta desactivada)
     */
    public static void trace (String msg, int level) {
        try {
            if (level > trcLevel) {
                return;
            }
            String wmsg = strcall() + msg;
            writeTrc(wmsg);
            if (trcp && !trlg) {
                writeOut(msg);
            }
        } catch (Throwable t) {
            if (werr) {
                t.printStackTrace();
            }
        }
    }

    public static void trace(String msg) {
        trace(msg, 1);
    }

    public static void trace(Throwable e) {
        trace(strerr(e), 1);
    }

    public static void trace(Throwable e, int level) {
        trace(strerr(e), level);
    }

    public static void trace(byte[] msg) {
        trace(new String(msg), 1);
    }

    public static void trace(byte[] msg, int level) {
        trace(new String(msg), level);
    }

    public static void tracevt(EventObject ev, String msg) {
        trace(ev.toString() + " " + msg);
    }

    public static void tracevt(EventObject ev) {
        tracevt(ev, "");
    }

    public static void tracemem() {
        try {
            if (!wtrc) {
                return;
            }
            Runtime run = Runtime.getRuntime();
            trace("totalMemory = " + run.totalMemory());
            trace("freeMemory  = " + run.freeMemory());
            trace("maxMemory   = " + run.maxMemory());
            trace("Processors  = " + run.availableProcessors());
        } catch (Throwable t) {
        }
    }

    public static void tracesys(String text) {
        try {
            if (!wtrc) {
                return;
            }
            if (text != null) {
                trace(text, 0);
            }
            tracep("java.version");
            tracep("java.class.version");
            tracep("java.home");
            tracep("java.class.path");
            tracep("java.ext.dirs");
            tracep("java.vm.name");
            tracep("os.name");
            tracep("os.version");
            tracep("user.name");
            tracep("user.language");
            tracep("user.timezone");
            tracemem();
            trace(ApTh.infoth(), 0);
            Context ctx = new InitialContext();
            if (ctx != null) {
                trace("context=" + ctx.getNameInNamespace(), 0);
            }
        } catch (Throwable t) {
        }
    }

    private static void tracep(String prop) {
        try {
            trace(prop + " = " + System.getProperty(prop));
        } catch (Throwable t) {
        }
    }

    public static void traceobj(Object o) {
        try {
            Class c = o.getClass();
            trace("class=" + c.getName() + " obj=" + o.toString());
        } catch (Exception e) {
            trace("Error traceobj");
        }
    }

    //
    public static String boxerr(String texto, String url) {
        String str;
        str = "<script language=\"JavaScript\">";
        str += " alert(\"" + texto + "\");";
        if (url == null) {
            str += " if (document.referrer != '')";
            str += " this.location.href=document.referrer;";
            str += " else this.location.href='" + urlerr + "';";
        } else {
            str += " this.location.href=" + "'" + url + "';";
        }
        str += " </script> ";
        return str;
    }

    public static String boxerr(String texto) {
        return boxerr(texto, null);
    }

    public static String boxwarn(String texto) {
        String str;
        str = "<script>";
        str += " alert(\"" + texto + "\");";
        str += " </script> ";
        return str;
    }

}
