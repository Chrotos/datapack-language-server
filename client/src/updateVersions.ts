import { window, ProgressLocation, ExtensionContext } from "vscode";

export function executeCommand(context: ExtensionContext) {
    return async () => {
        const response = await fetch('https://api.papermc.io/v2/projects/paper')

        if (!response.ok) {
            window.showErrorMessage("Failed to fetch versions");
            return;
        }

        const json: any = await response.json();
        context.globalState.update('versions', json.versions);

        window.showInformationMessage("Versions updated");
    }
}