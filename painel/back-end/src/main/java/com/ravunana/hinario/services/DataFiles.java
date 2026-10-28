package com.ravunana.hinario.services;

import java.io.File;

final class DataFiles {

    private DataFiles() {
    }

    static File resolve(String fileName) {
        File dir = new File(System.getProperty("user.dir"));
        for (File d = dir; d != null; d = d.getParentFile()) {
            if (new File(d, "pom.xml").exists() && new File(d, "mvnw").exists()) {
                return new File(new File(d, "data"), fileName);
            }
            File backend = new File(d, "painel/back-end");
            if (new File(backend, "pom.xml").exists()) {
                return new File(new File(backend, "data"), fileName);
            }
        }
        return new File(new File(dir, "data"), fileName);
    }
}
