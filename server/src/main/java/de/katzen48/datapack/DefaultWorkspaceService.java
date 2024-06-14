package de.katzen48.datapack;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.katzen48.datapack.converters.ConverterHelper;

public class DefaultWorkspaceService implements WorkspaceService {

    private DefaultLanguageServer languageServer;
    private CommandCompiler commandCompiler;
    private ReflectionHelper reflectionHelper;
    private HashMap<String, String> documentContents;
    //LSClientLogger clientLogger;

    public DefaultWorkspaceService(DefaultLanguageServer languageServer, CommandCompiler commandCompiler, ReflectionHelper reflectionHelper, HashMap<String, String> documentContents) {
        this.languageServer = languageServer;
        this.commandCompiler = commandCompiler;
        this.reflectionHelper = reflectionHelper;
        this.documentContents = documentContents;
        //this.clientLogger = LSClientLogger.getInstance();
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {

    }

    @Override
    public void didRenameFiles(RenameFilesParams params) {

    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        if (params.getCommand().equals("java-datapack-language-server.convert-command")) {
            String documentUri = URLDecoder.decode(((JsonPrimitive) params.getArguments().get(0)).getAsString(), StandardCharsets.UTF_8);
            int lineNo = ((JsonPrimitive) params.getArguments().get(1)).getAsInt();
            convertCommand(documentUri, lineNo);
        } else if (params.getCommand().equals("java-datapack-language-server.convert-commands-all")) {
            for (String documentUri : documentContents.keySet()) {
                convertCommands(documentUri);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private void convertCommands(String uri) {
        String currentContent = documentContents.get(uri);
        String[] lines = currentContent.split(System.lineSeparator());

        for (int i = 0; i < lines.length; i++) {
            convertCommand(uri, i);
        }
    }

    private void convertCommand(String uri, int lineNo) {
        String currentContent = documentContents.get(uri);
        String line = currentContent.lines().skip(lineNo).findFirst().orElse("");

        ParseResults<Object> results = commandCompiler.compile(line);
        CommandSyntaxException exception = commandCompiler.resolveException(results);

        if (exception != null) {
            String convertedCommand = ConverterHelper.convertCommand(line, reflectionHelper);
            if (!convertedCommand.equals(line)) {
                WorkspaceEdit edit = new WorkspaceEdit();
                edit.getChanges().put(uri, List.of(new TextEdit(new Range(new Position(lineNo, 0), new Position(lineNo, line.length())), convertedCommand)));
                languageServer.languageClient.applyEdit(new ApplyWorkspaceEditParams(edit));
            }
        }
    }
}