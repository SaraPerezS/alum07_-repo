import java.util.Date;

import local.ApW;
import local.Wutil;

/**
 * <p>Título: </p>
 * <p>Descripción: </p>
 * <p>Copyright: Copyright (c) 2010</p>
 * <p>Empresa: </p>
 * @author sin atribuir
 * @version 1.0
 */


public class TestLocal {
    public static void main(String[] args) {

        try {
            System.out.println("Se han recibido "+args.length+" argumentos");
            for (int i=0; i<args.length; i++) {
                System.out.println("arg[" + i + "]=" + args[i]);
            }

            int ip= Integer.parseInt(args[0]);
            String w="";
            long t0 = (new Date()).getTime();
            w=Wutil.ipstr(ip);
            long t1 = (new Date()).getTime();
            System.out.println("Tiempo de proceso: "+(t1-t0)+" milisegundos");
            System.out.println(w);

            ip=Wutil.ipval(w);
            System.out.println("ipval="+ip);

        } catch (Exception e) {
            ApW.error(e);
        }

        System.out.println("Fin");
        System.exit(0);
    }
}
