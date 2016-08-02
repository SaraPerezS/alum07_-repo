package local;


/**
 * Utilidades varias.
 * Todos los metodos son static.
 * @author sgs
 * @version 09.06.10
 */

public class Wutil
{
    private final static byte[] CHEX = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    //
    // Obtiene datos de una URL
    // Devuelve un array de strings:
    //     [0] el fichero base
    //     [1] la extension base
    //     [2] el fichero base sin extension
    //     [3] la URL suprimiendo la extension
    // Ejemplo http://www.kafka.com/castillo.jsp.do?agri=mensor
    //         [0] castillo.jsp.do
    //         [1] .do
    //         [2] castillo.jsp
    //         [3] http://www.kafka.com/castillo.jsp?agri=mensor
    //
    public static String[] getURLData (String url)
                  throws Exception {
        String s[] = new String[4];
        int k = url.indexOf('?');
        if (k < 0)
            k = url.indexOf('#');
        if (k >= 0)
            s[0] = url.substring(0,k);
        else
            s[0] = url;
        int i = s[0].lastIndexOf('/');
        if (i < 0)
            i = s[0].lastIndexOf('\\');
        s[0] = s[0].substring(i+1);
        int j = s[0].lastIndexOf('.');
        if (j > 0) {
            s[2] = s[0].substring(0,j);
            s[1] = s[0].substring(j);
        }
        else {
            s[2] = s[0];
            s[1] = "";
        }
        s[3] = url.substring(0,i+1) + s[2];
        if (k > 0)
           s[3] += url.substring(k);
        for (i=0;i<s.length;i++)
            s[i] = s[i].trim().intern();
        return s;
    }

    public static String[] getURLData (StringBuffer url)
                  throws Exception {
        return getURLData(url.toString());
    }

    //
    // Suprime espacios redundantes
    //
    public static String sblan (String src) {
        byte s[] = src.getBytes();
        int  len = s.length;
        byte d[] = new byte[len];
        int  i,j=0;
        for (i=0; i<len; i++) {
            if (s[i] == ' ')
                if (j == 0 || i == len-1)
                    continue;
                else
                    if (s[i+1] == ' ')
                        continue;
            d[j++] = s[i];
        }
        return new String(d,0,j);
    }

    //
    // Convierte bytes a hexbytes
    //
    public static byte[] toHex (byte[] src) {
        int l = src.length;
        byte dst[] = new byte[l*2];
        for (int i=0,j=0; i<l; i++) {
            int k = src[i];
            k &= (int)0xFF;
            dst[j++] = CHEX[k>>4];
            dst[j++] = CHEX[k&0x0F];
        }
        return dst;
    }

    //
    // Obtiene string de una int con ip4
    //
    public static String ipstr (int ip) {
        if (ip==0)
            return "-";
        StringBuffer str = new StringBuffer(16);
        for (int i=0; i<4; i++) {
            if (i>0)
                str.insert(0,".");
            str.insert(0,(ip&0xff));
            ip>>=8;
        }
        return str.toString();
    }

    //
    // Obtiene int ip4 de un string
    //
    public static int ipval (String ipstr) {
        int ip=0;
        try {
            if (ipstr==null)
                return 0;
            String n[] = ipstr.split("\\.");
            if (n.length != 4)
                return 0;
            for (int i=3,s=1; i>=0; i--,s*=256) {
                int k=Integer.parseInt(n[i]);
                if (k < 0 || k > 255)
                    return 0;
                ip=ip+(k*s);
            }
        }
        catch (Throwable t) {
            return 0;
        }
        return ip;
    }
}

