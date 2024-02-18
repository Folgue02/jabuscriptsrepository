package me.folgue.jabuScriptsRepository.storage;

import java.util.*;
import java.util.Optional;

public interface ScriptStorage {

    boolean exists(ScriptId id) throws Exception;

    Optional<String> getScriptContents(ScriptId id) throws Exception;

    List<String> getGroupIds() throws Exception;

    Optional<List<String>> getGroupIdScripts(String groupId) throws Exception;

    Optional<List<String>> getScriptVersions(String groupId, String scriptName) throws Exception;

    default String getScriptInputStreamUnchecked(ScriptId id) throws Exception {
        var inputStream = this.getScriptContents(id);
        if (inputStream.isPresent()) {
            return inputStream.get();
        }
        return null;
    }

    /**
     * @param id Id of the new script.
     * @param contents Contents of the new script.
     * @return {@code true} if the script id didn't exist already in the storage
     * and the new script was stored.
     */
    boolean saveScript(ScriptId id, String contents) throws Exception;
}
