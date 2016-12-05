    private static Logger logger = Logger.getLogger("NetKit");

    static {
        System.setProperty("java.util.logging.config.file","logging.properties");
        logger = Logger.getLogger("AnalyzeMultiROC");
        // logger.log(Level.FINE, prefix + " memory usage: used=" + (((double) u) / 1024000.0) + "M committed=" + (((double) c) / 1024000.0) + "M");
    }
