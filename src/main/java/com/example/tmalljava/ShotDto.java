package com.example.tmalljava;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.image.BufferedImage;

@Data
@AllArgsConstructor
public class ShotDto {
    private String name;
    private BufferedImage bufferedImage;
}
