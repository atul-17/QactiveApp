package com.cumulations.libreV2.tcp_tunneling.enums;

public enum BatteryType {
  BATTERY_HIGH(100), BATTERY_MIDDLE(70), BATTERY_LOW(30), BATTERY_WARNING(10);

    private int value;

    private BatteryType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}