package me.folgue.jabuScriptsRepository;

import me.folgue.jabuScriptsRepository.storage.ScriptStorage;
import me.folgue.jabuScriptsRepository.storage.fs.FsStorage;
import spark.Spark;

public class App {

    public final static String SCRIPT_REPOSITORY_LOCATION = System.getenv().getOrDefault("SCRIPT_REPOSITORY_LOCATION", "./scripts");
    public final static int PORT = 8080;

    public static void main(String[] args) {
        ScriptStorage storage = new FsStorage(SCRIPT_REPOSITORY_LOCATION);
        var repServer = new RepositoryServer(storage, PORT);

        repServer.start();
    }
}
