import { window, ProgressLocation, ExtensionContext } from "vscode";

export function executeCommand(context: ExtensionContext) {
    return async () => {
        const response = await fetch('https://api.papermc.io/v2/projects/paper')

        if (!response.ok) {
            window.showErrorMessage("Failed to fetch versions");
            return;
        }

        const json: any = await response.json();
        const versions: string[] = json.versions;


        context.globalState.update('versions', versions.filter((version: string) => version.match("1\\.(19\\.4|([2-9][0-9])(\\.\\d{0,}){0,1})")));

        window.showInformationMessage("Versions updated");
    }
}