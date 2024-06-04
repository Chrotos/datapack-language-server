package de.katzen48.datapack;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public class DefaultWorkspaceService implements WorkspaceService {

    private DefaultLanguageServer languageServer;
    //LSClientLogger clientLogger;

    public DefaultWorkspaceService(DefaultLanguageServer languageServer) {
        this.languageServer = languageServer;
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
}