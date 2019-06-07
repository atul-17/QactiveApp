package com.cumulations.libreV2.tcp_tunneling.enums;

public enum AQModeSelect {
    Trillium(0x00), Power(0x01), Left(0x02), Right(0x03);

    private int value;

    private AQModeSelect(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}