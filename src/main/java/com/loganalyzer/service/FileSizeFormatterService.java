package com.loganalyzer.service;

import org.springframework.stereotype.Service;

@Service
public class FileSizeFormatterService {

    public String format(Long sizeInBytes) {

        if (sizeInBytes == null || sizeInBytes <= 0) {
            return "0 B";
        }

        double size = sizeInBytes;

        if (size < 1024) {
            return sizeInBytes + " B";
        }

        size = size / 1024;

        if (size < 1024) {
            return trim(size) + " KB";
        }

        size = size / 1024;

        if (size < 1024) {
            return trim(size) + " MB";
        }

        size = size / 1024;

        return trim(size) + " GB";
    }

    private String trim(double value) {

        String formatted = String.format("%.1f", value);

        return formatted.endsWith(".0")
                ? formatted.substring(0, formatted.length() - 2)
                : formatted;
    }
}
