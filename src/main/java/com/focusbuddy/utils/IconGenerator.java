package com.focusbuddy.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IconGenerator {
    
    public static void generateDefaultIcon() {
        try {
            // Create a simple 64x64 icon
            WritableImage image = new WritableImage(64, 64);
            PixelWriter pixelWriter = image.getPixelWriter();
            
            // Create a simple gradient background
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    double hue = (x + y) * 2.0;
                    Color color = Color.hsb(hue, 0.8, 0.9);
                    pixelWriter.setColor(x, y, color);
                }
            }
            
            // This would save the image to resources folder
            // In a real application, you'd use ImageIO or similar
            System.out.println("Default icon generated (placeholder)");
            
        } catch (Exception e) {
            System.err.println("Failed to generate icon: " + e.getMessage());
        }
    }
}
