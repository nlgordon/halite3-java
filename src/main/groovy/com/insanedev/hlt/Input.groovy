package com.insanedev.hlt

class Input {
    private final String[] input
    private int current

    Input(final String line) {
        Log.log("Read input: '$line'")
        input = line.split(" ")
    }

    int getInt() {
        return Integer.parseInt(input[current++])
    }

    static Input readInput() {
        return new Input(readLine())
    }

    static String readLine() {
        try {
            final StringBuilder builder = new StringBuilder()

            int buffer
            for (; (buffer = System.in.read()) >= 0;) {
                if (buffer == '\n') {
                    break
                }
                if (buffer == '\r') {
                    // Ignore carriage return if on windows for manual testing.
                    continue
                }
                builder.append((char)buffer)
            }

            return builder.toString()
        } catch (final Exception e) {
            Log.log("Input connection from server closed. Exiting...")
            System.exit(0)
            throw new IllegalStateException(e)
        }
    }
}
