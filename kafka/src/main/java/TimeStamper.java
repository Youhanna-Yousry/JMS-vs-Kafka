public class TimeStamper {
    LongConverter longConverter;

    public TimeStamper() {
        this.longConverter = new LongConverter();
    }

    public byte[] addTimeStamp(byte[] message) {
        long timestamp = System.currentTimeMillis();
        byte[] timestampBytes = longConverter.longToBytes(timestamp);
        byte[] result = new byte[message.length + timestampBytes.length];
        System.arraycopy(message, 0, result, 0, message.length);
        System.arraycopy(timestampBytes, 0, result, message.length, timestampBytes.length);
        return result;
    }

    public long extractTimeStamp(byte[] message) {
        int messageLength = message.length - Long.SIZE / Byte.SIZE;
        byte[] timestampBytes = new byte[Long.SIZE / Byte.SIZE];
        System.arraycopy(message, messageLength, timestampBytes, 0, Long.SIZE / Byte.SIZE);
        return longConverter.bytesToLong(timestampBytes);
    }
}
