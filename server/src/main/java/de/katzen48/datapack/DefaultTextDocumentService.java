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
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;

import de.katzen48.datapack.converters.ConverterHelper;
import de.katzen48.datapack.highlighting.SemanticToken;
import de.katzen48.datapack.highlighting.SemanticTokenType;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.List;
import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class DefaultTextDocumentService implements TextDocumentService {
    private DefaultLanguageServer languageServer;
    private CommandCompiler commandCompiler;
    private ReflectionHelper reflectionHelper;
    // private LSClientLogger clientLogger;
    private HashMap<String, Integer> validationTasks;
    private ValidationHelper validationHelper;

    private final HashMap<String, String> documentContents;
    private final HashSet<String> openPackFolders = new HashSet<>();

    public DefaultTextDocumentService(DefaultLanguageServer languageServer, CommandCompiler commandCompiler,
            ReflectionHelper reflectionHelper, HashMap<String, Integer> validationTasks,
            HashMap<String, String> documentContents, ValidationHelper validationHelper) {
        this.languageServer = languageServer;
        this.commandCompiler = commandCompiler;
        this.reflectionHelper = reflectionHelper;
        this.validationTasks = validationTasks;
        this.documentContents = documentContents;
        this.validationHelper = validationHelper;
        // this.clientLogger = LSClientLogger.getInstance();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {
        List<TextDocumentContentChangeEvent> contentChanges = didChangeTextDocumentParams.getContentChanges();
        String uri = URLDecoder.decode(didChangeTextDocumentParams.getTextDocument().getUri(), StandardCharsets.UTF_8);
        documentContents.put(uri, contentChanges.get(contentChanges.size() - 1).getText());
        debounceValidation(contentChanges.get(contentChanges.size() - 1).getText(), uri);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {
        // debounceValidation(didSaveTextDocumentParams.getText(),
        // didSaveTextDocumentParams.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        if (position.getPosition().getCharacter() == 0) {
            return commandCompiler.getCompletionSuggestions("", 0).thenApply(suggestions -> {
                return Either.forLeft(createCompletionItems(suggestions, false, "", position.getPosition()));
            });
        }

        String uri = URLDecoder.decode(position.getTextDocument().getUri(), StandardCharsets.UTF_8);
        String currentContent = documentContents.get(uri);
        if (currentContent == null) {
            return CompletableFuture.supplyAsync(() -> Either.forLeft(List.of()));
        }

        String line = currentContent.lines().skip(position.getPosition().getLine()).findFirst().orElse("");
        int cursor = position.getPosition().getCharacter();
        boolean hasWhiteSpace = line.substring(0, cursor).contains(" ");

        return commandCompiler.getCompletionSuggestions(line, cursor).thenApply(suggestions -> {
            return Either.forLeft(createCompletionItems(suggestions, hasWhiteSpace, line, position.getPosition()));
        });
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Either<Command, CodeAction>> codeActions = new ArrayList<>();

            int lineStart = params.getRange().getStart().getLine();
            int lineEnd = params.getRange().getEnd().getLine();

            String uri = URLDecoder.decode(params.getTextDocument().getUri(), StandardCharsets.UTF_8);
            String currentContent = documentContents.get(uri);
            if (currentContent != null) {
                String line = currentContent.lines().skip(lineStart).findFirst().orElse("");

                ParseResults<Object> results = commandCompiler.compile(line);

                CommandSyntaxException exception = commandCompiler.resolveException(results);

                if (exception != null) {
                    String convertedCommand = ConverterHelper.convertCommand(line, reflectionHelper);
                    if (!convertedCommand.equals(line)) {
                        Command command = new Command("Convert Command",
                                "java-datapack-language-server.convert-command", List.of(uri, lineStart, lineEnd));

                        codeActions.add(Either.forLeft(command));
                    }
                }
            }

            return codeActions;
        });
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayListMultimap<Integer, SemanticToken> lineTokens = ArrayListMultimap.create();

            String documentUri = URLDecoder.decode(params.getTextDocument().getUri(), StandardCharsets.UTF_8);

            if (documentUri.endsWith(".mcfunction")) {
                if (documentContents.containsKey(documentUri)) {
                    String text = documentContents.get(documentUri);

                    AtomicInteger lineNo = new AtomicInteger(-1);
                    text.lines().forEach(line -> {
                        lineNo.incrementAndGet();
                        line = line.stripTrailing();

                        if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("$")) {
                            ParseResults<Object> results = commandCompiler.compile(line);

                            CommandSyntaxException exception = commandCompiler.resolveException(results);

                            if (exception == null) {
                                parseSemanticTokens(results.getContext(), lineNo.get(), line, lineTokens);
                            }
                        }
                    });
                }
            }

            return new SemanticTokens(buildData(lineTokens));
        });
    }

    private void parseSemanticTokens(CommandContextBuilder<?> context, int lineNo, String line, ArrayListMultimap<Integer, SemanticToken> lineTokens) {        
        setData(lineTokens, lineNo, 0, line.contains(" ") ? line.indexOf(' ') : line.length(), SemanticTokenType.Command);
        
        context.getArguments().forEach((name, argument) -> {
            Object result = argument.getResult();

            if (result != null) {
                String typeName = result.getClass().getSimpleName().replace("[]", "");
                if (typeName.contains("$$Lambda")) {
                    typeName = typeName.substring(0, typeName.indexOf("$$Lambda"));
                }

                if (typeName.isBlank() || typeName.length() == 1) {
                    return;
                }

                try {
                    SemanticTokenType type = SemanticTokenType.valueOf(typeName);

                    if (type != null) {
                        if (type == SemanticTokenType.CompoundTag || type == SemanticTokenType.NBTTagCompound) {
                            String compoundPart = line.substring(argument.getRange().getStart(), argument.getRange().getEnd());
                            parseSemanticNbt(lineNo, new StringReader(compoundPart), argument.getRange().getStart(), lineTokens);
                            return;
                        }

                        setData(lineTokens, lineNo, argument.getRange().getStart(), argument.getRange().getLength(), type);
                    }
                } catch (IllegalArgumentException e) {
                    log(String.format("Line: %d, Char: %d - %s", lineNo, argument.getRange().getStart(),
                            e.toString()));
                    return;
                }
            }
        });

        if (context.getChild() != null) {
            parseSemanticTokens(context.getChild(), lineNo, line, lineTokens);
        }

        context.getNodes().forEach(node -> {
            if (node.getNode() instanceof LiteralCommandNode) {
                setData(lineTokens, lineNo, node.getRange().getStart(), node.getRange().getLength(), SemanticTokenType.Literal);
            }
        });
    }

    private ArrayList<Integer> buildData(ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        ArrayList<Integer> data = new ArrayList<>();

        int lastLineNo = 0;
        int lastOffset = 0;

        for (int lineNo : lineTokens.keySet()) {
            List<SemanticToken> tokens = lineTokens.get(lineNo);
            tokens.sort((a, b) -> a.start() - b.start());

            for (SemanticToken token : tokens) {
                int deltaLine = lineNo - lastLineNo;

                int deltaOffset = 0;
                if (deltaLine != 0) {
                    deltaOffset = token.start();
                    lastOffset = 0;
                } else {
                    deltaOffset = token.start() - lastOffset;
                }
        
                lastOffset = token.start();
                lastLineNo = lineNo;
        
                data.add(deltaLine);
                data.add(deltaOffset);
                data.add(token.length());
                data.add(token.tokenType().ordinal());
                data.add(0);
            }
        }

        return data;
    }

    private void setData(ArrayListMultimap<Integer, SemanticToken> lineTokens, int lineNo, int start, int length, SemanticTokenType type) {
        lineTokens.put(lineNo, new SemanticToken(start, length, type));
    }

    private void parseSemanticNbt(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        parseSemanticNbt(lineNo, reader, offset, SemanticTokenType.NbtValue, lineTokens);
    }

    private void parseSemanticNbt(int lineNo, StringReader reader, int offset, SemanticTokenType valueType, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        if (!reader.canRead()) {
            return;
        }

        char c = reader.peek();
        if (c == '{') {
            parseSemanticNbtStruct(lineNo, reader, offset, lineTokens);
        } else {
            if (c == '[') {
                parseSemanticNbtList(lineNo, reader, offset, lineTokens);
            } else {
                parseSemanticNbtTypedValue(lineNo, reader, offset, valueType, lineTokens);
            }
        }
    }

    private void parseSemanticNbtStruct(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        reader.skip();
        reader.skipWhitespace();
        while (reader.canRead() && reader.peek() != '}') {
            int i = reader.getCursor();

            reader.skipWhitespace();
            if (!reader.canRead()) {
                return;
            }

            try {
                reader.skipWhitespace();
                int start = reader.getCursor() + offset;
                String key = reader.readString();
                if (key.isEmpty()) {
                    log("NBT Struct key is empty at line " + lineNo + " char " + reader.getCursor() + offset);
                    reader.setCursor(i);
                    return;
                }
                int end = reader.getCursor() + offset;
                setData(lineTokens, lineNo, start, end - start, SemanticTokenType.NbtKey);

                setData(lineTokens, lineNo, start + 1, 1, SemanticTokenType.NbtKeyValueSeparator);

                reader.skipWhitespace();
                reader.skip();

                parseSemanticNbt(lineNo, reader, offset, lineTokens);

                if (!parseElementSeparator(lineNo, reader, offset, lineTokens)) {
                    break;
                }

                reader.skipWhitespace();
                if (!reader.canRead()) {
                    break;
                }
            } catch (CommandSyntaxException e) {
                log(e.toString());
                return;
            }
        }

        reader.skipWhitespace();
        reader.skip();
    }

    private void parseSemanticNbtList(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        if (reader.canRead(3) && !StringReader.isQuotedStringStart(reader.peek(1)) && reader.peek(2) == ';') {
            parseSemanticNbtArray(lineNo, reader, offset, lineTokens);
        } else {
            parseSemanticNbtListTag(lineNo, reader, offset, lineTokens);
        }
    }

    private void parseSemanticNbtArray(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        reader.skip();
        
        int i = reader.getCursor();
        char c = reader.read();

        setData(lineTokens, lineNo, offset, reader.getCursor(), SemanticTokenType.NBTArrayType);

        reader.read();
        reader.skipWhitespace();

        setData(lineTokens, lineNo, offset, reader.getCursor(), SemanticTokenType.NBTElementSeparator);
        if (!reader.canRead()) {
            return;
        } else if (c == 'B') {
            parseSemanticArray(lineNo, reader, offset, lineTokens);
        } else if (c == 'L') {
            parseSemanticArray(lineNo, reader, offset, lineTokens);
        } else if (c == 'I') {
            parseSemanticArray(lineNo, reader, offset, lineTokens);
        } else {
            reader.setCursor(i);
        }
    }

    private void parseSemanticNbtTypedValue(int lineNo, StringReader reader, int offset, SemanticTokenType type, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        if (StringReader.isQuotedStringStart(reader.peek())) {
            int start = reader.getCursor() + offset;
            
            try {
                reader.readQuotedString();
                int end = reader.getCursor() + offset;

                setData(lineTokens, lineNo, start, end - start, type);
            } catch (CommandSyntaxException e) {
                log(e.toString());
            }
            
            return;
        } else {
            int start = reader.getCursor() + offset;
            String string = reader.readUnquotedString();
            if (!string.isEmpty()) {
                int end = reader.getCursor() + offset;
                setData(lineTokens, lineNo, start, end - start, type);
                return;
            }
        }
    }

    private void parseSemanticArray(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        while (reader.peek() != ']') {
            parseSemanticNbt(lineNo, reader, offset, SemanticTokenType.NBTArrayValue, lineTokens);

            if (!parseElementSeparator(lineNo, reader, offset, lineTokens)) {
                break;
            }

            if (!reader.canRead()) {
                return;
            }
        }

        reader.skipWhitespace();
        reader.skip();
    }

    private void parseSemanticNbtListTag(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        reader.skip();
        
        reader.skipWhitespace();
        if (!reader.canRead()) {
            return;
        } else {
            while (reader.peek() != ']') {
                parseSemanticNbt(lineNo, reader, offset, SemanticTokenType.NBTArrayValue, lineTokens);

                if (!parseElementSeparator(lineNo, reader, offset, lineTokens)) {
                    break;
                }

                if (!reader.canRead()) {
                    return;
                }
            }

            reader.skipWhitespace();
            reader.skip();
        }
    }

    private boolean parseElementSeparator(int lineNo, StringReader reader, int offset, ArrayListMultimap<Integer, SemanticToken> lineTokens) {
        reader.skipWhitespace();
        if (reader.canRead() && reader.peek() == ',') {
            setData(lineTokens, lineNo, reader.getCursor() + offset, 1, SemanticTokenType.NBTElementSeparator);

            reader.skip();
            reader.skipWhitespace();
            return true;
        } else {
            return false;
        }
    }

    private List<CompletionItem> createCompletionItems(Suggestions suggestions, boolean containsWhitespace, String line, Position position) {
        ArrayList<CompletionItem> completionItems = new ArrayList<>();
        int startIndex = 0;
        int endIndex = line.length();
        if (position.getCharacter() > 0) {
            String part = line.substring(0, position.getCharacter());
            startIndex = part.lastIndexOf(' ') + 1;
            
            endIndex = line.substring(startIndex).indexOf(' ');
            if (endIndex == -1) {
                endIndex = line.length();
            } else {
                endIndex += startIndex;
            }
        }

        final int finalStartIndex = startIndex;
        final int finalEndIndex = endIndex;
        suggestions.getList().forEach(suggestion -> {
            if (suggestion.getText().isEmpty()) {
                return;
            }
            if (!containsWhitespace && suggestion.getText().contains(":")) {
                return;
            }

            CompletionItem completionItem = new CompletionItem();
            completionItem.setLabel(suggestion.getText());

            Range range = new Range(new Position(position.getLine(), finalStartIndex), new Position(position.getLine(), finalEndIndex));
            completionItem.setTextEdit(Either.forRight(new InsertReplaceEdit(suggestion.getText(), range, range)));

            completionItems.add(completionItem);
        });

        return completionItems;
    }

    public void debounceValidation(String text, String documentUri) {
        if (validationTasks.containsKey(documentUri)) {
            if (Bukkit.getScheduler().isQueued(validationTasks.get(documentUri))
                    || Bukkit.getScheduler().isCurrentlyRunning(validationTasks.get(documentUri))) {
                Bukkit.getScheduler().cancelTask(validationTasks.get(documentUri));
            }
            validationTasks.remove(documentUri);
        }

        validationTasks.put(documentUri,
                Bukkit.getScheduler().scheduleSyncDelayedTask(LanguagePlugin.getProvidingPlugin(getClass()), () -> {
                    ArrayList<Diagnostic> diagnostics = new ArrayList<>();

                    if (documentUri.endsWith(".mcfunction")) {
                        AtomicInteger lineNo = new AtomicInteger(-1);
                        text.lines().forEach(line -> {
                            lineNo.incrementAndGet();
                            line = line.stripTrailing();

                            if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("$")) {
                                ParseResults<Object> results = commandCompiler.compile(line);

                                CommandSyntaxException exception = commandCompiler.resolveException(results);

                                if (exception != null) {
                                    Diagnostic diagnostic = new Diagnostic(
                                            new Range(new Position(lineNo.get(), exception.getCursor()),
                                                    new Position(lineNo.get(), exception.getCursor())),
                                            exception.getMessage());
                                    diagnostic.setSeverity(DiagnosticSeverity.Error);
                                    diagnostics.add(diagnostic);
                                    return;
                                }

                                results.getContext().getArguments().forEach((name, argument) -> {
                                    validateArgument(name, argument, results.getContext(), diagnostics, lineNo.get());
                                });
                            }
                        });
                    } else if (documentUri.endsWith(".json")) {
                        String workspaceFolder = getWorkspaceFolder(documentUri);

                        if (workspaceFolder != null) {
                            String error = validationHelper.validate(workspaceFolder, documentUri, text).orElse(null);
                            if (error != null) {
                                List<String> lines = text.lines().toList();

                                diagnostics.add(new Diagnostic(
                                        new Range(new Position(0, 0),
                                                new Position(lines.size(), lines.get(lines.size() - 1).length())),
                                        error));
                            }
                        }
                    }

                    languageServer.languageClient
                            .publishDiagnostics(new PublishDiagnosticsParams(documentUri, diagnostics));

                    validationTasks.remove(documentUri);
                }, 15L));
    }

    private void validateArgument(String name, ParsedArgument<?, ?> argument, CommandContextBuilder<?> context,
            ArrayList<Diagnostic> diagnostics, int lineNo) {
        if (reflectionHelper.getCompoundTagClass().isInstance(argument.getResult())) {
            validateCompoundTag(argument.getResult(), argument, context, diagnostics, lineNo);
            return;
        }
    }

    private void validateCompoundTag(Object tag, ParsedArgument<?, ?> argument, CommandContextBuilder<?> context,
            ArrayList<Diagnostic> diagnostics, int lineNo) {
        if (context.getArguments().containsKey("entity")) {
            validateEntityCompoundTag(tag, argument, context, diagnostics, lineNo);
        }
    }

    private void validateEntityCompoundTag(Object tag, ParsedArgument<?, ?> argument, CommandContextBuilder<?> context,
            ArrayList<Diagnostic> diagnostics, int lineNo) {
        ParsedArgument<?, ?> entity = context.getArguments().get("entity");
        if (reflectionHelper.getReferenceClass().isInstance(entity.getResult())) {
            if (reflectionHelper.getResourceLocationPathFromEntityTypeReference(entity.getResult())
                    .equals("entity_type")) {
                validateEntityTypeCompoundTag(tag, argument, entity.getResult(), context, diagnostics, lineNo);
            }
        }
    }

    private void validateEntityTypeCompoundTag(Object tag, ParsedArgument<?, ?> argument, Object entityType,
            CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        try {
            Object vec3 = reflectionHelper.getVec3Proxy().create(0, 0, 0);
            Object entity = reflectionHelper.getSummonCommandProxy().createEntity(
                    reflectionHelper.getMinecraftServerProxy()
                            .createCommandSourceStack(reflectionHelper.getMinecraftServerProxy().getServer()),
                    entityType, vec3, tag, true);
            Object dataAccessor = reflectionHelper.getEntityDataAccessorProxy().create(entity);

            reflectionHelper.getCompoundTagProxy().getAllKeys(tag).forEach(key -> {
                if (isNbtException(key)) {
                    return;
                }

                Object entityData = reflectionHelper.getEntityDataAccessorProxy().getData(dataAccessor);
                if (!reflectionHelper.getCompoundTagProxy().contains(entityData, key)) {
                    Diagnostic diagnostic = new Diagnostic(
                            new Range(new Position(lineNo, argument.getRange().getStart()),
                                    new Position(lineNo, argument.getRange().getEnd())),
                            "Unknown tag: " + key);
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
                    String uri = URLDecoder.decode(folder.getUri(), StandardCharsets.UTF_8);
                    initializeDirectory(uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void reloadData() throws IOException {
        File worldFolder = Bukkit.getServer().getWorld("world").getWorldFolder();
        if (worldFolder == null) {
            throw new RuntimeException("Could not find world folder");
        }

        File datapackFolder = new File(worldFolder, "datapacks");
        FileUtils.deleteDirectory(datapackFolder);

        for (String folder : openPackFolders) {
            Path folderPath = new File(folder).toPath();
            Files.walk(folderPath).forEach(path -> {
                File file = path.toFile();
                if (file.getName().equals("pack.mcmeta")) {
                    Path dataFolderPath = file.getParentFile().toPath().resolve("data");
                    if (dataFolderPath.toFile().exists()) {
                        String randomName = UUID.randomUUID().toString().replace("-", "");
                        File datapack = new File(datapackFolder, randomName);
                        datapack.mkdirs();

                        Path dataPath = datapack.toPath().resolve("data");
                        try {
                            FileUtils.forceDeleteOnExit(datapack);
                            copy(path, datapack.toPath().resolve("pack.mcmeta"));
                            copy(dataFolderPath, dataPath);

                            for (File namespace : dataPath.toFile().listFiles()) {
                                if (namespace.isDirectory()) {
                                    File functionsDir = namespace.toPath().resolve("functions").toFile();
                                    if (functionsDir.exists()) {
                                        Files.walk(functionsDir.toPath()).forEach(functionFile -> {
                                            if (functionFile.toFile().getName().endsWith(".mcfunction")) {
                                                try (FileWriter writer = new FileWriter(functionFile.toFile())) {
                                                    writer.flush();
                                                } catch (IOException e) {
                                                    log(e.toString());
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
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

                        documentContents.put(file.toPath().toUri().toString(), text.toString());
                        debounceValidation(text.toString(), file.toPath().toUri().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (file.getName().endsWith(".json")) {
                    try {
                        StringBuilder text = new StringBuilder();
                        Files.readAllLines(path).forEach(line -> {
                            text.append(line + System.lineSeparator());
                        });

                        documentContents.put(file.toPath().toUri().toString(), text.toString());
                        debounceValidation(text.toString(), file.toPath().toUri().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private void initializeDirectory(String folder) throws IOException {
        Path folderPath = Paths.get(URI.create(folder));

        Files.walk(folderPath).forEach(path -> {
            File file = path.toFile();
            if (file.getName().equals("pack.mcmeta")) {
                Path dataFolderPath = file.getParentFile().toPath().resolve("data");
                if (dataFolderPath.toFile().exists()) {
                    openPackFolders.add(file.getParentFile().toPath().toString());
                }
            }
        });

        reloadData();
    }

    private void copy(Path sourcePath, Path destPath) throws IOException {
        File source = sourcePath.toFile();
        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, destPath.toFile());
        } else if (source.isFile()) {
            FileUtils.copyFile(source, destPath.toFile());
        }
    }

    private String getWorkspaceFolder(String uri) {
        Path path = null;
        try {
            path = Paths.get(URI.create(uri));
        } catch (Exception e) {
            try {
                path = new File(uri).toPath();
            } catch (Exception e2) {
                return null;
            }
        }

        for (String folder : openPackFolders) {
            Path folderPath = new File(folder).toPath();
            if (path.startsWith(folderPath)) {
                return folder;
            }
        }

        return null;
    }
}
