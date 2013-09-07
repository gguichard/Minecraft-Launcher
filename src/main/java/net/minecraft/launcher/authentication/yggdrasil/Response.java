package net.minecraft.launcher.authentication.yggdrasil;

public class Response {
    private String error;
    private String errorMessage;
    private String cause;

    public String getCause() {
        return cause;
    }

    public String getError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}