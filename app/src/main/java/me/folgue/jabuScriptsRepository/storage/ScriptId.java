package me.folgue.jabuScriptsRepository.storage;

import com.github.zafarkhaja.semver.Version;

public record ScriptId(String groupId, String scriptName, Version scriptVersion) {
}
