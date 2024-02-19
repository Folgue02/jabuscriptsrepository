package me.folgue.jabuScriptsRepository;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import java.util.*;
import me.folgue.jabuScriptsRepository.storage.ScriptId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.*;

import me.folgue.jabuScriptsRepository.storage.ScriptStorage;
import spark.Request;
import spark.Response;

public class RepositoryServer {

    public final static Logger LOG = LoggerFactory.getLogger(RepositoryServer.class);
    private final ScriptStorage storage;
    private final Integer port;
    private final Gson gson = new Gson();

    public RepositoryServer(ScriptStorage storage, int port) {
        this.storage = storage;
        this.port = port;
    }

    private String handleListScripts(Request req, Response res) throws Exception {
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
    }

    private String handleListGroupIds(Request req, Response res) throws Exception {
        res.type("application/json");
        return gson.toJson(this.storage.getGroupIds());
    }

    private String handleListVersions(Request req, Response res) throws Exception {
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
    }

    private String handleGetScript(Request req, Response res) throws Exception {
        String groupId = req.params("groupId");
        String scriptName = req.params("script");
        String scriptVersion = req.params("scriptVersion");
        Optional<Version> parsedScriptVersion = Version.tryParse(scriptVersion);
        var responseContent = "";
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
    }

    public String handlePostScript(Request req, Response res) throws Exception {
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
    }

    private void exceptionHandler(Exception e) {
        LOG.error(e.toString());
    }

    public void start() {
        // TODO: Refactor (separate handlers into different functions)
        port(this.port);
        initExceptionHandler(e -> this.exceptionHandler(e));

        path("/api", () -> {
            path("/list", () -> {
                get("/scripts/:groupId", (req, res) -> this.handleListScripts(req, res));
                get("/groupids", (req, res) -> this.handleListGroupIds(req, res));
                get("/versions/:groupId/:script", (req, res) -> this.handleListVersions(req, res));
            });

            path("/get", () -> {
                get("/script/:groupId/:script/:scriptVersion", (req, res) -> this.handleGetScript(req, res));
            });

            path("/post", () -> {
                post("/script/:groupId/:script/:scriptVersion", (req, res) -> this.handlePostScript(req, res));
            });
        });
    }
}
