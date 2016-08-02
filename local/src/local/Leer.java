package local;

import java.io.*;

/**
 * Lectura de datos desde terminal.
 */
public class Leer {
    private static String read() {
        String sdato = "";
        try {
            // Definir un flujo de caracteres de entrada: in
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(isr);
            // Leer. La entrada finaliza al pulsar la tecla Entrar
            sdato = in.readLine();
        } catch(IOException e) {
            return "";
        }
        return sdato.intern(); // devolver el dato tecleado
    }
    
    public static String dato(String texto) {
        prompt(texto);
        return read();
    }
    
    public static String dato() {
        return read();
    }
    
    private static void prompt(String texto) {
        System.out.print(texto);
        System.out.flush();
    }
    
    public static String passw(String texto) {
        MaskingThread maskingthread = new MaskingThread(texto);
        Thread th = new Thread(maskingthread);
        th.start();
        String rs = read();
        maskingthread.stopMasking();
        return rs;
    }
    
    public static short datoShort() {
        try {
            return Short.parseShort(read().trim());
        } catch(NumberFormatException e) {
            return Short.MIN_VALUE; // valor más pequeño
        }
    }
    
    public static int datoInt() {
        try {
            return Integer.parseInt(read().trim());
        } catch(NumberFormatException e) {
            return Integer.MIN_VALUE; // valor más pequeño
        }
    }
    
    public static int datoInt(String texto) {
        prompt(texto);
        return datoInt();
    }
    
    public static long datoLong() {
        try {
            return Long.parseLong(read().trim());
        } catch(NumberFormatException e) {
            return Long.MIN_VALUE; // valor más pequeño
        }
    }
    
    public static long datoLong(String texto) {
        prompt(texto);
        return datoLong();
    }
    
    public static float datoFloat() {
        try {
            Float f = new Float(read().trim());
            return f.floatValue();
        } catch(NumberFormatException e) {
            return Float.NaN; // No es un Número; valor float.
        }
    }
    
    public static double datoDouble() {
        try {
            Double d = new Double(read().trim());
            return d.doubleValue();
        } catch(NumberFormatException e) {
            return Double.NaN; // No es un Número; valor double.
        }
    }
}


//
class MaskingThread extends Thread {
    private boolean stop = false;
    private String prompt;
    
    public MaskingThread(String prompt) {
        this.prompt = prompt;
    }
    
    public void run() {
        while(!stop) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {this.stop = true;}
            if (!stop) {
                System.out.print("\r" + prompt + " \r" + prompt);
            }
            System.out.flush();
        }
    }
    
    public void stopMasking() {
        this.stop = true;
        System.out.print("\r                                                       \r");
    }
    
}

