package com.potato.potatotool.content.redTeam.payload;

public class PayloadOutputRequest {
    private final byte[] bytes;
    private final PayloadFormat format;
    private final String className;
    private final String outputPath;
    private final String loaderClassName;

    private PayloadOutputRequest(Builder builder) {
        this.bytes = builder.bytes;
        this.format = builder.format;
        this.className = builder.className;
        this.outputPath = builder.outputPath;
        this.loaderClassName = builder.loaderClassName;
    }

    public static Builder builder(byte[] bytes, PayloadFormat format) {
        return new Builder(bytes, format);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public PayloadFormat getFormat() {
        return format;
    }

    public String getClassName() {
        return className;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getLoaderClassName() {
        return loaderClassName;
    }

    public static class Builder {
        private final byte[] bytes;
        private final PayloadFormat format;
        private String className;
        private String outputPath;
        private String loaderClassName;

        private Builder(byte[] bytes, PayloadFormat format) {
            this.bytes = bytes;
            this.format = format;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder loaderClassName(String loaderClassName) {
            this.loaderClassName = loaderClassName;
            return this;
        }

        public PayloadOutputRequest build() {
            return new PayloadOutputRequest(this);
        }
    }
}
