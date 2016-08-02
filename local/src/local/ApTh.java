package local;

/**
 * Utilidades para threads
 *
 * @author sgs 16.05.13
 */
public class ApTh {

    /**
     * Pausa de los milisegundos indicados.
     */
    public static void Sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Genera una excepcion arbitraria sin necesidad de declarar throws.
     */
    @SuppressWarnings("deprecation")
    public static void sThrow(Throwable e) {
        Thread.currentThread().stop(e);
    }

    /**
     * Espera hasta final thread un tiempo maximo. Devuelve el tiempo restante
     */
    public static int weth(Thread t, int msec) {
        while (msec > 0) {
            try {
                if (!t.isAlive()) {
                    return msec;
                }
                Thread.sleep(20);
                msec -= 20;
            } catch (InterruptedException e) {
            }
        }
        return 0;
    }

    /**
     * Devuelve informacion thread en curso
     */
    public static String infoth() {
        try {
            Thread th = Thread.currentThread();
            return "thread id=" + th.getId() + " name=" + th.getName() + " pri=" + th.getPriority()
                    + " group=" + th.getThreadGroup().getName() + " acnt=" + Thread.activeCount();
        } catch (Throwable t) {
        }
        return "";
    }
}
