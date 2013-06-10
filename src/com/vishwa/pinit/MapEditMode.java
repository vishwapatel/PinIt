package com.vishwa.pinit;

public enum MapEditMode {
    DEFAULT_MODE(0),
    CREATE_NOTE(1),
    EDIT_NOTE(2);

    private int code;

    private MapEditMode(int code) {
        this.code = code; 
    }

    public int getCode() {
        return code;
    }

}
