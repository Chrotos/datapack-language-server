package de.katzen48.datapack;

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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

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
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {        /*
        return CompletableFuture.supplyAsync(() -> {
            CompletionItem completionItem = new CompletionItem();
            completionItem.setLabel("Test completion item");
            completionItem.setInsertText("Test");
            completionItem.setDetail("Snippet");
            completionItem.setKind(CompletionItemKind.Snippet);
            return Either.forLeft(List.of(completionItem));
        });
        */

        return commandCompiler.getCompletionSuggestions("").thenApply(suggestions -> {
            return Either.forLeft(createCompletionItems(suggestions));
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

    private List<CompletionItem> createCompletionItems(Suggestions suggestions) {
        ArrayList<CompletionItem> completionItems = new ArrayList<>();
        
        suggestions.getList().forEach(suggestion -> {
            if (suggestion.getText().isEmpty()) {
                return;
            }
            if (suggestion.getText().contains(":")) {
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
    
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ParseResults<Object> results = commandCompiler.compile(line);
    
                    CommandSyntaxException exception = commandCompiler.resolveException(results);
    
                    if (exception != null) {
                        Diagnostic diagnostic = new Diagnostic(new Range(new Position(lineNo.get(), exception.getCursor()), new Position(lineNo.get(), exception.getCursor())), exception.getMessage());
                        diagnostic.setSeverity(DiagnosticSeverity.Error);
                        diagnostics.add(diagnostic);
                        return;
                    }

                    /*
                    if (parsedLine.incrementAndGet() == 2) {
                        results.getContext().getArguments().forEach((name, argument) -> {
                            System.out.format("%s: %s(%s)%n", name, argument.getResult().getClass(), argument.getResult());
                        });
                    }
                    */

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
}
