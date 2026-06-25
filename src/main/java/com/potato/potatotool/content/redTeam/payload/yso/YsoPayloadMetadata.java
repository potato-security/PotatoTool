package com.potato.potatotool.content.redTeam.payload.yso;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class YsoPayloadMetadata {
    private final String name;
    private final Set<YsoPayloadCategory> categories;
    private final YsoPayloadInputType inputType;
    private final boolean implemented;
    private final boolean supportsClassFile;
    private final String dependencyHint;
    private final String verificationHint;

    private YsoPayloadMetadata(Builder builder) {
        this.name = builder.name;
        this.categories = Collections.unmodifiableSet(new LinkedHashSet<YsoPayloadCategory>(builder.categories));
        this.inputType = builder.inputType;
        this.implemented = builder.implemented;
        this.supportsClassFile = builder.supportsClassFile;
        this.dependencyHint = builder.dependencyHint;
        this.verificationHint = builder.verificationHint;
    }

    public String getName() {
        return name;
    }

    public Set<YsoPayloadCategory> getCategories() {
        return categories;
    }

    public YsoPayloadInputType getInputType() {
        return inputType;
    }

    public boolean isImplemented() {
        return implemented;
    }

    public boolean supportsClassFile() {
        return supportsClassFile;
    }

    public String getDependencyHint() {
        return dependencyHint;
    }

    public String getVerificationHint() {
        return verificationHint;
    }

    public boolean hasCategory(YsoPayloadCategory category) {
        return category == YsoPayloadCategory.ALL || categories.contains(category);
    }

    public static Builder builder(String name, YsoPayloadInputType inputType) {
        return new Builder(name, inputType);
    }

    public static class Builder {
        private final String name;
        private final YsoPayloadInputType inputType;
        private final Set<YsoPayloadCategory> categories = new LinkedHashSet<YsoPayloadCategory>();
        private boolean implemented = true;
        private boolean supportsClassFile;
        private String dependencyHint = "";
        private String verificationHint = "";

        private Builder(String name, YsoPayloadInputType inputType) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Yso metadata name is empty");
            }
            if (inputType == null) {
                throw new IllegalArgumentException("Yso metadata input type is null: " + name);
            }
            this.name = name;
            this.inputType = inputType;
        }

        public Builder categories(YsoPayloadCategory... values) {
            if (values != null) {
                categories.addAll(Arrays.asList(values));
            }
            return this;
        }

        public Builder supportsClassFile(boolean value) {
            this.supportsClassFile = value;
            return this;
        }

        public Builder dependencyHint(String value) {
            this.dependencyHint = value == null ? "" : value;
            return this;
        }

        public Builder verificationHint(String value) {
            this.verificationHint = value == null ? "" : value;
            return this;
        }

        public Builder implemented(boolean value) {
            this.implemented = value;
            return this;
        }

        public YsoPayloadMetadata build() {
            if (categories.isEmpty()) {
                categories.add(YsoPayloadCategory.CLASS_LOADING);
            }
            return new YsoPayloadMetadata(this);
        }
    }
}
