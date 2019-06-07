package com.cumulations.libreV2.tcp_tunneling.enums;

public enum ModelId {
    Arena(0x01), Festival(0x02), Central(0x03), Concert(0x04),
    Stadium(0x05), G1(0x06), G2(0x07), Station(0x08);

    private int value;

    private ModelId(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}