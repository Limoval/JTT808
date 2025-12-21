package com.lk.jtt808.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ColorEnum {

    BLUE(0x001,"蓝色"),
    YELLOW(0x002,"黄色"),
    BLACK(0x003,"黑色"),
    WHITE(0x004,"白色"),
    GREEN(0x005,"绿色"),
    OTHER(0x009,"其他"),
    FARMER_YELLOW(0x05B,"农黄色"),
    FARMER_GREEN(0x05C,"农绿色"),
    YELLOW_BLUE(0x05D,"黄绿色"),
    Gradient_GREEN(0x05E,"渐变绿"),

    ;
    // 颜色标识
    private int id;

    // 描述
    private String desc;

    public static ColorEnum getColor(int colorId){
        for(ColorEnum colorEnum : ColorEnum.values()){
            if(colorEnum.getId() == colorId){
                return colorEnum;
            }
        }
        return OTHER;
    }
}
