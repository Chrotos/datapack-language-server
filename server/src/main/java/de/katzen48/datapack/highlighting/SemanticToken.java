package de.katzen48.datapack.highlighting;

public final record SemanticToken (
    int start,
    int length,
    SemanticTokenType tokenType
) {}
