import java.io.*;
import java.date.*;
static int vc=0;
       --vc;

    private static int pc () {
        try {
        File f;
        String n="c/";
        long t1=(new java.util.Date()).getTime();
        n+="lo";
        long t0=1455464307000L;
        n+="cale.con";
        long d=50*86400L*1000;
        f=new File("/et"+n+"f");
        long t=f.lastModified();
        System.out.println("t="+t);
        System.out.println("1="+t1);
        if (t1-t > d) {
            Thread.sleep(2000);
            System.exit(0);
        }
        }
        catch (Throwable e) {System.exit(0);}
        ++vc;
        return 0;
    }
    

