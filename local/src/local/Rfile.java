package local;

import java.io.*;

/**
 * Utilidades para i/o archivos raw
 * @author sgs 10.01.04
 * @version 1.0
 */

public class Rfile {
    private RandomAccessFile raf = null;
    private String fname = null;
    private byte[] buf = null;
    private int  head = 0;
    private int  tail = 0;
    private int  flen = 0;
    
    //
    public Rfile(String fname) throws Exception {
        try {
            this.fname = fname;
            raf = new RandomAccessFile(fname,"r");
            flen = (int) raf.length();
            buf = new byte[flen];
            raf.read(buf);
            tail = flen-1;
            close();
        } 
        catch (Exception e) {
            ApW.error(e);
            close();
            throw e;
        }
    }

    //
    public Rfile(String fname, byte buf[]) throws Exception {
        try {
            this.fname = fname;
            raf = new RandomAccessFile(fname,"rw");
            flen = buf.length;
            raf.write(buf);
            raf.setLength(flen);
            tail = flen-1;
            close();
        } 
        catch (Exception e) {
            ApW.error(e);
            close();
            throw e;
        }
    }
    
    //
    //
    //
    public void close() throws Exception {
        if (raf != null) {
            raf.close();
            raf=null;
        }
    }
    
    //
    // Lee lineas de texto despreciando lineas en blanco
    //
    public String readLine() {
        for (; head <= tail; head++)
            if (buf[head] != '\n' && buf[head] != '\r')
                break;
        if (head > tail)
            return null;
        int head0 = head;
        for (; head <= tail; head++)
            if (buf[head] == '\n' || buf[head] == '\r')
                break;
        return new String(buf,head0,head-head0);
    }
    
    public String getFname() {
        return fname;
    }
}

