package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.image.BufferedImage;
@Data
@AllArgsConstructor
public class ScaledImageDataVO {
    private int height;

    private BufferedImage scaledImage;

}
