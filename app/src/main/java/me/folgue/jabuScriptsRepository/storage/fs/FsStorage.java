package me.folgue.jabuScriptsRepository.storage.fs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import me.folgue.jabuScriptsRepository.RepositoryServer;
import me.folgue.jabuScriptsRepository.storage.ScriptId;
import me.folgue.jabuScriptsRepository.storage.ScriptStorage;

public class FsStorage implements ScriptStorage {

    private final String basePath;

    public FsStorage(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public List<String> getGroupIds() throws Exception {
        return Arrays.stream(new File(this.basePath).listFiles())
                .map(File::getName)
                .toList();
    }

    @Override
    public Optional<List<String>> getGroupIdScripts(String groupId) throws Exception {
        File groupIdScriptStorage = Paths.get(this.basePath, groupId).toFile();
        if (!groupIdScriptStorage.exists()) {
            return Optional.empty();
        } else {
            return Optional.of(
                    Arrays.stream(groupIdScriptStorage.listFiles())
                            .map(File::getName)
                            .toList()
            );
        }
    }

    @Override
    public Optional<List<String>> getScriptVersions(String groupId, String scriptName) throws Exception {
        File targetPath = Paths.get(this.basePath, groupId, scriptName).toFile();

        if (!targetPath.isDirectory()) {
            return Optional.empty();
        }

        return Optional.of(
                Arrays.stream(targetPath.listFiles())
                        .map(File::getName)
                        .toList()
        );
    }

    @Override
    public boolean exists(ScriptId id) throws Exception {
        return new File(this.generatePath(id)).isFile();
    }

    @Override
    public Optional<String> getScriptContents(ScriptId id) throws Exception {
        String targetPath = this.generatePath(id);
        if (!this.exists(id)) {
            return Optional.empty();
        } else if (!this.isValidAbsolutePath(targetPath)) {
            RepositoryServer.LOG.warn("Attempt to access illegal path: " + targetPath);
            return Optional.empty();
        } else {
            RepositoryServer.LOG.debug("Fetching " + targetPath);
            return Optional.of(Files.readString(Paths.get(targetPath)));
        }
    }

    @Override
    public boolean saveScript(ScriptId id, String contents) throws Exception {
        var targetPath = Paths.get(this.generatePath(id));
        if (this.exists(id)) {
            return false;
        }

        // Create the storage for the script if it didn't exist.
        Files.createDirectories(targetPath.getParent());

        Files.writeString(targetPath, contents);
        return true;
    }

    private String generatePath(ScriptId scriptId) {
        return Paths.get(
                this.basePath,
                scriptId.groupId(),
                scriptId.scriptName(),
                scriptId.scriptVersion().toString()
        ).toString();
    }

    /**
     * Checks if the given <b>absolute</b> path is valid, meaning that it
     * doesn't escape out of the bounds of the {@code this.basePath}.
     *
     * @param path Absolute path to check (<b><i>NOTE: ITS NOT A RELATIVE PATH
     * FROM THE BASE PATH</i></b>)
     * @return {@code true} if the path is valid, {@code false} otherwise.
     */
    public boolean isValidAbsolutePath(String path) {
        String normalizedPath = Paths.get(path).normalize().toString();

        return normalizedPath.startsWith(this.basePath);
    }

    public String getBasePath() {
        return this.basePath;
    }
}
