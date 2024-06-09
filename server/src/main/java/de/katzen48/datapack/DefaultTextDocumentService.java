package de.katzen48.datapack;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultTextDocumentService implements TextDocumentService {
    private DefaultLanguageServer languageServer;
    private CommandCompiler commandCompiler;
    private ReflectionHelper reflectionHelper;
    //private LSClientLogger clientLogger;
    private HashMap<String, Integer> validationTasks;

    private final HashMap<String, String> documentContents = new HashMap<>();

    public DefaultTextDocumentService(DefaultLanguageServer languageServer, CommandCompiler commandCompiler, ReflectionHelper reflectionHelper, HashMap<String, Integer> validationTasks) {
        this.languageServer = languageServer;
        this.commandCompiler = commandCompiler;
        this.reflectionHelper = reflectionHelper;
        this.validationTasks = validationTasks;
        //this.clientLogger = LSClientLogger.getInstance();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {
        List<TextDocumentContentChangeEvent> contentChanges = didChangeTextDocumentParams.getContentChanges();
        documentContents.put(didChangeTextDocumentParams.getTextDocument().getUri(), contentChanges.get(contentChanges.size() - 1).getText());
        debounceValidation(contentChanges.get(contentChanges.size() - 1).getText(), didChangeTextDocumentParams.getTextDocument().getUri());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {
        //debounceValidation(didSaveTextDocumentParams.getText(), didSaveTextDocumentParams.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        if (position.getPosition().getCharacter() == 0) {
            return commandCompiler.getCompletionSuggestions("", 0).thenApply(suggestions -> {
                return Either.forLeft(createCompletionItems(suggestions, false));
            });
        }

        String currentContent = documentContents.get(position.getTextDocument().getUri());
        if (currentContent == null) {
            return CompletableFuture.supplyAsync(() -> Either.forLeft(List.of()));
        }

        String line = currentContent.lines().skip(position.getPosition().getLine()).findFirst().orElse("");
        int cursor = position.getPosition().getCharacter();
        boolean hasWhiteSpace = line.substring(0, cursor).contains(" ");

        return commandCompiler.getCompletionSuggestions(line, cursor).thenApply(suggestions -> {
            return Either.forLeft(createCompletionItems(suggestions, hasWhiteSpace));
        });
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            Command command = new Command("test", "test");
            CodeAction codeAction = new CodeAction("test");
            codeAction.setCommand(command);

            return List.of(Either.forLeft(command));
        });
    }

    private List<CompletionItem> createCompletionItems(Suggestions suggestions, boolean containsWhitespace) {
        ArrayList<CompletionItem> completionItems = new ArrayList<>();
        
        suggestions.getList().forEach(suggestion -> {
            if (suggestion.getText().isEmpty()) {
                return;
            }
            if (!containsWhitespace && suggestion.getText().contains(":")) {
                return;
            }

            CompletionItem completionItem = new CompletionItem();
            completionItem.setLabel(suggestion.getText());
            completionItem.setInsertText(suggestion.getText());

            completionItems.add(completionItem);
        });

        return completionItems;
    }

    public void debounceValidation(String text, String documentUri) {
        if (validationTasks.containsKey(documentUri)) {
            if (Bukkit.getScheduler().isQueued(validationTasks.get(documentUri)) || Bukkit.getScheduler().isCurrentlyRunning(validationTasks.get(documentUri))) {
                Bukkit.getScheduler().cancelTask(validationTasks.get(documentUri));
            }
            validationTasks.remove(documentUri);
        }

        validationTasks.put(documentUri, Bukkit.getScheduler().scheduleSyncDelayedTask(LanguagePlugin.getProvidingPlugin(getClass()), () -> {
            ArrayList<Diagnostic> diagnostics = new ArrayList<>();
    
            AtomicInteger lineNo = new AtomicInteger(-1);
            text.lines().forEach(line -> {
                lineNo.incrementAndGet();
                line = line.stripTrailing();
    
                if (!line.isBlank() && !line.startsWith("#")) {
                    ParseResults<Object> results = commandCompiler.compile(line);
    
                    CommandSyntaxException exception = commandCompiler.resolveException(results);
    
                    if (exception != null) {
                        Diagnostic diagnostic = new Diagnostic(new Range(new Position(lineNo.get(), exception.getCursor()), new Position(lineNo.get(), exception.getCursor())), exception.getMessage());
                        diagnostic.setSeverity(DiagnosticSeverity.Error);
                        diagnostics.add(diagnostic);
                        return;
                    }

                    results.getContext().getArguments().forEach((name, argument) -> {
                        validateArgument(name, argument, results.getContext(), diagnostics, lineNo.get());
                    });
                }
            });
            languageServer.languageClient.publishDiagnostics(new PublishDiagnosticsParams(documentUri, diagnostics));

            validationTasks.remove(documentUri);
        }, 15L));
    }

    private void validateArgument(String name, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        if (reflectionHelper.getCompoundTagClass().isInstance(argument.getResult())) {
            validateCompoundTag(argument.getResult(), argument, context, diagnostics, lineNo);
            return;
        }
    }

    private void validateCompoundTag(Object tag, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        if (context.getArguments().containsKey("entity")) {
            validateEntityCompoundTag(tag, argument, context, diagnostics, lineNo);
        } else {
            //log("Could not find entity argument at line " + lineNo + " with value " + argument.getResult());
        }
    }

    private void validateEntityCompoundTag(Object tag, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        ParsedArgument<?,?> entity = context.getArguments().get("entity");
        if (reflectionHelper.getReferenceClass().isInstance(entity.getResult())) {
            if (reflectionHelper.getResourceLocationPathFromEntityTypeReference(entity.getResult()).equals("entity_type")) {
                validateEntityTypeCompoundTag(tag, argument, entity.getResult(), context, diagnostics, lineNo);
            }
        } else {
            //log("Unknown entity argument: " + entity.getClass().getName() + " at line " + lineNo + " with value " + entity);
        }
    }

    private void validateEntityTypeCompoundTag(Object tag, ParsedArgument<?,?> argument, Object entityType, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {        
        try {
            Object vec3 = reflectionHelper.getVec3Proxy().create(0, 0, 0);
            Object entity = reflectionHelper.getSummonCommandProxy().createEntity(
                reflectionHelper.getMinecraftServerProxy().createCommandSourceStack(reflectionHelper.getMinecraftServerProxy().getServer()),
                entityType, vec3, tag, true);
            Object dataAccessor = reflectionHelper.getEntityDataAccessorProxy().create(entity);

            reflectionHelper.getCompoundTagProxy().getAllKeys(tag).forEach(key -> {
                if (isNbtException(key)) {
                    return;
                }

                Object entityData = reflectionHelper.getEntityDataAccessorProxy().getData(dataAccessor);
                if (! reflectionHelper.getCompoundTagProxy().contains(entityData, key)) {
                    Diagnostic diagnostic = new Diagnostic(new Range(new Position(lineNo, argument.getRange().getStart()), new Position(lineNo, argument.getRange().getEnd())), "Unknown tag: " + key);
                    diagnostic.setSeverity(DiagnosticSeverity.Warning);
                    diagnostics.add(diagnostic);
                }
            });
        } catch (Exception e) {
            log(e.toString());
        }
    }

    private boolean isNbtException(String key) {
        return key.equals("PersistenceRequired") || key.equals("nbt");
    }

    private void log(String message) {
        System.out.println(message);   
    }

    public void initialize(InitializeParams initializeParams) {
        if (initializeParams.getWorkspaceFolders().size() > 0) {
            for (WorkspaceFolder folder : initializeParams.getWorkspaceFolders()) {
                try {
                    initializeDirectory(folder.getUri());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void initializeDirectory(String folder) throws IOException {
        File worldFolder = Bukkit.getServer().getWorld("world").getWorldFolder();
        if (worldFolder == null) {
            throw new RuntimeException("Could not find world folder");
        }
        
        File datapackFolder = new File(worldFolder, "datapacks");
        FileUtils.deleteDirectory(datapackFolder);

        Path folderPath = Paths.get(URI.create(folder));
        Files.walk(folderPath).forEach(path -> {
            File file = path.toFile();
            if (file.getName().equals("pack.mcmeta")) {
                String randomName = UUID.randomUUID().toString().replace("-", "");
                File datapack = new File(datapackFolder, randomName);
                datapack.mkdirs();

                Path dataPath = datapack.toPath().resolve("data");
                try {
                    FileUtils.forceDeleteOnExit(datapack);
                    copy(file.toPath(), datapack.toPath().resolve("pack.mcmeta"));
                    copy(file.getParentFile().toPath().resolve("data"), dataPath);

                    for (File namespace : dataPath.toFile().listFiles()) {
                        if (namespace.isDirectory()) {
                            File functionsDir = namespace.toPath().resolve("functions").toFile();
                            if (functionsDir.exists()) {
                                FileUtils.deleteDirectory(functionsDir);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Bukkit.reloadData();

        Files.walk(folderPath).forEach(path -> {
            File file = path.toFile();
            if (file.getName().endsWith(".mcfunction")) {
                try {
                    StringBuilder text = new StringBuilder();
                    Files.readAllLines(path).forEach(line -> {
                        text.append(line + System.lineSeparator());
                    });

                    documentContents.put(file.toURI().toString(), text.toString());

                    debounceValidation(text.toString(), file.toURI().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void copy(Path sourcePath, Path destPath) throws IOException {
        File source = sourcePath.toFile();
        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, destPath.toFile());
        } else if(source.isFile()) {
            FileUtils.copyFile(source, destPath.toFile());
        }
    }
}
