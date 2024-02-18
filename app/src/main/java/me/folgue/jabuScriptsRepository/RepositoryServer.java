package me.folgue.jabuScriptsRepository;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import java.util.*;
import me.folgue.jabuScriptsRepository.storage.ScriptId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.*;

import me.folgue.jabuScriptsRepository.storage.ScriptStorage;

public class RepositoryServer {

    private final static Logger LOG = LoggerFactory.getLogger(RepositoryServer.class);
    private final ScriptStorage storage;
    private final Integer port;

    public RepositoryServer(ScriptStorage storage, int port) {
        this.storage = storage;
        this.port = port;
    }

    public void start() {
        // TODO: Refactor (separate handlers into different functions)
        var gson = new Gson();
        port(this.port);
        initExceptionHandler(e -> {
            LOG.error(e.toString());
        });

        path("/api", () -> {
            path("/list", () -> {
                get("/scripts/:groupId", (req, res) -> {
                    res.type("application/json");
                    String targetGroupId = req.params("groupId");
                    Optional<List<String>> scriptsOptional = this.storage.getGroupIdScripts(targetGroupId);
                    var responseContent = "";

                    if (scriptsOptional.isPresent()) {
                        List<String> scripts = scriptsOptional.get();
                        responseContent = gson.toJson(scripts);
                    } else {
                        res.status(404);
                    }

                    return responseContent;
                });

                get("/groupids", (req, res) -> {
                    res.type("application/json");
                    return gson.toJson(this.storage.getGroupIds());
                });

                get("/versions/:groupId/:script", (req, res) -> {
                    res.type("application/json");
                    String groupId = req.params("groupId");
                    String scriptName = req.params("script");
                    var responseContent = "";
                    Optional<List<String>> scriptVersions = this.storage.getScriptVersions(groupId, scriptName);

                    if (scriptVersions.isEmpty()) {
                        res.status(404);
                    } else {
                        responseContent = gson.toJson(scriptVersions.get());
                    }

                    return responseContent;
                });
            });

            path("/get", () -> {
                get("/script/:groupId/:script/:scriptVersion", (req, res) -> {
                    String groupId = req.params("groupId");
                    String scriptName = req.params("script");
                    String scriptVersion = req.params("scriptVersion");
                    Optional<Version> parsedScriptVersion = Version.tryParse(scriptVersion);
                    String responseContent = "";
                    res.type("application/json");

                    if (parsedScriptVersion.isEmpty()) {
                        res.status(400);
                    } else {
                        Optional<String> script = this.storage.getScriptContents(new ScriptId(groupId, scriptName, parsedScriptVersion.get()));
                        if (script.isEmpty()) {
                            res.status(404);
                        } else {
                            responseContent = script.get();
                        }
                    }

                    return responseContent;
                });

            });

            path("/post", () -> {
                post("/script/:groupId/:script/:scriptVersion", (req, res) -> {
                    String groupId = req.params("groupId");
                    String scriptName = req.params("script");
                    String scriptVersion = req.params("scriptVersion");
                    Optional<Version> parsedScriptVersion = Version.tryParse(scriptVersion);
                    String scriptContent = req.body();
                    String responseContent = "";
                    res.type("text/plain");

                    if (parsedScriptVersion.isEmpty()) {
                        res.status(400);
                    } else {
                        var scriptId = new ScriptId(groupId, scriptName, parsedScriptVersion.get());
                        if (this.storage.exists(scriptId)) {
                            res.status(409);
                        } else {
                            this.storage.saveScript(scriptId, req.body());
                        }
                    }

                    return responseContent;
                });
            });
        });
    }
}
