import { window, ProgressLocation, QuickPickItem, ExtensionContext, QuickPickItemKind, workspace, Uri, Progress, commands } from "vscode";
import fs = require("fs");

export function executeCommand(context: ExtensionContext, callback: Function) {
    return async () => {
        const version = await selectVersion(context.globalState.get<string[]|null>('versions'));
    
        if (!version?.version) {
            window.showInformationMessage("No version selected");
            return;
        }

        const build = await getNewestBuild(version.version);

        if (!build) {
            return;
        }

        const fullVersion = `${build.version}-${build.build}`;
        context.workspaceState.update('selectedVersion', build.version);
        context.workspaceState.update('selectedFullVersion', fullVersion);

        const versionDir = Uri.joinPath(context.globalStorageUri, 'versions', build.version, fullVersion)
        const versionJar = Uri.joinPath(versionDir, `${fullVersion}.jar`)
        
        if (!fs.existsSync(versionJar.fsPath)) {
            workspace.fs.createDirectory(versionDir)
            window.withProgress({
                location: ProgressLocation.Notification,
                title: "Downloading " + version.version + ' ' + build?.build,
                cancellable: true
            }, async (progress, token) => {
                token.onCancellationRequested(() => {
                    console.log("User canceled the long running operation");
                });
    
                progress.report({ increment: 0 });
    
                await download(build, versionJar, progress);

                callback();
            });
        } else {
            callback();
        }
    }
}

async function download(build: VersionBuild, versionJar: Uri, progress: Progress<{ message?: string; increment?: number }>) {
    const response = await fetch(`https://api.papermc.io/v2/projects/paper/versions/${build.version}/builds/${build.build}/downloads/${build.download}`);
    if (!response.ok) {
        window.showErrorMessage("Failed to download build");
        return;
    }

    const contentLength = +response.headers.get('Content-Length');
    const reader = response.body.getReader();
    const chunks = [];

    let receivedLength = 0;
    let lastPercent = 0;

    while(true) {
        const {done, value} = await reader.read();

        if (done) {
            break;
        }

        chunks.push(value);

        receivedLength += value.length;
        const percent = Math.floor((receivedLength / contentLength) * 100);

        progress.report({ increment: percent - lastPercent });

        lastPercent = percent;
    }

    let chunksAll = new Uint8Array(receivedLength);
    let position = 0;
    for(let chunk of chunks) {
        chunksAll.set(chunk, position);
        position += chunk.length;
    }

    fs.writeFileSync(versionJar.fsPath, chunksAll)

    window.showInformationMessage("Download complete");
}

async function getNewestBuild(version: string): Promise<VersionBuild|null> {
    const response = await fetch(`https://api.papermc.io/v2/projects/paper/versions/${version}/builds`)
    if (!response.ok) {
        window.showErrorMessage("Failed to fetch builds");
        return null;
    }

    const json: any = await response.json();
    const builds : any[] = json.builds;

    if (builds.length === 0) {
        window.showErrorMessage("No builds for this version");
        return null;
    }

    const latest = builds[builds.length - 1]

    return new VersionBuild(version, latest.build, latest.downloads.application.name);
}

async function selectVersion(versions: string[]|null) : Promise<VersionItem|null> {   
    const selected = await window.showQuickPick(getVersionList(versions), {
        title: "Enter the version you want to download",
        canPickMany: false,
        matchOnDescription: true
    });

    return selected;
}

function getVersionList(versions: string[]|null): VersionItem[] {
    let items = versions ? versions.map(version => new VersionItem(version)).reverse(): [];

    if (items.length > 0) {
        let lastMajorMinor = null;

        let length = items.length;
        for (let i = 0; i < length; i++) {
            const item = items[i];
            const majorMinor = item.version.split('.', 2).join('.');
            if (lastMajorMinor !== majorMinor) {
                items = items.slice(0, i).concat(new VersionItem(majorMinor, true), items.slice(i));
                i++;
                length++;

                lastMajorMinor = majorMinor;
            }
        }
    }

    return items;
}

class VersionItem implements QuickPickItem {
    label: string;
    kind?: QuickPickItemKind;

    constructor(public version: string, public isMajorMinor: boolean = false) {
        this.label = version;

        if (isMajorMinor) {
            this.kind = QuickPickItemKind.Separator;
        }
    }
}

class VersionBuild {
    constructor(public version: string, public build: string, public download: string) {}
}