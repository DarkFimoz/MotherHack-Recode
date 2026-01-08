package fun.motherhack.utils.other;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class FileUtils {

    public void reset(String str) throws IOException {
        Path path = Paths.get(str);
        if (Files.exists(path)) new File(str).delete();
        Files.createFile(path);
    }

    public boolean exists(String str) {
        return Files.exists(Paths.get(str));
    }
}