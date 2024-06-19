package de.katzen48.datapack;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileRename;
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
    private DefaultTextDocumentService textDocumentService;
    //LSClientLogger clientLogger;

    public DefaultWorkspaceService(DefaultLanguageServer languageServer, CommandCompiler commandCompiler, ReflectionHelper reflectionHelper, HashMap<String, String> documentContents, DefaultTextDocumentService textDocumentService) {
        this.languageServer = languageServer;
        this.commandCompiler = commandCompiler;
        this.reflectionHelper = reflectionHelper;
        this.documentContents = documentContents;
        this.textDocumentService = textDocumentService;
        //this.clientLogger = LSClientLogger.getInstance();
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        HashSet<String> changedFiles = new HashSet<>();
        HashSet<String> deletedFiles = new HashSet<>();
        
        didChangeWatchedFilesParams.getChanges().forEach(change -> {
            if (change.getType() != FileChangeType.Deleted) {
                changedFiles.add(change.getUri());
            } else {
                deletedFiles.add(change.getUri());
            }
        });

        deletedFiles.forEach(uri -> {
            Path path = Paths.get(URI.create(URLDecoder.decode(uri, StandardCharsets.UTF_8)));
            File file = path.toFile();

            documentContents.remove(file.toPath().toUri().toString());
        });

        changedFiles.forEach(uri -> {
            Path path = Paths.get(URI.create(URLDecoder.decode(uri, StandardCharsets.UTF_8)));
            File file = path.toFile();
            try {
                StringBuilder text = new StringBuilder();
                Files.readAllLines(path).forEach(line -> {
                    text.append(line + System.lineSeparator());
                });

                documentContents.put(file.toPath().toUri().toString(), text.toString());
                textDocumentService.debounceValidation(text.toString(), file.toPath().toUri().toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void didRenameFiles(RenameFilesParams params) {
        for (FileRename file : params.getFiles()) {
            String oldUri = URLDecoder.decode(file.getOldUri(), StandardCharsets.UTF_8);
            String newUri = URLDecoder.decode(file.getNewUri(), StandardCharsets.UTF_8);

            if (documentContents.containsKey(oldUri)) {
                documentContents.put(newUri, documentContents.get(oldUri));
                documentContents.remove(oldUri);
            }
        }
    }

    @Override
    public void didDeleteFiles(DeleteFilesParams params) {
        params.getFiles().forEach(event -> {
            Path path = Paths.get(URI.create(URLDecoder.decode(event.getUri(), StandardCharsets.UTF_8)));
            File file = path.toFile();

            documentContents.remove(file.toPath().toUri().toString());
        });
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        if (params.getCommand().equals("java-datapack-language-server.convert-command")) {
            String documentUri = URLDecoder.decode(((JsonPrimitive) params.getArguments().get(0)).getAsString(), StandardCharsets.UTF_8);
            int lineStart = ((JsonPrimitive) params.getArguments().get(1)).getAsInt();
            int lineEnd = ((JsonPrimitive) params.getArguments().get(2)).getAsInt();

            WorkspaceEdit edit = new WorkspaceEdit();
            for (int i = lineStart; i <= lineEnd; i++) {
                convertCommand(documentUri, i, edit);
            }

            languageServer.languageClient.applyEdit(new ApplyWorkspaceEditParams(edit));
        } else if (params.getCommand().equals("java-datapack-language-server.convert-commands-all")) {
            WorkspaceEdit edit = new WorkspaceEdit();
            
            for (String documentUri : documentContents.keySet()) {
                convertCommands(documentUri, edit);
            }

            languageServer.languageClient.applyEdit(new ApplyWorkspaceEditParams(edit));
        } else if (params.getCommand().equals("java-datapack-language-server.reload-data")) {
            try {
                textDocumentService.reloadData();
            } catch (Exception e) {
                languageServer.languageClient.logMessage(new MessageParams(MessageType.Error, e.toString()));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private void convertCommands(String uri, WorkspaceEdit edit) {
        String currentContent = documentContents.get(uri);
        String[] lines = currentContent.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            convertCommand(uri, i, edit);
        }
    }

    private void convertCommand(String uri, int lineNo, WorkspaceEdit edit) {
        String currentContent = documentContents.get(uri);
        String line = currentContent.lines().skip(lineNo).findFirst().orElse("");

        ParseResults<Object> results = commandCompiler.compile(line);
        CommandSyntaxException exception = commandCompiler.resolveException(results);

        if (exception != null) {
            String convertedCommand = ConverterHelper.convertCommand(line, reflectionHelper);
            if (!convertedCommand.equals(line)) {
                if (!edit.getChanges().containsKey(uri)) {
                    edit.getChanges().put(uri, new ArrayList<>());
                }
                edit.getChanges().get(uri).add(new TextEdit(new Range(new Position(lineNo, 0), new Position(lineNo, line.length())), convertedCommand));
            }
        }
    }
}