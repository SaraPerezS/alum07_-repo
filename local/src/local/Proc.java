package local;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Utilidades procesos
 * @author sgs 11.06.27
 * @version 1.0
 */

public class Proc {

    //
    public static String[][] exec1 (String cmd, String error) throws Exception {
        ApW.trace("cmd="+cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdError = new BufferedReader(new 
                 InputStreamReader(p.getErrorStream()));
        BufferedReader stdOut = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));
        ArrayList<String[]> m = new ArrayList<String[]>();
        String[] r;
        String[][] t = null;
        int rc = p.waitFor();
        String s;
        error="";
        while ((s = stdError.readLine()) != null) 
                error += s;
        if (rc != 0) 
            return null;
        while ((s = stdOut.readLine()) != null) {
             r = new String[2];
             int i=s.indexOf(". ");
             if (i > 0) {
                 int j=s.indexOf(" .");
                 if (j <= 0)
                     j=i;
                 r[0] = s.substring(0,j);
                 r[1] = s.substring(i+1);
             }
             else {
                 if (s.equals(""))
                     continue;
                 r[0] = s;
                 r[1] = "";
             }
             m.add(r);
        }
        t = (String[][])m.toArray(new String[1][]);
        ApW.trace("registros formados " +t.length);
        return t;
    }
    
}

