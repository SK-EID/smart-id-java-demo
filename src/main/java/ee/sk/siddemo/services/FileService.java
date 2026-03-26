package ee.sk.siddemo.services;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2025 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    @Value("${app.signed-files-directory}")
    private String signedFilesDirectory;

    public String createPath() {
        File containerFile = null;
        try {
            containerFile = File.createTempFile("sid-demo-container-", ".asice");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file", e);
        }
        Path targetPath = createSavePath(containerFile);
        return targetPath.toString();
    }

    private Path createSavePath(File containerFile) {
        Path targetDir = Paths.get(signedFilesDirectory);
        File directory = targetDir.toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return targetDir.resolve(containerFile.getName());
    }
}
