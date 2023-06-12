package xyz.cirno.pseudodcdimming.util;

public class PerceptualQuantizer {
    private static final double m1 = 2610.0 / 16384.0;
    private static final double m2 = 128.0 * 2523.0 / 4096.0;
    private static final double c1 = 3424.0 / 4096.0;
    private static final double c2 = 32.0 * 2413.0 / 4096.0;
    private static final double c3 = 32.0 * 2392.0 / 4096.0;

    public static double NitsToSignal(double nits) {
        var Ypowm1 = Math.pow(nits / 10000.0, m1);
        return Math.pow((c1 + c2 * Ypowm1) / (1.0 + c3 * Ypowm1), m2);
    }

    public static double SignalToNits(double signal)
    {
        var Epow1divm2 = Math.pow(signal, 1.0 / m2);
        return 10000 * Math.pow(Math.max(Epow1divm2 - c1, 0) / (c2 - c3 * Epow1divm2), 1 / m1);
    }
}
