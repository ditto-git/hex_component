package com.ditto.hex_component.hex_import.check;


public class CheckResult {
    public CheckResult(boolean result, String msg) {
        this.result = result;
        this.msg = msg;
    }

    private  boolean result;

    private  String msg;
}
