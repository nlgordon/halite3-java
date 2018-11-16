package com.insanedev.hlt

class Log {
    private final FileWriter file
    boolean debugging = false

    private static Log INSTANCE
    private static ArrayList<String> LOG_BUFFER = new ArrayList<>()

    static {
        Runtime.getRuntime().addShutdownHook(new AtExit())
    }

    private static class AtExit extends Thread {
        @Override
        void run() {
            if (INSTANCE != null) {
                return
            }

            final long now_in_nanos = System.nanoTime()
            final String filename = "bot-unknown-" + now_in_nanos + ".log"
            final FileWriter writer = new FileWriter(filename)
            for (final String message : LOG_BUFFER) {
                writer.append(message).append('\n')
            }
        }
    }

    private Log(final FileWriter f) {
        file = f
    }

    static void safeOpen(final int botId) {
        if (INSTANCE != null) {
            return
        }

        open(botId)
    }

    static void open(final int botId) {
        if (INSTANCE != null) {
            log("Error: log: tried to open(" + botId + ") but we have already opened before.")
            throw new IllegalStateException()
        }

        final String filename = "bot-" + botId + ".log"
        final FileWriter writer
        writer = new FileWriter(filename)
        INSTANCE = new Log(writer)

        try {
            for (final String message : LOG_BUFFER) {
                writer.append(message).append('\n')
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e)
        }
        writer.flush()
        LOG_BUFFER.clear()
    }

    static void log(final String message) {
        if (INSTANCE == null) {
            LOG_BUFFER.add(message)
            return
        }

        try {
            INSTANCE.file.append(message).append('\n').flush()
        } catch (final IOException e) {
            e.printStackTrace()
        }
    }

    static void debug(final String message) {
        if (INSTANCE == null) {
            LOG_BUFFER.add(message)
            return
        }

        if (!INSTANCE.debugging) {
            return
        }

        try {
            INSTANCE.file.append(message).append('\n').flush()
        } catch (final IOException e) {
            e.printStackTrace()
        }
    }

    static void enableDebugging() {
        if (INSTANCE) {
            INSTANCE.debugging = true
        }
    }
}
