package de.katzen48.datapack;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.Holder.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultTextDocumentService implements TextDocumentService {
    private DefaultLanguageServer languageServer;
    private CommandCompiler commandCompiler;
    //private LSClientLogger clientLogger;
    private int validationTask = -1;

    public DefaultTextDocumentService(DefaultLanguageServer languageServer, CommandCompiler commandCompiler) {
        this.languageServer = languageServer;
        this.commandCompiler = commandCompiler;
        //this.clientLogger = LSClientLogger.getInstance();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {
        debounceValidation(didChangeTextDocumentParams.getContentChanges().getLast().getText(), didChangeTextDocumentParams.getTextDocument().getUri());
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

    private void debounceValidation(String text, String documentUri) {
        if (validationTask != -1) {
            if (Bukkit.getScheduler().isQueued(validationTask) || Bukkit.getScheduler().isCurrentlyRunning(validationTask)) {
                Bukkit.getScheduler().cancelTask(validationTask);
            }
            validationTask = -1;
        }

        validationTask = Bukkit.getScheduler().scheduleSyncDelayedTask(LanguagePlugin.getProvidingPlugin(getClass()), () -> {
            ArrayList<Diagnostic> diagnostics = new ArrayList<>();
    
            AtomicInteger lineNo = new AtomicInteger(-1);
            AtomicInteger parsedLine = new AtomicInteger();
            text.lines().forEach(line -> {
                lineNo.incrementAndGet();
    
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ParseResults<?> results = commandCompiler.compile(line);
    
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
        }, 15L);
    }

    private void validateArgument(String name, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        switch (argument.getResult()) {
            case CompoundTag tag -> {
                validateCompoundTag(tag, argument, context, diagnostics, lineNo);
            }
            default -> {
                //log("Unknown argument type: " + argument.getResult().getClass().getName() + " for " + name + " at line " + lineNo + " with value " + argument.getResult());
            }
        }
    }

    private void validateCompoundTag(CompoundTag tag, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        if (context.getArguments().containsKey("entity")) {
            validateEntityCompoundTag(tag, argument, context, diagnostics, lineNo);
        } else {
            //log("Could not find entity argument at line " + lineNo + " with value " + argument.getResult());
        }
    }

    private void validateEntityCompoundTag(CompoundTag tag, ParsedArgument<?,?> argument, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {
        ParsedArgument<?,?> entity = context.getArguments().get("entity");
        if (entity.getResult() instanceof Reference reference) {
            if (reference.key().registry().getPath().equals("entity_type")) {
                validateEntityTypeCompoundTag(tag, argument, reference, context, diagnostics, lineNo);
            }
        } else {
            //log("Unknown entity argument: " + entity.getClass().getName() + " at line " + lineNo + " with value " + entity);
        }
    }

    private void validateEntityTypeCompoundTag(CompoundTag tag, ParsedArgument<?,?> argument, Reference<EntityType<?>> entityType, CommandContextBuilder<?> context, ArrayList<Diagnostic> diagnostics, int lineNo) {        
        try {
            Entity entity = SummonCommand.createEntity(MinecraftServer.getServer().createCommandSourceStack(), entityType, new Vec3(0, 0, 0), tag, true);
            EntityDataAccessor data = new EntityDataAccessor(entity);

            tag.getAllKeys().forEach(key -> {
                if (isNbtException(key)) {
                    return;
                }

                if (! data.getData().contains(key)) {
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
        return key.equals("PersistenceRequired");
    }

    private void log(String message) {
        System.out.println(message);   
    }
}
