package com.http.load.tool.dataobjects;

/**
 * Created by manish kumar.
 */
public class Parameter {

    private String name;
    private String value;

    // Used to optimize the execution. User doesn't provide this as input.
    private boolean replaceValue;

    public String getName() {
        return name;
    }

    public Parameter setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Parameter setValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isReplaceValue() {
        return replaceValue;
    }

    public Parameter setReplaceValue(boolean replaceValue) {
        this.replaceValue = replaceValue;
        return this;
    }
}
