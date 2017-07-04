package com.http.load.tool.dataobjects;

/**
 * Created by manish kumar.
 */
public class TestInput {

    // Allows changing test input data at runtime.
    private HttpLoadInput httpLoadInput = new HttpLoadInput();

    public HttpLoadInput getHttpLoadInput() {
        return httpLoadInput;
    }

    public TestInput setHttpLoadInput(HttpLoadInput httpLoadInput) {
        this.httpLoadInput = httpLoadInput;
        return this;
    }
}
